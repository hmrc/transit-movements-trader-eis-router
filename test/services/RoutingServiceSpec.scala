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
import models.ChannelType.{Api, Web}
import models.ParseError.{DepartureEmpty, InvalidMessageCode, PresentationEmpty}
import models.RoutingOption.{Gb, Reject, Xi}
import models.requests.ChannelRequest
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.{HeaderNames, MimeTypes}
import play.api.{Configuration, Environment}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future
import scala.xml.NodeSeq

class RoutingServiceSpec extends AnyFreeSpec with Matchers with GuiceOneAppPerSuite with OptionValues with ScalaFutures with MockitoSugar with BeforeAndAfterEach with ScalaCheckDrivenPropertyChecks {

  private def service(fsrc: FeatureSwitchRouteConverter = mock[FeatureSwitchRouteConverter], messageConnector: MessageConnector = mock[MessageConnector]) = new RoutingService(fsrc, messageConnector)

  private def fakeRequest(channel: ChannelType): ChannelRequest[NodeSeq] =
    ChannelRequest(
      FakeRequest(
        method = "POST",
        uri = routes.MessagesController.post().url,
        headers = FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.XML)),
        body = NodeSeq.Empty), channel)

  implicit val hc = HeaderCarrier()

  val channelGen = Gen.oneOf(ChannelType.values)
  val routeGen = Gen.oneOf(RoutingOption.values)

  "submitMessage" - {

      "returns InvalidMessageCode if message has incorrect message rootNode" in {
        val badXML = <TransitWrapper>
          <ABCDE></ABCDE>
        </TransitWrapper>
        implicit val request = fakeRequest(Api)

        val result = service().submitMessage(badXML)

        result mustBe a[Left[InvalidMessageCode, _]]
      }

      "returns DepartureEmpty if departure message with no office of departure" in {
        val input = <TransitWrapper>
          <CC928A></CC928A>
        </TransitWrapper>
        implicit val request = fakeRequest(Api)

        val result = service().submitMessage(input)

        result mustBe a[Left[DepartureEmpty, _]]
      }

      "returns DestinationEmpty if destination message with no office of presentation" in {
        val input = <TransitWrapper>
          <CC008A></CC008A>
        </TransitWrapper>
        implicit val request = fakeRequest(Api)

        val result = service().submitMessage(input)

        result mustBe a[Left[PresentationEmpty, _]]
      }

      "departure message starting with XI forwarded according to fsrc" in {
        val input = <TransitWrapper>
          <CC015B>
            <CUSOFFDEPEPT>
              <RefNumEPT1>XI12345</RefNumEPT1>
            </CUSOFFDEPEPT>
          </CC015B>
        </TransitWrapper>

        forAll(channelGen, routeGen) {
          (channel, route) =>
            implicit val request = fakeRequest(channel)

            val mc = mock[MessageConnector]
            when(mc.post(any(), any())(any(), any())).thenReturn(Right(Future.successful(HttpResponse(200, ""))))

            val fsrc = mock[FeatureSwitchRouteConverter]
            when(fsrc.convert(eqTo(Some(Xi)), eqTo(channel))).thenReturn(route)

            service(fsrc, mc).submitMessage(input)

            verify(mc).post(any(), eqTo(route))(any(), any())
        }
      }

      "departure message starting with GB forwarded according to fsrc" in {
        val input = <TransitWrapper>
          <CC015B>
            <CUSOFFDEPEPT>
              <RefNumEPT1>GB12345</RefNumEPT1>
            </CUSOFFDEPEPT>
          </CC015B>
        </TransitWrapper>


        forAll(channelGen, routeGen) {
          (channel, route) =>
            implicit val request = fakeRequest(channel)

            val mc = mock[MessageConnector]
            when(mc.post(any(), any())(any(), any())).thenReturn(Right(Future.successful(HttpResponse(200, ""))))

            val fsrc = mock[FeatureSwitchRouteConverter]
            when(fsrc.convert(eqTo(Some(Gb)), eqTo(channel))).thenReturn(route)

            service(fsrc, mc).submitMessage(input)

            verify(mc).post(any(), eqTo(route))(any(), any())
        }
      }

      "destination message starting with XI forwarded according to fsrc" in {
        val input = <TransitWrapper>
          <CC007A>
            <CUSOFFPREOFFRES>
              <RefNumRES1>XI12345</RefNumRES1>
            </CUSOFFPREOFFRES>
          </CC007A>
        </TransitWrapper>

        forAll(channelGen, routeGen) {
          (channel, route) =>
            implicit val request = fakeRequest(channel)

            val mc = mock[MessageConnector]
            when(mc.post(any(), any())(any(), any())).thenReturn(Right(Future.successful(HttpResponse(200, ""))))

            val fsrc = mock[FeatureSwitchRouteConverter]
            when(fsrc.convert(eqTo(Some(Xi)), eqTo(channel))).thenReturn(route)

            service(fsrc, mc).submitMessage(input)

            verify(mc).post(any(), eqTo(route))(any(), any())
        }
      }

      "destination message starting with GB forwarded according to fsrc" in {
        val input = <TransitWrapper>
          <CC007A>
            <CUSOFFPREOFFRES>
              <RefNumRES1>GB12345</RefNumRES1>
            </CUSOFFPREOFFRES>
          </CC007A>
        </TransitWrapper>

        forAll(channelGen, routeGen) {
          (channel, route) =>
            implicit val request = fakeRequest(channel)

            val mc = mock[MessageConnector]
            when(mc.post(any(), any())(any(), any())).thenReturn(Right(Future.successful(HttpResponse(200, ""))))

            val fsrc = mock[FeatureSwitchRouteConverter]
            when(fsrc.convert(eqTo(Some(Gb)), eqTo(channel))).thenReturn(route)

            service(fsrc, mc).submitMessage(input)

            verify(mc).post(any(), eqTo(route))(any(), any())
        }
      }
    }
}
