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

package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.Materializer
import controllers.actions.ChannelAction
import models.ChannelType.Api
import models.ParseError.InvalidMessageCode
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers._
import services.RoutingService
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

class MessagesControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ScalaCheckDrivenPropertyChecks {

  val requestXmlBody = <CC007A>
    <SynIdeMES1>UNOC</SynIdeMES1>
    <SynVerNumMES2>3</SynVerNumMES2>
    <MesRecMES6>NCTS</MesRecMES6>
    <DatOfPreMES9>20200204</DatOfPreMES9>
    <TimOfPreMES10>1302</TimOfPreMES10>
    <IntConRefMES11>WE202002046</IntConRefMES11>
    <AppRefMES14>NCTS</AppRefMES14>
    <TesIndMES18>0</TesIndMES18>
    <MesIdeMES19>1</MesIdeMES19>
    <MesTypMES20>GB007A</MesTypMES20>
    <HEAHEA>
      <DocNumHEA5>99IT9876AB88901209</DocNumHEA5>
      <CusSubPlaHEA66>EXAMPLE1</CusSubPlaHEA66>
      <ArrNotPlaHEA60>NW16XE</ArrNotPlaHEA60>
      <ArrNotPlaHEA60LNG>EN</ArrNotPlaHEA60LNG>
      <ArrAgrLocOfGooHEA63LNG>EN</ArrAgrLocOfGooHEA63LNG>
      <SimProFlaHEA132>0</SimProFlaHEA132>
      <ArrNotDatHEA141>20200204</ArrNotDatHEA141>
    </HEAHEA>
    <TRADESTRD>
      <NamTRD7>EXAMPLE2</NamTRD7>
      <StrAndNumTRD22>Baker Street</StrAndNumTRD22>
      <PosCodTRD23>NW16XE</PosCodTRD23>
      <CitTRD24>London</CitTRD24>
      <CouTRD25>GB</CouTRD25>
      <NADLNGRD>EN</NADLNGRD>
      <TINTRD59>EXAMPLE3</TINTRD59>
    </TRADESTRD>
    <CUSOFFPREOFFRES>
      <RefNumRES1>GB000128</RefNumRES1>
    </CUSOFFPREOFFRES>
  </CC007A>

  private val fakeValidXmlRequest = FakeRequest(
    method = "POST",
    uri = routes.MessagesController.post().url,
    headers = FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.XML, "channel" -> Api.name)),
    body = requestXmlBody
  )

  private val fakeEmptyRequest =
    FakeRequest(method = "POST", uri = routes.MessagesController.post().url, headers = FakeHeaders(Seq("channel" -> Api.name)), body = AnyContentAsXml)

  private val fakeJsonRequest = FakeRequest(
    method = "POST",
    uri = routes.MessagesController.post().url,
    headers = FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON, "channel" -> Api.name)),
    body = Json.parse(""" {"key": "value"} """)
  )

  val passThroughStatusCodes = Gen.oneOf(Seq(
    SERVICE_UNAVAILABLE,
    REQUEST_ENTITY_TOO_LARGE,
    UNAUTHORIZED,
    TOO_MANY_REQUESTS,
    NOT_FOUND
  ))

  implicit val system: ActorSystem = ActorSystem("MessagesControllerSpec")

  implicit def mat: Materializer = ActorMaterializer()

  private def controller(routingService: RoutingService = mock[RoutingService]) =
    new MessagesController(Helpers.stubControllerComponents(), app.injector.instanceOf[ChannelAction], routingService)

  "POST any XML" should {
    "should return 202 Accepted when routing service successful" in {
      val rs = mock[RoutingService]
      when(rs.submitMessage(any(), any())(any())).thenReturn(Right(Future.successful(HttpResponse(ACCEPTED, ""))))

      val result = controller(rs).post()(fakeValidXmlRequest)
      status(result) shouldBe ACCEPTED
    }
    "should return 502 BAD GATEWAY Error when routing service receives 500" in {
      val rs = mock[RoutingService]
      when(rs.submitMessage(any(), any())(any())).thenReturn(Right(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, ""))))

      val result = controller(rs).post()(fakeValidXmlRequest)
      status(result) shouldBe BAD_GATEWAY
    }
    "should return 502 BAD GATEWAY Error when routing service receives 504" in {
      val rs = mock[RoutingService]
      when(rs.submitMessage(any(), any())(any())).thenReturn(Right(Future.successful(HttpResponse(GATEWAY_TIMEOUT, ""))))

      val result = controller(rs).post()(fakeValidXmlRequest)
      status(result) shouldBe BAD_GATEWAY
    }
    "should return 422 UNPROCESSABLE ENTITY Error when routing service receives 403" in {
      val rs = mock[RoutingService]
      when(rs.submitMessage(any(), any())(any())).thenReturn(Right(Future.successful(HttpResponse(FORBIDDEN, ""))))

      val result = controller(rs).post()(fakeValidXmlRequest)
      status(result) shouldBe UNPROCESSABLE_ENTITY
    }
    "should pass through other status codes" in forAll(passThroughStatusCodes) { statusCode =>
      val rs = mock[RoutingService]
      when(rs.submitMessage(any(), any())(any())).thenReturn(Right(Future.successful(HttpResponse(statusCode, ""))))

      val result = controller(rs).post()(fakeValidXmlRequest)
      status(result) shouldBe statusCode
    }
    "should return 400 Bad Request when parse error returned" in {
      val rs = mock[RoutingService]
      when(rs.submitMessage(any(), any())(any())).thenReturn(Left(InvalidMessageCode("test message")))

      val result = controller(rs).post()(fakeValidXmlRequest)
      status(result) shouldBe BAD_REQUEST
    }
  }

  "POST any JSON" should {
    "should return 415 UnsupportedMediaType" in {
      val result = controller().post()(fakeJsonRequest)

      status(result) mustEqual UNSUPPORTED_MEDIA_TYPE
    }
  }

  "POST empty request" should {
    "should return 415 UnsupportedMediaType" in {
      val result = controller().post()(fakeEmptyRequest)

      status(result) mustEqual UNSUPPORTED_MEDIA_TYPE
    }
  }
}
