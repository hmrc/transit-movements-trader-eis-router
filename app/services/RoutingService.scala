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
import logging.Logging
import models.FailureMessage
import models.MessageType
import models.Office
import models.ParseError
import models.ParseError.InvalidMessageCode
import models.RejectionMessage
import models.requests.ChannelRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.xml.NodeSeq

class RoutingService @Inject() (routeChecker: RouteChecker, messageConnector: MessageConnector) extends Logging {

  def submitMessage(request: ChannelRequest[NodeSeq], headerCarrier: HeaderCarrier)(implicit ec: ExecutionContext): Either[FailureMessage, Future[HttpResponse]] =
    XmlParser.getValidRoot(request.body) match {
      case None =>
        Left(InvalidMessageCode(s"Invalid Message Type"))

      case Some(XmlParser.RootNode(messageType, rootXml)) =>
        val parseOffice: Either[ParseError, Office] =
          if (MessageType.arrivalValues.contains(messageType)) {
            logger.debug("Determining office of presentation ...")
            XmlParser.officeOfPresentation(rootXml)
          } else {
            logger.debug("Determining office of departure ...")
            XmlParser.officeOfDeparture(rootXml)
          }

        parseOffice.flatMap {
          office =>
            val routingOption = office.getRoutingOption

            logger.debug(s"Office of departure/presentation $office routing option $routingOption with channel ${request.channel}")

            Either.cond(
              routeChecker.canForward(routingOption, request.channel),
              messageConnector.post(request, routingOption, headerCarrier),
              RejectionMessage(s"Routing to ${office.value.take(2)} rejected on ${request.channel} channel")
            )
        }
    }
}
