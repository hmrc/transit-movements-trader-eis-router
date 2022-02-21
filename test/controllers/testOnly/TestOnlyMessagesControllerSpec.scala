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

package controllers.testOnly

import controllers.MessagesController
import controllers.actions.ChannelAction
import models.ChannelType.Api
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.{HeaderNames, MimeTypes}
import play.api.test.Helpers.{ACCEPTED, GATEWAY_TIMEOUT, defaultAwaitTimeout, status}
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import services.RoutingService
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future
import scala.xml.Elem

class TestOnlyMessagesControllerSpec
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with MockitoSugar
    with ScalaCheckPropertyChecks {

  private def fakeXmlRequest(xml: Elem): FakeRequest[Elem] = FakeRequest(
    method = "POST",
    uri = routes.TestOnlyMessagesController.post.url,
    headers = FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.XML, "channel" -> Api.name)),
    body = xml
  )

  private def controller(mockRoutingService: RoutingService) = {
    val cc = Helpers.stubControllerComponents()
    val channelAction = app.injector.instanceOf[ChannelAction]
    new TestOnlyMessagesController(
      cc,
      channelAction,
      new MessagesController(
        cc,
        channelAction,
        mockRoutingService
      )
    )
  }

  "post" when {

    "posting XML to trigger timeout" should {
      "return gateway timeout" in {
        val mockRoutingService = mock[RoutingService]

        val xml =
          <CC007A>
            <MesSenMES3>SYST17B-NCTS_TIMEOUT</MesSenMES3>
          </CC007A>

        val result = controller(mockRoutingService).post()(fakeXmlRequest(xml))
        status(result) shouldBe GATEWAY_TIMEOUT

        verify(mockRoutingService, never()).submitMessage(any(), any(), any())
      }
    }

    "posting any other XML" should {
      "interact with routing service" in {
        forAll(arbitrary[String]) {
          str =>
            val mockRoutingService = mock[RoutingService]

            val xml =
              <CC007A>
                <MesSenMES3>{str}</MesSenMES3>
              </CC007A>

            when(mockRoutingService.submitMessage(any(), any(), any()))
              .thenReturn(Right(Future.successful(HttpResponse(ACCEPTED, ""))))

            val result = controller(mockRoutingService).post()(fakeXmlRequest(xml))
            status(result) shouldBe ACCEPTED

            verify(mockRoutingService).submitMessage(any(), any(), any())
        }
      }
    }
  }
}
