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
import com.google.inject.Inject
import config.AppConfig
import config.CircuitBreakerConfig
import config.RequestTimeoutConfig
import config.RetryConfig
import models.Movement
import models.RoutingOption
import models.RoutingOption.Gb
import models.RoutingOption.Xi
import play.api.Configuration
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import retry.RetryDetails
import retry.alleycats.instances._
import retry.retryingOnFailures
import services.RetriesService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderNames => HMRCHeaderNames}

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.NodeSeq

class MessageConnector @Inject() (
  appConfig: AppConfig,
  config: Configuration,
  http: HttpClient,
  httpv2: HttpClientV2,
  retries: RetriesService
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) extends CircuitBreakers
    with Logging {

  private case class EisDetails(
    url: String,
    token: String,
    routingMessage: String,
    circuitBreaker: CircuitBreaker,
    retryConfig: RetryConfig,
    requestTimeoutConfig: RequestTimeoutConfig
  )

  private lazy val niEisDetails =
    EisDetails(
      appConfig.eisniUrl,
      appConfig.eisniBearerToken,
      "routing to NI",
      niCircuitBreaker,
      appConfig.eisniRetry,
      appConfig.eisniTimeout
    )

  private lazy val gbEisDetails =
    EisDetails(
      appConfig.eisgbUrl,
      appConfig.eisgbBearerToken,
      "routing to GB",
      gbCircuitBreaker,
      appConfig.eisgbRetry,
      appConfig.eisgbTimeout
    )

  private val headerCarrierConfig = HeaderCarrier.Config.fromConfig(config.underlying)

  def getHeader(header: String, url: String)(implicit hc: HeaderCarrier): String =
    hc
      .headersForUrl(headerCarrierConfig)(url)
      .find {
        case (name, _) => name.toLowerCase == header.toLowerCase
      }
      .map {
        case (_, value) => value
      }
      .getOrElse("undefined")

  override lazy val gbCircuitBreakerConfig: CircuitBreakerConfig = appConfig.eisgbCircuitBreaker
  override lazy val niCircuitBreakerConfig: CircuitBreakerConfig = appConfig.eisniCircuitBreaker

  private def statusCodeFailure(response: HttpResponse): Boolean =
    shouldCauseRetry(response) || response.status == Status.FORBIDDEN

  private def shouldCauseRetry(response: HttpResponse): Boolean =
    Status.isServerError(response.status)

  def shouldCauseCircuitBreakerStrike[A](result: Try[HttpResponse]): Boolean =
    result match {
      case Success(response) if !shouldCauseRetry(response) => false
      case _                                                => true
    }

  def onFailure(
    instance: String
  )(httpResponse: HttpResponse, retryDetails: RetryDetails): Future[Unit] = {
    val attemptNumber = retryDetails.retriesSoFar + 1
    if (retryDetails.givingUp) {
      logger.error(
        s"Message when $instance failed with status code ${httpResponse.status}. " +
          s"Attempted $attemptNumber times in ${retryDetails.cumulativeDelay.toSeconds} seconds, giving up."
      )
    } else {
      val nextAttempt =
        retryDetails.upcomingDelay
          .map(
            d => s"in ${d.toSeconds} seconds"
          )
          .getOrElse("immediately")
      logger.warn(
        s"Message when $instance failed with status code ${httpResponse.status}. " +
          s"Attempted $attemptNumber times in ${retryDetails.cumulativeDelay.toSeconds} seconds so far, trying again $nextAttempt."
      )
    }
    Future.unit
  }

  def post(xml: NodeSeq, routingOption: RoutingOption, hc: HeaderCarrier): Future[HttpResponse] = {
    val payload = xml.mkString
    post(payload, routingOption, hc, messageSize(payload))
  }

  def post(payload: String, routingOption: RoutingOption, hc: HeaderCarrier, size: Int): Future[HttpResponse] = {
    val details = routingOption match {
      case Xi => niEisDetails
      case Gb => gbEisDetails
    }

    val timeout = details.requestTimeoutConfig.timeout(size)

    // It is assumed that all errors are fatal (see recover block) and so we just need to retry on failures.
    retryingOnFailures(
      retries.createRetryPolicy(details.retryConfig),
      (t: HttpResponse) => Future.successful(!shouldCauseRetry(t)),
      onFailure(details.routingMessage)
    ) {
      val correlationId = UUID.randomUUID().toString
      val requestHeaders = hc.headers(OutgoingHeaders.headers) ++ Seq(
        "X-Correlation-Id"        -> correlationId,
        "CustomProcessHost"       -> "Digital",
        HeaderNames.ACCEPT        -> MimeTypes.XML, // can't use ContentTypes.XML because EIS will not accept "application/xml; charset=utf-8"
        HeaderNames.AUTHORIZATION -> s"Bearer ${details.token}"
      )

      implicit val headerCarrier: HeaderCarrier = hc
        .copy(authorization = None, otherHeaders = Seq.empty)
        .withExtraHeaders(requestHeaders: _*)

      val requestId       = getHeader(HMRCHeaderNames.xRequestId, details.url)
      lazy val logMessage = s"""|Posting NCTS message, ${details.routingMessage}
                                |X-Correlation-Id: $correlationId
                                |${HMRCHeaderNames.xRequestId}: $requestId
                                |X-Message-Type: ${getHeader("X-Message-Type", details.url)}
                                |X-Message-Sender: ${getHeader("X-Message-Sender", details.url)}
                                |Accept: ${getHeader("Accept", details.url)}
                                |CustomProcessHost: ${getHeader("CustomProcessHost", details.url)}
                                |Timestamp (UTC): ${DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))}
                                |""".stripMargin

      details.circuitBreaker
        .withCircuitBreaker(
          httpv2
            .post(url"${details.url}")
            .withBody(payload)
            .transform(
              req => req.withRequestTimeout(timeout)
            )
            .execute[HttpResponse]
            .map {
              result =>
                val logMessageWithStatus = logMessage + s"Response status: ${result.status}"

                if (statusCodeFailure(result))
                  logger.warn(logMessageWithStatus)
                else
                  logger.info(logMessageWithStatus)

                // TODO: TEMPORARY, NEVER TO GO TO PRODUCTION
                if (result.status >= 500) {
                  val headerString = result.headers.map {
                    x =>
                      if (x._1 == HeaderNames.AUTHORIZATION) (x._1, Seq("REDACTED"))
                      else x
                  }.map(x => s"${x._1}: ${x._2.mkString(" | ")}").mkString(System.lineSeparator())
                  logger.error(
                    s"""Additional logging information for request ID: $requestId and correlationId $correlationId
                      |
                      |Headers:
                      |$headerString
                      |
                      |Status: ${result.status}
                      |
                      |Body:
                      |
                      |${result.body}
                      |""".stripMargin)
                }

                result
            }
            .recover {
              case NonFatal(e) =>
                val message = logMessage +
                  s"Request Error: ${details.url} failed to retrieve data with message ${e.getMessage}"
                logger.error(message)
                HttpResponse(Status.INTERNAL_SERVER_ERROR, message)
            },
          shouldCauseCircuitBreakerStrike
        )
        .recover {
          case NonFatal(e) =>
            val message = logMessage + s"Error: Unable to process message ${e.getMessage}"
            logger.error(message)
            HttpResponse(Status.INTERNAL_SERVER_ERROR, message)
        }
    }
  }

  def postNCTSMonitoring(
    messageCode: String,
    timestamp: LocalDateTime,
    routingOption: RoutingOption,
    hc: HeaderCarrier
  ): Future[Int] = {

    implicit val headerCarrier: HeaderCarrier =
      hc.withExtraHeaders(hc.headers(Seq("X-Message-Sender")): _*)

    val movementJson: JsValue =
      Json.toJson(
        Movement(
          getHeader("X-Message-Sender", appConfig.nctsMonitoringUrl),
          messageCode,
          timestamp,
          routingOption.prefix
        )
      )

    http
      .POSTString[HttpResponse](appConfig.nctsMonitoringUrl, movementJson.toString())
      .map {
        result =>
          if (result.status != Status.OK)
            logger.warn(s"[MessageConnector][postNCTSMonitoring] Failed with status ${result.status}")
          result.status
      }
      .recover {
        case NonFatal(e) =>
          val message =
            s"[MessageConnector][postNCTSMonitoring] ${appConfig.nctsMonitoringUrl} failed to send movement to ncts monitoring with message ${e.getMessage}"
          logger.warn(message)
          INTERNAL_SERVER_ERROR
      }
  }

  private def messageSize(message: String) = message.getBytes(StandardCharsets.UTF_8).length
}
