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

import java.util.UUID

import com.google.inject.Inject
import config.AppConfig
import connectors.util.CustomHttpReader
import models.RoutingOption.{Gb, Xi}
import models.RoutingOption
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent.{ExecutionContext, Future}
import logging.Logging

class MessageConnector @Inject()(config: AppConfig, http: HttpClient)(implicit ec: ExecutionContext) extends Logging {

  private def extraHeaders: Seq[(String, String)] =
    Seq(
      "X-Correlation-Id" -> UUID.randomUUID().toString,
      "CustomProcessHost" -> "Digital"
    )

  private case class EisDetails(url: String, token: String, routingMessage: String)

  def post(xml: String, rOption: RoutingOption)(implicit requestHeader: RequestHeader, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val details = (rOption match {
      case Xi => EisDetails(config.eisniUrl, config.eisniBearerToken, "routing to NI")
      case Gb => EisDetails(config.eisgbUrl, config.eisgbBearerToken, "routing to GB")
    })

    val customHeaders = OutgoingRequestFilter() ++ extraHeaders
    val newHeaderCarrier = headerCarrier
      .copy(authorization = Some(Authorization(s"Bearer ${details.token}")))
      .withExtraHeaders(customHeaders: _*)


    def getHeader(header: String): String = 
      newHeaderCarrier
        .headers
        .find(_._1.toUpperCase == header.toUpperCase)
        .map(_._2)
        .getOrElse("undefined")


    http.POSTString[HttpResponse](details.url, xml)(CustomHttpReader, newHeaderCarrier, implicitly) map {
      case result =>

        val logMessage =
          s"""|Posting NCTS message, ${details.routingMessage}
              |X-Correlation-ID: ${getHeader("X-Correlation-Id")}
              |X-Request-ID: ${getHeader("X-Request-Id")}
              |X-Message-Type: ${getHeader("X-Message-Type")}
              |X-Message-Sender: ${getHeader("X-Message-Sender")}
              |Response status: ${result.status}
              """.stripMargin

        if (result.status < 500 || result.status != 403) logger.info(logMessage)           
        else logger.warn(logMessage)

        result
    }
  }
}
