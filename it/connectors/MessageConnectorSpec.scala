package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class MessageConnectorSpec extends AnyWordSpec with Matchers with WiremockSuite with ScalaFutures with MockitoSugar with IntegrationPatience {
  "post" should {

    "add CustomProcessHost and X-Correlation-Id headers to messages" in {

      val app = appBuilder.build()

      running(app) {

        val connector = app.injector.instanceOf[MessageConnector]
        val config = app.injector.instanceOf[AppConfig]

        implicit val hc = HeaderCarrier()
        implicit val requestHeader = FakeRequest()

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
          )
          .withHeader("X-Correlation-Id", matching("\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b"))
          .withHeader("CustomProcessHost", equalTo("Digital"))
          .willReturn(aResponse().withStatus(ACCEPTED))
        )

        val result = connector.post("<document></document>", config.eisgbUrl, config.eisgbBearerToken).futureValue

        result.status mustEqual ACCEPTED
      }
    }

    "return ACCEPTED when post is successful" in {
      val app = appBuilder.build()

      running(app) {
        val connector = app.injector.instanceOf[MessageConnector]
        val config = app.injector.instanceOf[AppConfig]

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
          ).withHeader("Authorization", equalTo("Bearer bearertokenhere")).willReturn(aResponse().withStatus(ACCEPTED))
        )

        implicit val hc = HeaderCarrier()
        implicit val requestHeader = FakeRequest()

        val result = connector.post("<document></document>", config.eisgbUrl, config.eisgbBearerToken).futureValue

        result.status mustEqual ACCEPTED
      }
    }

    "return BAD_GATEWAY when the server returns INTERNAL_SERVER_ERROR" in {

      val app = appBuilder.build()

      running(app) {
        val connector = app.injector.instanceOf[MessageConnector]
        val config = app.injector.instanceOf[AppConfig]

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
          ).willReturn(serverError())
        )

        implicit val hc = HeaderCarrier()
        implicit val requestHeader = FakeRequest()

        val result = connector.post("<document></document>", config.eisgbUrl, config.eisgbBearerToken).futureValue

        result.status mustEqual BAD_GATEWAY
      }
    }

    "return BAD_GATEWAY when the server returns GATEWAY_TIMEOUT" in {

      val app = appBuilder.build()

      running(app) {
        val connector = app.injector.instanceOf[MessageConnector]
        val config = app.injector.instanceOf[AppConfig]

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
          ).willReturn(aResponse().withStatus(GATEWAY_TIMEOUT))
        )

        implicit val hc = HeaderCarrier()
        implicit val requestHeader = FakeRequest()

        val result = connector.post("<document></document>", config.eisgbUrl, config.eisgbBearerToken).futureValue

        result.status mustEqual BAD_GATEWAY
      }
    }

    "return BAD_REQUEST when post returns BAD_REQUEST" in {

      val app = appBuilder.build()

      running(app) {
        val connector = app.injector.instanceOf[MessageConnector]
        val config = app.injector.instanceOf[AppConfig]

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
          ).withHeader("Authorization", equalTo("Bearer bearertokenhere")).willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        implicit val hc = HeaderCarrier()
        implicit val requestHeader = FakeRequest()

        val result = connector.post("<document></document>", config.eisgbUrl, config.eisgbBearerToken).futureValue

        result.status mustEqual BAD_REQUEST
      }
    }

    "return INTERNAL_SERVER_ERROR when post returns UNAUTHORIZED" in {
      val app = appBuilder.build()

      running(app) {
        val connector = app.injector.instanceOf[MessageConnector]
        val config = app.injector.instanceOf[AppConfig]

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
          ).withHeader("Authorization", equalTo("Bearer bearertokenhere")).willReturn(aResponse().withStatus(UNAUTHORIZED))
        )

        implicit val hc = HeaderCarrier()
        implicit val requestHeader = FakeRequest()

        val result = connector.post("<document></document>", config.eisgbUrl, config.eisgbBearerToken).futureValue

        result.status mustEqual INTERNAL_SERVER_ERROR
      }
    }

    "return BAD_GATEWAY when post returns BAD_GATEWAY" in {
      val app = appBuilder.build()

      running(app) {
        val connector = app.injector.instanceOf[MessageConnector]
        val config = app.injector.instanceOf[AppConfig]

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
          ).withHeader("Authorization", equalTo("Bearer bearertokenhere")).willReturn(aResponse().withStatus(BAD_GATEWAY))
        )

        implicit val hc = HeaderCarrier()
        implicit val requestHeader = FakeRequest()

        val result = connector.post("<document></document>", config.eisgbUrl, config.eisgbBearerToken).futureValue

        result.status mustEqual BAD_GATEWAY
      }
    }
  }

  override protected def portConfigKey: String = "microservice.services.eis.port"
}
