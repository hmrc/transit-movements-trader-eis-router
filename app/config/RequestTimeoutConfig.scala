/*
 * Copyright 2022 HM Revenue & Customs
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

package config

import play.api.Configuration

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.FiniteDuration

case class RequestTimeoutConfig(messageSizeLimit: Int, smallMessageTimeout: FiniteDuration, largeMessageTimeout: FiniteDuration) {
  def timeout(message: String): FiniteDuration = timeout(message.getBytes(StandardCharsets.UTF_8).length)

  def timeout(messageSize: Int): FiniteDuration =
    if (messageSize <= messageSizeLimit) smallMessageTimeout
    else largeMessageTimeout
}

object RequestTimeoutConfig {

  def fromServicesConfig(serviceName: String, config: Configuration) =
    RequestTimeoutConfig(
      config.get[Int](
        s"microservice.services.$serviceName.request-timeout.small-message-size-limit"
      ),
      config.get[FiniteDuration](
        s"microservice.services.$serviceName.request-timeout.small-message-timeout"
      ),
      config.get[FiniteDuration](
        s"microservice.services.$serviceName.request-timeout.large-message-timeout"
      )
    )
}
