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
import models.RoutingOption.Xi
import models.requests.ChannelRequest
import models.{FailureMessage, MessageType, Office, ParseError, ParseHandling, PresentationOffice, RejectionMessage, RoutingOption}
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import scala.xml.NodeSeq

class RoutingService @Inject() (fsrc: FeatureSwitchRouteChecker, messageConnector: MessageConnector) extends ParseHandling {

  val logger = Logger(this.getClass)

  def submitMessage(xml: NodeSeq)(implicit request: ChannelRequest[NodeSeq], requestHeader: RequestHeader, headerCarrier: HeaderCarrier): Either[FailureMessage, Future[HttpResponse]] = {
    XmlParser.getValidRoot(xml) match {
      case None => Left(InvalidMessageCode(s"Invalid Message Type"))
      case Some((rootXml, messageType)) =>
        val officeEither: Either[ParseError, Office] = if(MessageType.arrivalValues.contains(messageType)) {
          XmlParser.officeOfPresentation(rootXml)
        }
        else {
          XmlParser.officeOfDeparture(rootXml)
        }

        officeEither.flatMap {
          office =>
            val routingOption = office.getRoutingOption
            if(fsrc.canForward(routingOption, request.channel)) {
              Right(messageConnector.post(xml.toString(), routingOption))
            }
            else {
              Left(RejectionMessage(s"Routing to ${office.value.splitAt(2)._1} rejected on ${request.channel} channel"))
            }
        }
    }
  }
}
