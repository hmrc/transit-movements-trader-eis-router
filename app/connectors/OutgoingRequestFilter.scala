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

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

object OutgoingRequestFilter {
  val CustomUpstreamHeaders = Seq(
    "X-Forwarded-Host",
    "X-Correlation-ID",
    "Date",
    "Content-Type",
    "Accept",
    "X-Message-Type",
    "X-Message-Sender"
  )

  def retainOnlyCustomUpstreamHeaders()(implicit requestHeader: RequestHeader): Seq[(String, String)] = {
    requestHeader.headers.headers.filter(x => CustomUpstreamHeaders.contains(x._1))
  }

  def enforceAuthHeaderCarrier()(implicit requestHeader: RequestHeader, headerCarrier: HeaderCarrier): HeaderCarrier = {
    val newHeaderCarrier = headerCarrier
      .copy(authorization = Some(Authorization(requestHeader.headers.get(HeaderNames.AUTHORIZATION).getOrElse(""))))
    newHeaderCarrier
  }
}
