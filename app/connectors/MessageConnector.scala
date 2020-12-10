/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent.{ExecutionContext, Future}

class MessageConnector @Inject()(config: AppConfig, http: HttpClient)(implicit ec: ExecutionContext) {

  def post(xml: String)(implicit requestHeader: RequestHeader, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val url = config.eisUrl

    val customHeaders = OutgoingRequestFilter() ++ extraHeaders

    val newHeaderCarrier = headerCarrier
      .copy(authorization = Some(Authorization(s"Bearer ${config.eisBearerToken}")))
      .withExtraHeaders(customHeaders: _*)

    http.POSTString[HttpResponse](url, xml)(CustomHttpReader, newHeaderCarrier, implicitly)
  }

  private def extraHeaders: Seq[(String, String)] =
    Seq(
      "X-Correlation-Id" -> UUID.randomUUID().toString,
      "CustomProcessHost" -> "Digital"
    )
}