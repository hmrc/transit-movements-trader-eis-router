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
import models.ParseError.{DepartureEmpty, DestinationEmpty, InvalidMessageCode}
import models.{DepartureOffice, DestinationOffice, MessageType, Office, ParseError, ParseHandling}
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import scala.xml.NodeSeq

class RoutingService @Inject() (appConfig: AppConfig, messageConnector: MessageConnector) extends ParseHandling {

  def submitMessage(xml: NodeSeq)(implicit requestHeader: RequestHeader, headerCarrier: HeaderCarrier): Either[ParseError, Future[HttpResponse]] = {

    MessageType.allMessages.filter(x => x.rootNode == xml.head.label).headOption match {
      case None => Left(InvalidMessageCode(s"Invalid Message Type: ${xml.head.label}"))
      case Some(messageType) =>
        val officeEither: Either[ParseError, Office] = if(MessageType.arrivalValues.contains(messageType)) {
          officeOfDestination(xml)
        }
        else {
          officeOfDeparture(xml)
        }

        officeEither.map {
          office =>
            if(office.value.startsWith("XI")) {
              Logger.info("routing to NI")
              messageConnector.post(xml.toString(), appConfig.eisniUrl, appConfig.eisniBearerToken)
            }
            else {
              Logger.info("routing to GB")
              messageConnector.post(xml.toString(), appConfig.eisgbUrl, appConfig.eisgbBearerToken)
            }
        }
    }
  }

  private val officeOfDeparture: ReaderT[ParseHandler, NodeSeq, DepartureOffice] =
    ReaderT[ParseHandler, NodeSeq, DepartureOffice](xml => {
      (xml \ "CUSOFFDEPEPT" \ "RefNumEPT1").text match {
        case departure if departure.isEmpty =>Left(DepartureEmpty("Departure Empty"))
        case departure => Right(DepartureOffice(departure))
      }
    })

  private val officeOfDestination: ReaderT[ParseHandler, NodeSeq, DestinationOffice] =
    ReaderT[ParseHandler, NodeSeq, DestinationOffice](xml => {
      (xml \ "CUSOFFDESEST" \ "RefNumEST1").text match {
        case destination if destination.isEmpty =>Left(DestinationEmpty("Destination Empty"))
        case destination => Right(DestinationOffice(destination))
      }
    })
}
