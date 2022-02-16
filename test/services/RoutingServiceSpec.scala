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

package services

import config.AppConfig
import connectors.MessageConnector
import models.ChannelType.Api
import models.MessageType.{arrivalValues, departureValues}
import models.ParseError._
import models.RoutingOption.{Gb, Xi}
import models.{ChannelType, FailureMessage, MessageType, RoutingOption}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import scala.xml.Elem

class RoutingServiceSpec
    extends AnyFreeSpec
    with Matchers
    with GuiceOneAppPerSuite
    with OptionValues
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaCheckDrivenPropertyChecks {

  private def service(
    fsrc: RouteChecker = mock[RouteChecker],
    messageConnector: MessageConnector = mock[MessageConnector],
    appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  ) = new RoutingService(fsrc, messageConnector, appConfig)

  implicit val hc = HeaderCarrier()

  val channelGen = Gen.oneOf(ChannelType.values)
  val routeGen   = Gen.oneOf(RoutingOption.values)
  val notNCTSMessageTypeGen = Gen.oneOf(
    MessageType.values.filterNot(msg => (arrivalValues ++ departureValues) contains msg)
  )
  val nctsMessageTypeArrGen = Gen.oneOf(
    MessageType.values.filter(msg => arrivalValues contains msg)
  )
  val nctsMessageTypeDepGen = Gen.oneOf(
    MessageType.values.filter(msg => departureValues contains msg)
  )

  "submitMessage" - {

    "returns InvalidMessageCode if message has incorrect message rootNode" in {
      val badXML = <TransitWrapper>
        <ABCDE></ABCDE>
      </TransitWrapper>

      val result = service().submitMessage(badXML, Api, hc)

      result mustBe a[Left[InvalidMessageCode, _]]
    }

    "returns DepartureEmpty if departure message with no office of departure" in {
      val input = <TransitWrapper>
        <CC928A></CC928A>
      </TransitWrapper>

      val result = service().submitMessage(input, Api, hc)

      result mustBe a[Left[DepartureEmpty, _]]
    }

    "returns DestinationEmpty if destination message with no office of presentation" in {
      val input = <TransitWrapper>
        <CC008A></CC008A>
      </TransitWrapper>

      val result = service().submitMessage(input, Api, hc)

      result mustBe a[Left[PresentationEmpty, _]]
    }

    "returns GuaranteeReferenceEmpty if guarantee message with no guarantee reference" in {
      val input = <TransitWrapper>
        <CD034A></CD034A>
      </TransitWrapper>

      val result = service().submitMessage(input, Api, hc)

      result mustBe a[Left[GuaranteeReferenceEmpty, _]]
    }

    "departure message starting with XI sends XI flag when feature switch is true" in {
      val input = <TransitWrapper>
        <CC015B>
          <CUSOFFDEPEPT>
            <RefNumEPT1>XI12345</RefNumEPT1>
          </CUSOFFDEPEPT>
        </CC015B>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Xi), eqTo(channel))).thenReturn(true)

        service(fsrc, mc).submitMessage(input, channel, hc)

        verify(mc).post(any(), eqTo(Xi), eqTo(hc))
      }
    }

    "departure message starting with XI is rejected feature switch is false" in {
      val input = <TransitWrapper>
        <CC015B>
          <CUSOFFDEPEPT>
            <RefNumEPT1>XI12345</RefNumEPT1>
          </CUSOFFDEPEPT>
        </CC015B>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Xi), eqTo(channel))).thenReturn(false)

        val result = service(fsrc, mc).submitMessage(input, channel, hc)

        result mustBe a[Left[FailureMessage, _]]
      }
    }

    "departure message starting with GB sends Gb flag when feature switch is true" in {
      val input = <TransitWrapper>
        <CC015B>
          <CUSOFFDEPEPT>
            <RefNumEPT1>GB12345</RefNumEPT1>
          </CUSOFFDEPEPT>
        </CC015B>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Gb), eqTo(channel))).thenReturn(true)

        service(fsrc, mc).submitMessage(input, channel, hc)

        verify(mc).post(any(), eqTo(Gb), eqTo(hc))
        verify(mc).postNCTSMonitoring(any(), any(), any(), any())
      }
    }

    "departure message starting with GB is rejected feature switch is false" in {
      val input = <TransitWrapper>
        <CC015B>
          <CUSOFFDEPEPT>
            <RefNumEPT1>GB12345</RefNumEPT1>
          </CUSOFFDEPEPT>
        </CC015B>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Gb), eqTo(channel))).thenReturn(false)

        val result = service(fsrc, mc).submitMessage(input, channel, hc)

        result mustBe a[Left[FailureMessage, _]]
      }
    }

    "never posts a movement to NCTS monitoring if message type is not a Departure or Arrival message type" in {

      def nonDepartureXml(messageCode: String): Elem = {
        scala.xml.XML.loadString(s"""
           |<TransitWrapper>
           |   <$messageCode>
           |      <GUAREF2>
           |         <GuaRefNumGRNREF21>XI12345</GuaRefNumGRNREF21>
           |      </GUAREF2>
           |   </$messageCode>
           |</TransitWrapper>""".stripMargin)
      }

      forAll(notNCTSMessageTypeGen, channelGen) { (messageType, channelType) =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(any(), any())).thenReturn(true)

        service(fsrc, mc).submitMessage(nonDepartureXml(messageType.rootNode), channelType, hc)
        verify(mc).post(any(), any(), any())
        verify(mc, never()).postNCTSMonitoring(any(), any(), any(), any())
      }
    }

    "never posts a movement to NCTS monitoring if the ncts-monitoring feature switch is disabled" in {

      val input = <TransitWrapper>
        <CC015B>
          <CUSOFFDEPEPT>
            <RefNumEPT1>GB12345</RefNumEPT1>
          </CUSOFFDEPEPT>
        </CC015B>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Gb), eqTo(channel))).thenReturn(true)

        val mockAppConfig: AppConfig = mock[AppConfig]

        when(mockAppConfig.nctsMonitoringEnabled).thenReturn(false)

        service(fsrc, mc, mockAppConfig).submitMessage(input, channel, hc)

        verify(mc).post(any(), eqTo(Gb), eqTo(hc))
        verify(mc, never()).postNCTSMonitoring(any(), any(), any(), any())
      }
    }

    "posts arrivals to NCTS monitoring if ncts-monitoring feature is enabled" in {

      def messageXML(rootNode: String): Elem = {
        scala.xml.XML.loadString(s"""
             |<TransitWrapper>
             |   <$rootNode>
             |      <CUSOFFPREOFFRES>
             |         <RefNumRES1>XI12345</RefNumRES1>
             |      </CUSOFFPREOFFRES>
             |   </$rootNode>
             |</TransitWrapper>""".stripMargin)
      }

      forAll(nctsMessageTypeArrGen, channelGen) { (msgType, channel) =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))
        when(mc.postNCTSMonitoring(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(any(), any())).thenReturn(true)

        service(fsrc, mc).submitMessage(messageXML(msgType.rootNode), channel, hc)

        verify(mc).postNCTSMonitoring(any(), any(), any(), any())
        verify(mc).post(any(), any(), any())
      }
    }

    "posts departures to NCTS monitoring if ncts-monitoring feature is enabled" in {

      def messageXML(rootNode: String): Elem = {
        scala.xml.XML.loadString(s"""
             |<TransitWrapper>
             |   <$rootNode>
             |      <CUSOFFDEPEPT>
             |         <RefNumEPT1>XI12345</RefNumEPT1>
             |      </CUSOFFDEPEPT>
             |   </$rootNode>
             |</TransitWrapper>""".stripMargin)
      }

      forAll(nctsMessageTypeDepGen, channelGen) { (msgType, channel) =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))
        when(mc.postNCTSMonitoring(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(any(), any())).thenReturn(true)

        service(fsrc, mc).submitMessage(messageXML(msgType.rootNode), channel, hc)

        verify(mc).postNCTSMonitoring(any(), any(), any(), any())
        verify(mc).post(any(), any(), any())
      }
    }

    "destination message starting with XI sends XI flag when feature switch is true" in {
      val input = <TransitWrapper>
        <CC007A>
          <CUSOFFPREOFFRES>
            <RefNumRES1>XI12345</RefNumRES1>
          </CUSOFFPREOFFRES>
        </CC007A>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Xi), eqTo(channel))).thenReturn(true)

        service(fsrc, mc).submitMessage(input, channel, hc)

        verify(mc).postNCTSMonitoring(any(), any(), any(), any())
        verify(mc).post(any(), eqTo(Xi), eqTo(hc))
      }
    }

    "destination message starting with XI is rejected feature switch is false" in {
      val input = <TransitWrapper>
        <CC007A>
          <CUSOFFPREOFFRES>
            <RefNumRES1>XI12345</RefNumRES1>
          </CUSOFFPREOFFRES>
        </CC007A>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Xi), eqTo(channel))).thenReturn(false)

        val result = service(fsrc, mc).submitMessage(input, channel, hc)

        result mustBe a[Left[FailureMessage, _]]
      }
    }

    "destination message starting with GB sends Gb flag when feature switch is true" in {
      val input = <TransitWrapper>
        <CC007A>
          <CUSOFFPREOFFRES>
            <RefNumRES1>GB12345</RefNumRES1>
          </CUSOFFPREOFFRES>
        </CC007A>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Gb), eqTo(channel))).thenReturn(true)

        service(fsrc, mc).submitMessage(input, channel, hc)

        verify(mc).postNCTSMonitoring(any(), any(), any(), any())
        verify(mc).post(any(), eqTo(Gb), eqTo(hc))
      }
    }

    "destination message starting with GB is rejected feature switch is false" in {
      val input = <TransitWrapper>
        <CC007A>
          <CUSOFFPREOFFRES>
            <RefNumRES1>GB12345</RefNumRES1>
          </CUSOFFPREOFFRES>
        </CC007A>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Gb), eqTo(channel))).thenReturn(false)

        val result = service(fsrc, mc).submitMessage(input, channel, hc)

        result mustBe a[Left[FailureMessage, _]]
      }
    }

    "guarantee message starting with XI sends XI flag when feature switch is true" in {
      val input = <TransitWrapper>
        <CD034A>
          <GUAREF2>
            <GuaRefNumGRNREF21>20XI0000010000GX1</GuaRefNumGRNREF21>
          </GUAREF2>
        </CD034A>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Xi), eqTo(channel))).thenReturn(true)

        service(fsrc, mc).submitMessage(input, channel, hc)

        verify(mc, never()).postNCTSMonitoring(any(), any(), any(), any())
        verify(mc).post(any(), eqTo(Xi), eqTo(hc))
      }
    }

    "guarantee message starting with XI is rejected feature switch is false" in {
      val input = <TransitWrapper>
        <CD034A>
          <GUAREF2>
            <GuaRefNumGRNREF21>20XI0000010000GX1</GuaRefNumGRNREF21>
          </GUAREF2>
        </CD034A>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Xi), eqTo(channel))).thenReturn(false)

        val result = service(fsrc, mc).submitMessage(input, channel, hc)

        result mustBe a[Left[FailureMessage, _]]
      }
    }

    "guarantee message starting with GB sends Gb flag when feature switch is true" in {
      val input = <TransitWrapper>
        <CD034A>
          <GUAREF2>
            <GuaRefNumGRNREF21>21GB3300BE0001067A001017</GuaRefNumGRNREF21>
          </GUAREF2>
        </CD034A>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Gb), eqTo(channel))).thenReturn(true)

        service(fsrc, mc).submitMessage(input, channel, hc)

        verify(mc, never()).postNCTSMonitoring(any(), any(), any(), any())
        verify(mc).post(any(), eqTo(Gb), eqTo(hc))
      }
    }

    "guarantee message starting with GB is rejected feature switch is false" in {
      val input = <TransitWrapper>
        <CD034A>
          <GUAREF2>
            <GuaRefNumGRNREF21>21GB3300BE0001067A001017</GuaRefNumGRNREF21>
          </GUAREF2>
        </CD034A>
      </TransitWrapper>

      forAll(channelGen) { channel =>
        val mc = mock[MessageConnector]
        when(mc.post(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

        val fsrc = mock[RouteChecker]
        when(fsrc.canForward(eqTo(Gb), eqTo(channel))).thenReturn(false)

        val result = service(fsrc, mc).submitMessage(input, channel, hc)

        result mustBe a[Left[FailureMessage, _]]
      }
    }
  }
}
