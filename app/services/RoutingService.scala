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
import models.RoutingOption

class RoutingService @Inject() (routeChecker: RouteChecker, messageConnector: MessageConnector)
    extends Logging {

  def submitMessage(
    xml: NodeSeq,
    channel: ChannelType,
    headerCarrier: HeaderCarrier
  ): Either[FailureMessage, Future[HttpResponse]] = {
    XmlParser.getValidRoot(xml) match {
      case None =>
        Left(InvalidMessageCode(s"Invalid Message Type"))

      case Some(XmlParser.RootNode(messageType, _))
          if MessageType.guaranteeValues.contains(messageType) =>

        val routingOption = RoutingOption.Gb

        logger.debug(
          s"Guarantee message ${messageType.code} routing option ${routingOption.prefix} with channel ${channel.name}"
        )

        Either.cond(
          routeChecker.canForward(routingOption, channel),
          messageConnector.post(xml, routingOption, headerCarrier),
          RejectionMessage(
            s"Routing to guarantee management system rejected on ${channel.name} channel"
          )
        )

      case Some(XmlParser.RootNode(messageType, rootXml)) =>
        val parseOffice: Either[ParseError, Office] =
          if (MessageType.arrivalValues.contains(messageType)) {
            logger.debug("Determining office of presentation ...")
            XmlParser.officeOfPresentation(rootXml)
          } else {
            logger.debug("Determining office of departure ...")
            XmlParser.officeOfDeparture(rootXml)
          }

        parseOffice.flatMap { office =>
          val routingOption = office.getRoutingOption

          logger.debug(
            s"Office of departure/presentation ${office.value} routing option ${routingOption.prefix} with channel ${channel.name}"
          )

          Either.cond(
            routeChecker.canForward(routingOption, channel),
            messageConnector.post(xml, routingOption, headerCarrier),
            RejectionMessage(
              s"Routing to ${office.value.take(2)} rejected on ${channel.name} channel"
            )
          )
        }
    }
  }
}
