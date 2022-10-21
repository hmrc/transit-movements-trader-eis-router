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

import akka.stream.Materializer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.Scenario
import config.AppConfig
import config.RetryConfig
import models.Movement
import models.RoutingOption
import models.RoutingOption.Gb
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import retry.RetryPolicies
import retry.RetryPolicy
import services.RetriesService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.client.RequestBuilder

import java.time.LocalDateTime
import java.time.ZoneOffset
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.failed

class MessageConnectorSpec
    extends AnyWordSpec
    with Matchers
    with WiremockSuite
    with ScalaFutures
    with MockitoSugar
    with IntegrationPatience
    with ScalaCheckPropertyChecks
    with TableDrivenPropertyChecks {

  private object NoRetries extends RetriesService {

    override def createRetryPolicy(config: RetryConfig)(implicit
      ec: ExecutionContext
    ): RetryPolicy[Future] =
      RetryPolicies.alwaysGiveUp[Future](cats.implicits.catsStdInstancesForFuture(ec))
  }

  private object OneRetry extends RetriesService {

    override def createRetryPolicy(config: RetryConfig)(implicit
      ec: ExecutionContext
    ): RetryPolicy[Future] =
      RetryPolicies.limitRetries[Future](1)(cats.implicits.catsStdInstancesForFuture(ec))
  }

  lazy val appWithNoRetries: GuiceApplicationBuilder =
    appBuilder.bindings(bind[RetriesService].toInstance(NoRetries))

  lazy val appWithOneRetry: GuiceApplicationBuilder =
    appBuilder.bindings(bind[RetriesService].toInstance(OneRetry))

  lazy val appBuilderGen: Gen[GuiceApplicationBuilder] =
    Gen.oneOf(appWithOneRetry, appWithNoRetries)

  "post" should {

    "add CustomProcessHost and X-Correlation-Id headers to messages for GB" in forAll(
      appBuilderGen
    ) {
      appBuilder =>
        server.resetAll()
        val app = appBuilder.build()

        running(app) {

          // Important note: while this test considers successes, as this connector has a retry function,
          // we have to ensure that any success result is not retried. To do this, we make the stub return
          // a 202 status the first time it is called, then we transition it into a state where it'll return
          // an error. As the retry algorithm should not attempt a retry on a 202, the stub should only be
          // called once - so a 500 should never be returned.
          //
          // If a 500 error is returned, this most likely means a retry happened, the first place to look
          // should be the code the determines if a result is successful.

          def stub(currentState: String, targetState: String, codeToReturn: Int) =
            server.stubFor(
              post(
                urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures/gb")
              )
                .inScenario("Standard Call")
                .whenScenarioStateIs(currentState)
                .withHeader(
                  "X-Correlation-Id",
                  matching(
                    "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b"
                  )
                )
                .withHeader("CustomProcessHost", equalTo("Digital"))
                .withHeader(HeaderNames.ACCEPT, equalTo("application/xml"))
                .willReturn(aResponse().withStatus(codeToReturn))
                .willSetStateTo(targetState)
            )

          val connector = app.injector.instanceOf[MessageConnector]

          val secondState = "should now fail"

          stub(Scenario.STARTED, secondState, ACCEPTED)
          stub(secondState, secondState, INTERNAL_SERVER_ERROR)

          val hc = HeaderCarrier()

          val result = connector.post(<document></document>, Gb, hc).futureValue

          result.status mustEqual ACCEPTED
        }
    }

    "add CustomProcessHost and X-Correlation-Id headers to messages for XI" in forAll(
      appBuilderGen
    ) {
      appBuilder =>
        server.resetAll()
        val app = appBuilder.build()

        running(app) {
          def stub(currentState: String, targetState: String, codeToReturn: Int) =
            server.stubFor(
              post(
                urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures/ni")
              )
                .withHeader(
                  "X-Correlation-Id",
                  matching(
                    "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b"
                  )
                )
                .withHeader("CustomProcessHost", equalTo("Digital"))
                .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.XML))
                .inScenario("Standard Call")
                .whenScenarioStateIs(currentState)
                .willReturn(aResponse().withStatus(codeToReturn))
                .willSetStateTo(targetState)
            )

          val connector = app.injector.instanceOf[MessageConnector]

          val secondState = "should now fail"

          stub(Scenario.STARTED, secondState, ACCEPTED)
          stub(secondState, secondState, INTERNAL_SERVER_ERROR)

          val hc = HeaderCarrier()

          val result = connector.post(<document></document>, RoutingOption.Xi, hc).futureValue

          result.status mustEqual ACCEPTED
        }
    }

    "return ACCEPTED when post is successful" in forAll(appBuilderGen) {
      appBuilder =>
        server.resetAll()
        val app = appBuilder.build()

        running(app) {
          def stub(currentState: String, targetState: String, codeToReturn: Int) =
            server.stubFor(
              post(
                urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures/gb")
              ).withHeader("Authorization", equalTo("Bearer bearertokenhereGB"))
                .withHeader(HeaderNames.ACCEPT, equalTo("application/xml"))
                .withHeader(
                  "X-Correlation-Id",
                  matching(
                    "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b"
                  )
                )
                .inScenario("Standard Call")
                .whenScenarioStateIs(currentState)
                .willReturn(aResponse().withStatus(codeToReturn))
                .willSetStateTo(targetState)
            )

          val connector = app.injector.instanceOf[MessageConnector]

          val secondState = "should now fail"

          stub(Scenario.STARTED, secondState, ACCEPTED)
          stub(secondState, secondState, INTERNAL_SERVER_ERROR)

          val hc = HeaderCarrier()

          val result = connector.post(<document></document>, Gb, hc).futureValue

          result.status mustEqual ACCEPTED
        }
    }

    "return ACCEPTED when post is successful on retry if there is an initial failure" in {
      val app = appWithOneRetry.build()

      running(app) {
        def stub(currentState: String, targetState: String, codeToReturn: Int) =
          server.stubFor(
            post(
              urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures/gb")
            )
              .inScenario("Flaky Call")
              .whenScenarioStateIs(currentState)
              .willSetStateTo(targetState)
              .withHeader("Authorization", equalTo("Bearer bearertokenhereGB"))
              .withHeader(HeaderNames.ACCEPT, equalTo("application/xml"))
              .withHeader(
                "X-Correlation-Id",
                matching("\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b")
              )
              .willReturn(aResponse().withStatus(codeToReturn))
          )

        val connector = app.injector.instanceOf[MessageConnector]

        val secondState = "should now succeed"

        stub(Scenario.STARTED, secondState, INTERNAL_SERVER_ERROR)
        stub(secondState, secondState, ACCEPTED)

        val hc = HeaderCarrier()

        val result = connector.post(<document></document>, Gb, hc).futureValue

        result.status mustEqual ACCEPTED
      }
    }

    val errorCodes = Gen.oneOf(
      Seq(
        BAD_REQUEST,
        FORBIDDEN,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        GATEWAY_TIMEOUT
      )
    )

    "pass through error status codes" in forAll(errorCodes, appBuilderGen) {
      (statusCode, appBuilder) =>
        val app = appBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[MessageConnector]

          server.stubFor(
            post(
              urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures/gb")
            ).withHeader("Authorization", equalTo("Bearer bearertokenhereGB"))
              .withHeader(HeaderNames.ACCEPT, equalTo("application/xml"))
              .withHeader(
                "X-Correlation-Id",
                matching(
                  "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b"
                )
              )
              .willReturn(aResponse().withStatus(statusCode))
          )

          val hc = HeaderCarrier()

          val result = connector.post(<document></document>, Gb, hc).futureValue

          result.status mustEqual statusCode
        }
    }

    "handle exceptions by returning an HttpResponse with status code 500" in forAll(appBuilderGen) {
      appBuilder =>
        val app = appBuilder.build()

        running(app) {
          implicit val materializer = app.injector.instanceOf[Materializer]
          val appConfig             = app.injector.instanceOf[AppConfig]
          val config                = app.injector.instanceOf[Configuration]
          val retriesService        = app.injector.instanceOf[RetriesService]
          val http                  = mock[HttpClient]
          val httpv2                = mock[HttpClientV2]

          val requestBuilder = mock[RequestBuilder]

          when(
            httpv2.post(any())(any())
          ).thenReturn(requestBuilder)

          when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)
          when(requestBuilder.transform(any())).thenReturn(requestBuilder)
          when(requestBuilder.execute(any(), any())).thenReturn(failed(new RuntimeException("Simulated timeout")))

          val connector = new MessageConnector(appConfig, config, http, httpv2, retriesService)
          val hc        = HeaderCarrier()
          val result    = connector.post(<document></document>, Gb, hc)

          result.futureValue.status mustEqual INTERNAL_SERVER_ERROR
        }
    }
  }

  "postNCTSMonitoring" should {

    "return 200 when post is successful" in forAll(appBuilderGen) {
      appBuilder =>
        val app = appBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[MessageConnector]

          val testMovement = Movement(
            "testXMsgSender",
            "TEST-ID",
            LocalDateTime.ofEpochSecond(1638349126L, 0, ZoneOffset.UTC),
            Gb.prefix
          )

          val testRequestBody = Json.toJson(testMovement)

          server.stubFor(
            post(urlEqualTo("/ncts/movement-notification"))
              .withHeader("X-Message-Sender", equalTo("testXMsgSender"))
              .withRequestBody(equalToJson(testRequestBody.toString()))
              .willReturn(aResponse())
          )

          val result = connector
            .postNCTSMonitoring(
              testMovement.messageCode,
              testMovement.timestamp,
              RoutingOption.Gb,
              HeaderCarrier().withExtraHeaders("X-Message-Sender" -> "testXMsgSender")
            )
            .futureValue

          result mustEqual OK
        }
    }

    val errorCodes = Gen.oneOf(
      Seq(
        BAD_REQUEST,
        FORBIDDEN,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        GATEWAY_TIMEOUT
      )
    )

    "pass through error status codes" in forAll(errorCodes, appBuilderGen) {
      (statusCode, appBuilder) =>
        val app = appBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[MessageConnector]

          server.stubFor(
            post(
              urlEqualTo("/ncts/movement-notification")
            ).willReturn(aResponse().withStatus(statusCode))
          )

          val result = connector
            .postNCTSMonitoring(
              "TEST-ID",
              LocalDateTime.ofEpochSecond(1638349126L, 0, ZoneOffset.UTC),
              Gb,
              HeaderCarrier()
            )
            .futureValue

          result mustEqual statusCode
        }
    }

    "handle exceptions by returning an HttpResponse with status code 500" in forAll(appBuilderGen) {
      appBuilder =>
        val app = appBuilder.build()

        running(app) {
          implicit val materializer = app.injector.instanceOf[Materializer]
          val appConfig             = app.injector.instanceOf[AppConfig]
          val config                = app.injector.instanceOf[Configuration]
          val retriesService        = app.injector.instanceOf[RetriesService]
          val http                  = mock[HttpClient]
          val httpv2                = mock[HttpClientV2]

          when(
            http.POSTString(
              any(): String,
              any(): String,
              any(): Seq[(String, String)]
            )(
              any(): HttpReads[HttpResponse],
              any(): HeaderCarrier,
              any(): ExecutionContext
            )
          ).thenReturn(failed(new RuntimeException("Simulated timeout")))

          val connector = new MessageConnector(appConfig, config, http, httpv2, retriesService)
          val result = connector
            .postNCTSMonitoring(
              "TEST-ID",
              LocalDateTime.ofEpochSecond(1638349126L, 0, ZoneOffset.UTC),
              Gb,
              HeaderCarrier()
            )
            .futureValue

          result mustEqual INTERNAL_SERVER_ERROR
        }
    }

  }
}
