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

import scala.concurrent.duration.FiniteDuration

case class RetryConfig(maxRetries: Int, delay: FiniteDuration, timeout: FiniteDuration)

object RetryConfig {

  def fromServicesConfig(serviceName: String, config: Configuration) =
    RetryConfig(
      config.get[Int](
        s"microservice.services.$serviceName.retry.max-retries"
      ),
      config.get[FiniteDuration](
        s"microservice.services.$serviceName.retry.delay-between-retries"
      ),
      config.get[FiniteDuration](
        s"microservice.services.$serviceName.retry.timeout"
      )
    )
}
