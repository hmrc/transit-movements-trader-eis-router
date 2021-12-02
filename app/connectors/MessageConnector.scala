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

package connectors

import com.google.inject.Inject
import config.AppConfig
import models.{Movement, RoutingOption}
import models.RoutingOption.Gb
import models.RoutingOption.Xi
import play.api.Configuration
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.{HeaderNames => HMRCHeaderNames}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.NodeSeq

class MessageConnector @Inject() (appConfig: AppConfig, config: Configuration, http: HttpClient)(
  implicit ec: ExecutionContext
) extends Logging {

  private case class EisDetails(url: String, token: String, routingMessage: String)

  private val headerCarrierConfig = HeaderCarrier.Config.fromConfig(config.underlying)

  def getHeader(header: String, url: String)(implicit hc: HeaderCarrier): String =
    hc
      .headersForUrl(headerCarrierConfig)(url)
      .find { case (name, _) => name.toLowerCase == header.toLowerCase }
      .map { case (_, value) => value }
      .getOrElse("undefined")

  def post(xml: NodeSeq, routingOption: RoutingOption, hc: HeaderCarrier): Future[HttpResponse] = {
    val details = routingOption match {
      case Xi => EisDetails(appConfig.eisniUrl, appConfig.eisniBearerToken, "routing to NI")
      case Gb => EisDetails(appConfig.eisgbUrl, appConfig.eisgbBearerToken, "routing to GB")
    }

    val requestHeaders = hc.headers(OutgoingHeaders.headers) ++ Seq(
      "X-Correlation-Id" -> UUID.randomUUID().toString,
      "CustomProcessHost" -> "Digital",
      HeaderNames.ACCEPT -> MimeTypes.XML, // can't use ContentTypes.XML because EIS will not accept "application/xml; charset=utf-8"
      HeaderNames.AUTHORIZATION -> s"Bearer ${details.token}"
    )

    implicit val headerCarrier = hc
      .copy(authorization = None, otherHeaders = Seq.empty)
      .withExtraHeaders(requestHeaders: _*)

      http
        .POSTString[HttpResponse](details.url, xml.toString)
        .map { result =>
          lazy val logMessage =
            s"""|Posting NCTS message, ${details.routingMessage}
                |X-Correlation-Id: ${getHeader("X-Correlation-Id", details.url)}
                |${HMRCHeaderNames.xRequestId}: ${getHeader(HMRCHeaderNames.xRequestId, details.url)}
                |X-Message-Type: ${getHeader("X-Message-Type", details.url)}
                |X-Message-Sender: ${getHeader("X-Message-Sender", details.url)}
                |Accept: ${getHeader("Accept", details.url)}
                |CustomProcessHost: ${getHeader("CustomProcessHost", details.url)}
                |Response status: ${result.status}
              """.stripMargin

          if (Status.isServerError(result.status) || result.status == Status.FORBIDDEN)
            logger.warn(logMessage)
          else
            logger.info(logMessage)

          result
        }
        .recover {
          case NonFatal(e) =>
            val message = s"${details.url} failed to retrieve data with message ${e.getMessage}"
            logger.warn(message)
            HttpResponse(Status.INTERNAL_SERVER_ERROR, message)
        }
    }

  def postNCTSMonitoring(messageCode: String, timestamp: LocalDateTime,
                         routingOption: RoutingOption, hc: HeaderCarrier): Future[HttpResponse] = {

    implicit val headerCarrier: HeaderCarrier = hc

    val movementJson: JsValue =
      Json.toJson(Movement(getHeader("X-Message-Sender", appConfig.nctsMonitoringUrl), messageCode, timestamp, routingOption.prefix))

    http
      .POSTString[HttpResponse](appConfig.nctsMonitoringUrl, movementJson.toString())
      .map { result =>
        if (result.status != Status.OK)
          logger.warn(s"[MessageConnector][postNCTSMonitoring] Failed with status ${result.status}")
        result
      }
      .recover {
        case NonFatal(e) =>
          val message = s"${appConfig.nctsMonitoringUrl} failed to send movement to ncts monitoring with message ${e.getMessage}"
          logger.warn(message)
          HttpResponse(Status.INTERNAL_SERVER_ERROR, message)
      }
  }
}
