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

import akka.pattern.CircuitBreaker
import akka.stream.Materializer
import config.CircuitBreakerConfig
import play.api.Logging

import scala.concurrent.duration.Duration

trait CircuitBreakers { self: Logging =>
  def materializer: Materializer
  def gbCircuitBreakerConfig: CircuitBreakerConfig
  def niCircuitBreakerConfig: CircuitBreakerConfig

  private val clazz = getClass.getSimpleName

  lazy val gbCircuitBreaker = new CircuitBreaker(
    scheduler = materializer.system.scheduler,
    maxFailures = gbCircuitBreakerConfig.maxFailures,
    callTimeout = gbCircuitBreakerConfig.callTimeout,
    resetTimeout = gbCircuitBreakerConfig.resetTimeout,
    maxResetTimeout = gbCircuitBreakerConfig.maxResetTimeout,
    exponentialBackoffFactor = gbCircuitBreakerConfig.exponentialBackoffFactor,
    randomFactor = gbCircuitBreakerConfig.randomFactor
  )(materializer.executionContext)
    .onOpen(logger.error(s"GB Circuit breaker for ${clazz} opening due to failures"))
    .onHalfOpen(logger.warn(s"GB Circuit breaker for ${clazz} resetting after failures"))
    .onClose {
      logger.warn(
        s"GB Circuit breaker for ${clazz} closing after trial connection success"
      )
    }
    .onCallFailure(_ => logger.error(s"GB Circuit breaker for ${clazz} recorded failed call"))
    .onCallBreakerOpen {
      logger.error(
        s"GB Circuit breaker for ${clazz} rejected call due to previous failures"
      )
    }
    .onCallTimeout { elapsed =>
      val duration = Duration.fromNanos(elapsed)
      logger.error(
        s"GB Circuit breaker for ${clazz} recorded failed call due to timeout after ${duration.toMillis}ms"
      )
    }

  lazy val niCircuitBreaker = new CircuitBreaker(
    scheduler = materializer.system.scheduler,
    maxFailures = niCircuitBreakerConfig.maxFailures,
    callTimeout = niCircuitBreakerConfig.callTimeout,
    resetTimeout = niCircuitBreakerConfig.resetTimeout,
    maxResetTimeout = niCircuitBreakerConfig.maxResetTimeout,
    exponentialBackoffFactor = niCircuitBreakerConfig.exponentialBackoffFactor,
    randomFactor = niCircuitBreakerConfig.randomFactor
  )(materializer.executionContext)
    .onOpen(logger.error(s"NI Circuit breaker for ${clazz} opening due to failures"))
    .onHalfOpen(logger.warn(s"NI Circuit breaker for ${clazz} resetting after failures"))
    .onClose {
      logger.warn(
        s"NI Circuit breaker for ${clazz} closing after trial connection success"
      )
    }
    .onCallFailure(_ => logger.error(s"NI Circuit breaker for ${clazz} recorded failed call"))
    .onCallBreakerOpen {
      logger.error(
        s"NI Circuit breaker for ${clazz} rejected call due to previous failures"
      )
    }
    .onCallTimeout { elapsed =>
      val duration = Duration.fromNanos(elapsed)
      logger.error(
        s"NI Circuit breaker for ${clazz} recorded failed call due to timeout after ${duration.toMillis}ms"
      )
    }

}
