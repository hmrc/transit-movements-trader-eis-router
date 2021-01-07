package services

import cats.data.ReaderT
import com.google.inject.Inject
import config.AppConfig
import connectors.MessageConnector
import models.ParseError.{DepartureEmpty, DestinationEmpty}
import models.{DepartureOffice, DestinationOffice, MessageType, Office, ParseError, ParseHandling}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import scala.xml.NodeSeq

class RoutingService @Inject() (appConfig: AppConfig, messageConnector: MessageConnector) extends ParseHandling {

  def submitMessage(xml: NodeSeq)(implicit requestHeader: RequestHeader, headerCarrier: HeaderCarrier): Either[String, Future[HttpResponse]] = {

    MessageType.allMessages.filter(x => x.rootNode == xml.head.label).headOption match {
      case None => Left(s"Invalid Message Type: ${xml.head.label}")
      case Some(messageType) =>
        val officeEither: Either[ParseError, Office] = if(MessageType.departureValues.contains(messageType)) {
          officeOfDestination(xml)
        }
        else {
          officeOfDeparture(xml)
        }

        officeEither match {
          case Left(error) => Left(error.message)
          case Right(office) => {
            if(office.value.startsWith("XI")) {
              Right(messageConnector.post(xml.toString(), appConfig.eisniUrl, appConfig.eisniBearerToken))
            }
            else {
              Right(messageConnector.post(xml.toString(), appConfig.eisgbUrl, appConfig.eisgbBearerToken))
            }
          }
        }
    }
  }

  val officeOfDeparture: ReaderT[ParseHandler, NodeSeq, DepartureOffice] =
    ReaderT[ParseHandler, NodeSeq, DepartureOffice](xml => {
      (xml \ "CUSOFFDEPEPT" \ "RefNumEPT1").text match {
        case departure if departure.isEmpty =>Left(DepartureEmpty("Departure Empty"))
        case departure => Right(DepartureOffice(departure))
      }
    })

  val officeOfDestination: ReaderT[ParseHandler, NodeSeq, DestinationOffice] =
    ReaderT[ParseHandler, NodeSeq, DestinationOffice](xml => {
      (xml \ "CUSOFFDESEST" \ "RefNumEST1").text match {
        case destination if destination.isEmpty =>Left(DestinationEmpty("Destination Empty"))
        case destination => Right(DestinationOffice(destination))
      }
    })
}
