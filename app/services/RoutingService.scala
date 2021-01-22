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

import cats.data.ReaderT
import com.google.inject.Inject
import config.AppConfig
import connectors.MessageConnector
import models.ParseError.{DepartureEmpty, InvalidMessageCode, PresentationEmpty}
import models.RoutingOption.{Gb, Reject, Xi}
import models.requests.ChannelRequest
import models.{ChannelType, DepartureOffice, FailureMessage, MessageType, Office, ParseError, ParseHandling, PresentationOffice, RejectionMessage}
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import scala.xml.NodeSeq

class RoutingService @Inject() (appConfig: AppConfig, messageConnector: MessageConnector) extends ParseHandling {

  val logger = Logger(this.getClass)

  def submitMessage(xml: NodeSeq)(implicit request: ChannelRequest[NodeSeq], requestHeader: RequestHeader, headerCarrier: HeaderCarrier): Either[FailureMessage, Future[HttpResponse]] = {
    getValidRoot(xml) match {
      case None => Left(InvalidMessageCode(s"Invalid Message Type"))
      case Some((rootXml, messageType)) =>
        val officeEither: Either[ParseError, Office] = if(MessageType.arrivalValues.contains(messageType)) {
          officeOfPresentation(rootXml)
        }
        else {
          officeOfDeparture(rootXml)
        }

        officeEither.flatMap {
          office =>
            getEisDetails(office, request.channel).map {
              details => messageConnector.post(xml.toString(), details.url, details.token)
            }
        }
    }
  }

  private case class EisDetails(url: String, token: String, routingMessage: String)

  private def getRoutingOption(office: Office, channelType: ChannelType) =
    (office.value.startsWith("XI"), channelType) match {
    case (true, _: ChannelType.web.type) => appConfig.webXiRouting
    case (true, _: ChannelType.api.type) => appConfig.apiXiRouting
    case (false, _: ChannelType.web.type) => appConfig.webGbRouting
    case (false, _: ChannelType.api.type) => appConfig.apiGbRouting
  }

  private def getEisDetails(office: Office, channelType: ChannelType): Either[RejectionMessage, EisDetails] = {
    getRoutingOption(office, channelType) match {
      case Xi => Right(EisDetails(appConfig.eisniUrl, appConfig.eisniBearerToken, "routing to NI"))
      case Gb => Right(EisDetails(appConfig.eisgbUrl, appConfig.eisgbBearerToken, "routing to GB"))
      case Reject => Left(RejectionMessage(s"Routing to ${office.value.splitAt(2)._1} rejected on $channelType channel"))
    }
  }

  private def getValidRoot(xml: NodeSeq): Option[(NodeSeq, MessageType)] = {
    MessageType.validMessages.map { m =>
      val result = (xml \\ m.rootNode)
      (result, m)
    }.filterNot(pair => pair._1 == NodeSeq.Empty).headOption
  }

  private val officeOfDeparture: ReaderT[ParseHandler, NodeSeq, DepartureOffice] =
    ReaderT[ParseHandler, NodeSeq, DepartureOffice](xml => {
      (xml \\ "CUSOFFDEPEPT" \ "RefNumEPT1").text match {
        case departure if departure.isEmpty =>Left(DepartureEmpty("Departure Empty"))
        case departure => Right(DepartureOffice(departure))
      }
    })

  private val officeOfPresentation: ReaderT[ParseHandler, NodeSeq, PresentationOffice] =
    ReaderT[ParseHandler, NodeSeq, PresentationOffice](xml => {
      (xml \ "CUSOFFPREOFFRES" \ "RefNumRES1").text match {
        case presentation if presentation.isEmpty => Left(PresentationEmpty("Presentation Empty"))
        case presentation => Right(PresentationOffice(presentation))
      }
    })
}
