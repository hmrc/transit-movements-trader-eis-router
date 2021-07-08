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
import logging.Logging
import models.RoutingOption
import models.RoutingOption.Gb
import models.RoutingOption.Xi
import models.requests.ChannelRequest
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.xml.NodeSeq

class MessageConnector @Inject() (config: AppConfig, http: HttpClient) extends Logging {

  private case class EisDetails(url: String, token: String, routingMessage: String)

  def post(request: ChannelRequest[NodeSeq], routingOption: RoutingOption, headerCarrier: HeaderCarrier)(implicit
    ec: ExecutionContext
  ): Future[HttpResponse] = {
    val details = routingOption match {
      case Xi => EisDetails(config.eisniUrl, config.eisniBearerToken, "routing to NI")
      case Gb => EisDetails(config.eisgbUrl, config.eisgbBearerToken, "routing to GB")
    }

    implicit val headerCarrierWithEisBearerToken = headerCarrier.copy(authorization = Some(Authorization(s"Bearer ${details.token}")))

    val correlationId = UUID.randomUUID().toString

    val customHeaders = OutgoingHeadersFilter.headersFromRequest(request) ++ Seq(
      "X-Correlation-Id"  -> correlationId,
      "CustomProcessHost" -> "Digital"
    )

    logger.info(s"Posting NCTS message with correlation ID $correlationId to ${details.url}, ${details.routingMessage}")

    http.POSTString[HttpResponse](details.url, request.body.toString, customHeaders)
  }
}
