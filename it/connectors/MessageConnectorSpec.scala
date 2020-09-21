package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.Duration
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

class MessageConnectorSpec extends AnyWordSpec with Matchers with WiremockSuite with ScalaFutures with MockitoSugar {
  "post" should {
    "return ACCEPTED when post is successful" in {
      val connector = app.injector.instanceOf[MessageConnector]

      server.stubFor(
        post(
          urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
        ).willReturn(aResponse().withStatus(ACCEPTED))
      )

      implicit val hc = HeaderCarrier()
      implicit val requestHeader = FakeRequest()

      val result = connector.post("<document></document>")

      whenReady(result, Timeout(Duration.Inf)) { r =>
        r.status mustEqual ACCEPTED
      }
    }

      "return INTERNAL_SERVER_ERROR" in {
        val connector = app.injector.instanceOf[MessageConnector]

        server.stubFor(
          post(
            urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
          ).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )

        implicit val hc = HeaderCarrier()
        implicit val requestHeader = FakeRequest()

        val result = connector.post("<document></document>")

        whenReady(result, Timeout(Duration.Inf)) { r =>
          r.status mustEqual INTERNAL_SERVER_ERROR
        }
      }

    "return BAD_REQUEST when post returns BAD_REQUEST" in {
      val connector = app.injector.instanceOf[MessageConnector]

      server.stubFor(
        post(
          urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
        ).willReturn(aResponse().withStatus(BAD_REQUEST))
      )

      implicit val hc = HeaderCarrier()
      implicit val requestHeader = FakeRequest()

      val result = connector.post("<document></document>")

      whenReady(result, Timeout(Duration.Inf)) { r =>
        r.status mustEqual BAD_REQUEST
      }
    }

    "return INTERNAL_SERVER_ERROR when post returns UNAUTHORIZED" in {
      val connector = app.injector.instanceOf[MessageConnector]

      server.stubFor(
        post(
          urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
        ).willReturn(aResponse().withStatus(UNAUTHORIZED))
      )

      implicit val hc = HeaderCarrier()
      implicit val requestHeader = FakeRequest()

      val result = connector.post("<document></document>")

      whenReady(result, Timeout(Duration.Inf)) { r =>
        r.status mustEqual INTERNAL_SERVER_ERROR
      }
    }

    "return INTERNAL_SERVER_ERROR when post returns BAD_GATEWAY" in {
      val connector = app.injector.instanceOf[MessageConnector]

      server.stubFor(
        post(
          urlEqualTo("/transits-movements-trader-at-departure-stub/movements/departures")
        ).willReturn(aResponse().withStatus(BAD_GATEWAY))
      )

      implicit val hc = HeaderCarrier()
      implicit val requestHeader = FakeRequest()

      val result = connector.post("<document></document>")

      whenReady(result, Timeout(Duration.Inf)) { r =>
        r.status mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }

  override protected def portConfigKey: String = "microservice.services.eis.port"
}
