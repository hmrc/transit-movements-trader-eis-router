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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.ChannelType
import models.RoutingOption.Gb
import models.RoutingOption.Xi
import models.requests.ChannelRequest
import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.NodeSeq

class MessageConnectorSpec
    extends AnyWordSpec
    with Matchers
    with WiremockSuite
    with ScalaFutures
    with MockitoSugar
    with IntegrationPatience
    with ScalaCheckDrivenPropertyChecks {
  "post" should {

    "add CustomProcessHost and X-Correlation-Id headers to messages for GB" in {

      val app = appBuilder.build()

      running(app) {

        val connector = app.injector.instanceOf[MessageConnector]

        val hc            = HeaderCarrier()
        val requestHeader = FakeRequest().withBody(<document></document>)
        val request       = ChannelRequest[NodeSeq](requestHeader, ChannelType.Api)

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures/gb")
          )
            .withHeader("X-Correlation-Id", matching("\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b"))
            .withHeader("CustomProcessHost", equalTo("Digital"))
            .willReturn(aResponse().withStatus(ACCEPTED))
        )

        val result = connector.post(request, Gb, hc).futureValue

        result.status mustEqual ACCEPTED
      }
    }

    "add CustomProcessHost and X-Correlation-Id headers to messages for XI" in {

      val app = appBuilder.build()

      running(app) {

        val connector = app.injector.instanceOf[MessageConnector]

        val hc            = HeaderCarrier()
        val requestHeader = FakeRequest().withBody(<document></document>)
        val request       = ChannelRequest[NodeSeq](requestHeader, ChannelType.Api)

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures/ni")
          )
            .withHeader("X-Correlation-Id", matching("\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b"))
            .withHeader("CustomProcessHost", equalTo("Digital"))
            .willReturn(aResponse().withStatus(ACCEPTED))
        )

        val result = connector.post(request, Xi, hc).futureValue

        result.status mustEqual ACCEPTED
      }
    }

    "return ACCEPTED when post is successful" in {
      val app = appBuilder.build()

      running(app) {
        val connector = app.injector.instanceOf[MessageConnector]

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures/gb")
          ).withHeader("Authorization", equalTo("Bearer bearertokenhereGB")).willReturn(aResponse().withStatus(ACCEPTED))
        )

        val hc            = HeaderCarrier()
        val requestHeader = FakeRequest().withBody(<document></document>)
        val request       = ChannelRequest[NodeSeq](requestHeader, ChannelType.Api)

        val result = connector.post(request, Gb, hc).futureValue

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

    "pass through error status codes" in forAll(errorCodes) {
      statusCode =>
        val app = appBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[MessageConnector]

          server.stubFor(
            post(
              urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures/gb")
            ).withHeader("Authorization", equalTo("Bearer bearertokenhereGB")).willReturn(aResponse().withStatus(statusCode))
          )

          val hc            = HeaderCarrier()
          val requestHeader = FakeRequest().withBody(<document></document>)
          val request       = ChannelRequest[NodeSeq](requestHeader, ChannelType.Api)

          val result = connector.post(request, Gb, hc).futureValue

          result.status mustEqual statusCode
        }
    }
  }

  override protected def portConfigKey: String = "microservice.services.eis.port"
}
