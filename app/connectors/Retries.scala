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

package connectors

import com.google.inject.ImplementedBy
import config.RetryConfig
import retry.RetryPolicies
import retry.RetryPolicy

import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[RetriesImpl])
trait Retries {

  def createRetryPolicy(config: RetryConfig)(implicit ec: ExecutionContext): RetryPolicy[Future]

}

@Singleton
class RetriesImpl extends Retries {

  override def createRetryPolicy(config: RetryConfig)(implicit ec: ExecutionContext): RetryPolicy[Future] =
    RetryPolicies.limitRetriesByCumulativeDelay(
      config.timeout,
      RetryPolicies.limitRetries[Future](config.maxRetries) join RetryPolicies.constantDelay[Future](config.delay)
    )

}
