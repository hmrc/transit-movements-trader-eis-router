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

package services

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.mockito.Mockito.verify
import config.AppConfig
import connectors.MessageConnector
import controllers.routes
import models.{ChannelType, FailureMessage, RoutingOption}
import models.ChannelType.{api, web}
import models.ParseError.{DepartureEmpty, InvalidMessageCode, PresentationEmpty}
import models.requests.ChannelRequest
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{HeaderNames, MimeTypes}
import play.api.{Configuration, Environment}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import scala.concurrent.Future
import scala.xml.NodeSeq

class RoutingServiceSpec extends AnyFreeSpec with Matchers with GuiceOneAppPerSuite with OptionValues with ScalaFutures with MockitoSugar with BeforeAndAfterEach {

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = makeAppConfig()

  private def service(messageConnector: MessageConnector = mock[MessageConnector], aConfig: AppConfig = appConfig) = new RoutingService(aConfig, messageConnector)

  private def fakeRequest(channel: ChannelType): ChannelRequest[NodeSeq] =
    ChannelRequest(
      FakeRequest(
        method = "POST",
        uri = routes.MessagesController.post().url,
        headers = FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.XML)),
        body = NodeSeq.Empty), channel)

  implicit val hc = HeaderCarrier()

  private class FakeableRoutesAppConfig(
     config: Configuration,
     sConfig: ServicesConfig,
     apiGb: String,
     apiNi: String,
     webGb: String,
     webNi: String,
     ) extends AppConfig(config, sConfig) {
    override val apiGbRouting: RoutingOption = RoutingOption.parseRoutingOption(apiGb)
    override val apiXiRouting: RoutingOption = RoutingOption.parseRoutingOption(apiNi)
    override val webGbRouting: RoutingOption = RoutingOption.parseRoutingOption(webGb)
    override val webXiRouting: RoutingOption = RoutingOption.parseRoutingOption(webNi)
  }

  private def makeAppConfig(apiGb: String = "Gb",
                            apiNi: String = "Xi",
                            webGb: String = "Gb",
                            webNi: String = "Xi") = new FakeableRoutesAppConfig(configuration, serviceConfig, apiGb, apiNi, webGb, webNi)

  "RoutingService" - {

    "Standard Config" - {
      "returns InvalidMessageCode if message has incorrect message rootNode" in {
        val badXML = <TransitWrapper><ABCDE></ABCDE></TransitWrapper>
        implicit val request = fakeRequest(api)

        val result = service().submitMessage(badXML)

        result mustBe a[Left[InvalidMessageCode, _]]
      }

      "returns DepartureEmpty if departure message with no office of departure" in {
        val input = <TransitWrapper><CC928A></CC928A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val result = service().submitMessage(input)

        result mustBe a[Left[DepartureEmpty, _]]
      }

      "returns DestinationEmpty if destination message with no office of presentation" in {
        val input = <TransitWrapper><CC008A></CC008A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val result = service().submitMessage(input)

        result mustBe a[Left[PresentationEmpty, _]]
      }

      "departure message forwarded to NI if departure office starts with XI" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>XI12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())
      }

      "destination message forwarded to NI if presentation office starts with XI" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>XI12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())

      }

      "departure message forwarded to GB if departure office starts with GB" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>GB12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

      }

      "destination message forwarded to GB if presentation office starts with GB" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>GB12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

      }

      "departure message forwarded to NI if departure office starts with XI (\\n formatted xml)" in {
        val input =
          <TransitWrapper>
            <CC015B><CUSOFFDEPEPT><RefNumEPT1>XI12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())
      }

      "destination message forwarded to NI if presentation office starts with XI (\\n formatted xml)" in {
        val input =
          <TransitWrapper>
            <CC007A><CUSOFFPREOFFRES><RefNumRES1>XI12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())

      }

      "departure message forwarded to GB if departure office starts with GB (\\n formatted xml)" in {
        val input =
          <TransitWrapper>
            <CC015B><CUSOFFDEPEPT><RefNumEPT1>GB12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

      }

      "destination message forwarded to GB if presentation office starts with GB (\\n formatted xml)" in {
        val input =
          <TransitWrapper>
            <CC007A><CUSOFFPREOFFRES><RefNumRES1>GB12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

      }

      "departure message forwarded to GB if departure office starts with other value" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>AB12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

      }

      "destination message forwarded to GB if presentation office starts with other value" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>AB12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val result = service(mc).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

      }
    }

    "Overriden Config" - {
      "api departure for NI rejected when config says Rejected" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>XI12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(apiNi = "Rejected")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Left[FailureMessage, _]]
      }

      "api departure for NI routed to Gb when config says Gb" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>XI12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(apiNi = "Gb")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())
      }

      "api departure for GB rejected when config says Rejected" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>GB12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(apiGb = "Rejected")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Left[FailureMessage, _]]

      }
      "api departure for GB routed to Xi when config says Xi" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>GB12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(apiGb = "Xi")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())

      }
      "api destination for NI rejected when config says Rejected" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>XI12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(apiNi = "Rejected")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Left[FailureMessage, _]]

      }
      "api destination for NI routed to Gb when config says Gb" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>XI12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(apiNi = "Gb")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

      }
      "api destination for GB rejected when config says Rejected" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>GB12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(apiGb = "Rejected")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Left[FailureMessage, _]]
      }
      "api destination for GB routed to Xi when config says Xi" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>GB12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(api)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(apiGb = "Xi")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())

      }
      "web departure for NI rejected when config says Rejected" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>XI12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(web)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(webNi = "Rejected")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Left[FailureMessage, _]]
      }

      "web departure for NI routed to Gb when config says Gb" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>XI12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(web)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(webNi = "Gb")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())
      }

      "web departure for GB rejected when config says Rejected" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>GB12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(web)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(webGb = "Rejected")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Left[FailureMessage, _]]

      }
      "web departure for GB routed to Xi when config says Xi" in {
        val input =
          <TransitWrapper><CC015B><CUSOFFDEPEPT><RefNumEPT1>GB12345</RefNumEPT1></CUSOFFDEPEPT></CC015B></TransitWrapper>
        implicit val request = fakeRequest(web)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(webGb = "Xi")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())

      }
      "web destination for NI rejected when config says Rejected" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>XI12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(web)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(webNi = "Rejected")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Left[FailureMessage, _]]

      }
      "web destination for NI routed to Gb when config says Gb" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>XI12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(web)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(webNi = "Gb")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

      }
      "web destination for GB rejected when config says Rejected" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>GB12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(web)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(webGb = "Rejected")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Left[FailureMessage, _]]
      }
      "web destination for GB routed to Xi when config says Xi" in {
        val input =
          <TransitWrapper><CC007A><CUSOFFPREOFFRES><RefNumRES1>GB12345</RefNumRES1></CUSOFFPREOFFRES></CC007A></TransitWrapper>
        implicit val request = fakeRequest(web)

        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val config = makeAppConfig(webGb = "Xi")

        val result = service(mc, config).submitMessage(input)

        result mustBe a[Right[_, Future[HttpResponse]]]

        verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())

      }
    }
  }
}
