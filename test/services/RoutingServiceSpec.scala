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
import models.ParseError.{DepartureEmpty, DestinationEmpty, InvalidMessageCode}
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class RoutingServiceSpec  extends AnyFreeSpec with Matchers with GuiceOneAppPerSuite with OptionValues with ScalaFutures with MockitoSugar with BeforeAndAfterEach {

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  private def service(messageConnector: MessageConnector = mock[MessageConnector]) = new RoutingService(appConfig, messageConnector)

  implicit val hc = HeaderCarrier()
  implicit val requestHeader = FakeRequest()


  "RoutingService" - {
    "returns InvalidMessageCode if message has unknown rootNode" in {
      val badXML = <ABCDE></ABCDE>

      val result = service().submitMessage(badXML)

      result mustBe a[Left[InvalidMessageCode, _]]
    }

    "returns DepartureEmpty if departure message with no office of departure" in {
      val input = <CC928A></CC928A>

      val result = service().submitMessage(input)

      result mustBe a[Left[DepartureEmpty, _]]
    }

    "returns DestinationEmpty if destination message with no office of destination" in {
      val input = <CC008A></CC008A>

      val result = service().submitMessage(input)

      result mustBe a[Left[DestinationEmpty, _]]
    }

    "departure message forwarded to NI if departure office starts with XI" in {
      val input =
      <CC015B>
        <CUSOFFDEPEPT>
          <RefNumEPT1>XI12345</RefNumEPT1>
        </CUSOFFDEPEPT>
      </CC015B>

      val mc = mock[MessageConnector]
      when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200)))

      val result = service(mc).submitMessage(input)

      result mustBe a[Right[_, Future[HttpResponse]]]

      verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())
    }

    "destination message forwarded to NI if destination office starts with XI" in {
      val input =
        <CC007A>
          <CUSOFFDESEST>
            <RefNumEST1>XI12345</RefNumEST1>
          </CUSOFFDESEST>
        </CC007A>

      val mc = mock[MessageConnector]
      when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200)))

      val result = service(mc).submitMessage(input)

      result mustBe a[Right[_, Future[HttpResponse]]]

      verify(mc).post(any(), eqTo(appConfig.eisniUrl), eqTo(appConfig.eisniBearerToken))(any(), any())

    }

    "departure message forwarded to GB if departure office starts with GB" in {
      val input =
        <CC015B>
          <CUSOFFDEPEPT>
            <RefNumEPT1>GB12345</RefNumEPT1>
          </CUSOFFDEPEPT>
        </CC015B>

      val mc = mock[MessageConnector]
      when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200)))

      val result = service(mc).submitMessage(input)

      result mustBe a[Right[_, Future[HttpResponse]]]

      verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

    }

    "destination message forwarded to GB if destination office starts with GB" in {
      val input =
        <CC007A>
          <CUSOFFDESEST>
            <RefNumEST1>GB12345</RefNumEST1>
          </CUSOFFDESEST>
        </CC007A>

      val mc = mock[MessageConnector]
      when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200)))

      val result = service(mc).submitMessage(input)

      result mustBe a[Right[_, Future[HttpResponse]]]

      verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

    }

    "departure message forwarded to GB if departure office starts with other value" in {
      val input =
        <CC015B>
          <CUSOFFDEPEPT>
            <RefNumEPT1>AB12345</RefNumEPT1>
          </CUSOFFDEPEPT>
        </CC015B>

      val mc = mock[MessageConnector]
      when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200)))

      val result = service(mc).submitMessage(input)

      result mustBe a[Right[_, Future[HttpResponse]]]

      verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

    }

    "destination message forwarded to GB if destination office starts with other value" in {
      val input =
        <CC007A>
          <CUSOFFDESEST>
            <RefNumEST1>AB12345</RefNumEST1>
          </CUSOFFDESEST>
        </CC007A>

      val mc = mock[MessageConnector]
      when(mc.post(any(), any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(200)))

      val result = service(mc).submitMessage(input)

      result mustBe a[Right[_, Future[HttpResponse]]]

      verify(mc).post(any(), eqTo(appConfig.eisgbUrl), eqTo(appConfig.eisgbBearerToken))(any(), any())

    }
  }
}
