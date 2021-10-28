/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import com.google.inject.Inject
import connectors.MessageConnector
import models.ChannelType
import models.FailureMessage
import models.GuaranteeReference
import models.MessageType
import models.Office
import models.ParseError
import models.ParseError.InvalidMessageCode
import models.RejectionMessage
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future
import scala.xml.NodeSeq
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.stream.alpakka.xml.scaladsl.XmlParsing
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Flow
import akka.NotUsed
import akka.stream.alpakka.xml.ParseEvent
import akka.stream.alpakka.xml.StartElement
import akka.stream.alpakka.xml.StartDocument
import models.MessageType.QueryOnGuarantees
import akka.stream.scaladsl.RunnableGraph
import play.api.libs.ws.WSClient
import akka.stream.scaladsl.Broadcast
import akka.stream.ClosedShape
import models.DepartureOffice
import models.PresentationOffice
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import models.ParseError._
import cats.data.EitherT
import play.api.libs.ws.WSResponse
import akka.stream.alpakka.xml.Characters

class RoutingService @Inject() (
  routeChecker: RouteChecker,
  messageConnector: MessageConnector,
  wsClient: WSClient
)(implicit val system: ActorSystem)
    extends Logging {

  implicit val ec: ExecutionContext = system.dispatcher

  val guaranteeReference =
    XmlParsing
      .subtree("GUAREF2" :: "GuaRefNumGRNREF21" :: Nil)
      .map { elem =>
        Either.cond(
          elem.getTextContent.nonEmpty,
          GuaranteeReference(elem.getTextContent).getRoutingOption,
          GuaranteeReferenceEmpty("Guarantee Reference Empty"): FailureMessage
        )
      }

  val officeOfDeparture =
    XmlParsing
      .subtree("CUSOFFDEPEPT" :: "RefNumEPT1" :: Nil)
      .map { elem =>
        Either.cond(
          elem.getTextContent.nonEmpty,
          DepartureOffice(elem.getTextContent).getRoutingOption,
          DepartureEmpty("Departure Empty"): FailureMessage
        )
      }

  val officeOfPresentation =
    XmlParsing
      .subtree("CUSOFFPREOFFRES" :: "RefNumRES1" :: Nil)
      .map { elem =>
        Either.cond(
          elem.getTextContent.nonEmpty,
          PresentationOffice(elem.getTextContent).getRoutingOption,
          PresentationEmpty("Presentation Empty"): FailureMessage
        )
      }

  def submitMessage(
    xml: Source[ByteString, _],
    channel: ChannelType,
    headerCarrier: HeaderCarrier
  ): Future[Either[FailureMessage, WSResponse]] = {

    // Peek at the first element and see which message type it has
    val routingSink = XmlParsing.parser
      .flatMapPrefix(4) {
        case StartDocument +: (_: StartElement) +: (start: StartElement) +: _ =>
          // Decide which parsing flow we need
          MessageType
            .withRootNode(start.localName)
            .map {
              case messageType if MessageType.guaranteeValues.contains(messageType) =>
                guaranteeReference
              case messageType if MessageType.departureValues.contains(messageType) =>
                officeOfDeparture
              case messageType if MessageType.arrivalValues.contains(messageType) =>
                officeOfPresentation
            }
            .getOrElse {
              logger.error(s"Unrecognised root node ${start.localName}")
              // We don't recognise this message
              throw new Exception
            }
        case StartDocument +: (_: StartElement) +: (_: Characters) +: (start: StartElement) +: _ =>
          // Decide which parsing flow we need
          MessageType
            .withRootNode(start.localName)
            .map {
              case messageType if MessageType.guaranteeValues.contains(messageType) =>
                guaranteeReference
              case messageType if MessageType.departureValues.contains(messageType) =>
                officeOfDeparture
              case messageType if MessageType.arrivalValues.contains(messageType) =>
                officeOfPresentation
            }
            .getOrElse {
              logger.error(s"Unrecognised root node ${start.localName}")
              // We don't recognise this message
              throw new Exception
            }
      }
      .toMat(Sink.head)(Keep.right)

    // Stream the request body into another Source;
    // parse out the routing info while doing so
    val (routingFuture, xmlSource) = xml
      .alsoToMat(routingSink)(Keep.right)
      .toMat(Sink.asPublisher(true))(Keep.both)
      .mapMaterializedValue { case (routing, publisher) =>
        (routing, Source.fromPublisher(publisher))
      }
      .run()

    val result = for {
      routing <- EitherT(routingFuture)

      response <- {
        if (routeChecker.canForward(routing, channel))
          EitherT.right[FailureMessage](messageConnector.post(xmlSource, routing, headerCarrier))
        else
          EitherT.leftT[Future, WSResponse](
            RejectionMessage(
              s"Routing to ${routing} rejected on ${channel.name} channel"
            ): FailureMessage
          )
      }

    } yield response

    result.value

    //   XmlParser.getValidRoot(xml) match {
    //     case None =>
    //       Left(InvalidMessageCode(s"Invalid Message Type"))

    //     case Some(XmlParser.RootNode(messageType, rootXml))
    //         if MessageType.guaranteeValues.contains(messageType) =>

    //       val parseGrn: Either[ParseError, GuaranteeReference] =
    //         XmlParser.guaranteeReference(rootXml)

    //       parseGrn.flatMap { grn =>
    //         val routingOption = grn.getRoutingOption

    //         logger.debug(
    //           s"Guarantee reference ${grn.value} routing option ${routingOption.prefix} with channel ${channel.name}"
    //         )

    //         Either.cond(
    //           routeChecker.canForward(routingOption, channel),
    //           messageConnector.post(xml, routingOption, headerCarrier),
    //           RejectionMessage(
    //             s"Routing to ${grn.countryCode} rejected on ${channel.name} channel"
    //           )
    //         )
    //       }

    //     case Some(XmlParser.RootNode(messageType, rootXml)) =>
    //       val parseOffice: Either[ParseError, Office] =
    //         if (MessageType.arrivalValues.contains(messageType)) {
    //           logger.debug("Determining office of presentation ...")
    //           XmlParser.officeOfPresentation(rootXml)
    //         } else {
    //           logger.debug("Determining office of departure ...")
    //           XmlParser.officeOfDeparture(rootXml)
    //         }

    //       parseOffice.flatMap { office =>
    //         val routingOption = office.getRoutingOption

    //         logger.debug(
    //           s"Office of departure/presentation ${office.value} routing option ${routingOption.prefix} with channel ${channel.name}"
    //         )

    //         Either.cond(
    //           routeChecker.canForward(routingOption, channel),
    //           messageConnector.post(xml, routingOption, headerCarrier),
    //           RejectionMessage(
    //             s"Routing to ${office.value.take(2)} rejected on ${channel.name} channel"
    //           )
    //         )
    //       }
    //   }
    // }
  }
}
