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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest

class OutgoingHeadersFilterSpec extends AnyFreeSpec with Matchers with GuiceOneAppPerSuite with OptionValues with ScalaFutures with MockitoSugar with BeforeAndAfterEach {
  "OutgoingHeadersFilter" - {
    "headersFromRequest must retain only custom upstream headers" in {
      val requestHeader = FakeRequest().withHeaders(
        "X-Forwarded-Host" -> "mdtp",
        "X-Correlation-ID" -> "137302f5-71ae-40a4-bd92-cac2ae7sde2f",
        "Date" -> "Tue, 29 Sep 2020 11:46:50 +0100",
        "Content-Type" -> "application/xml",
        "Accept" -> "application/xml",
        "X-Message-Type" -> "IE015",
        "X-Message-Sender" -> "MDTP-000000000000000000000000011-01",
        "Authorization" -> "Bearer 123",
        "Connection" -> "Keep-Alive",
        "Keep-Alive" -> "timeout=5, max=1000",
        "Age" -> "0"
      )

      val result: Seq[(String, String)] = OutgoingHeadersFilter.headersFromRequest(requestHeader)

      result.size mustBe 5

      result must contain("Date" -> "Tue, 29 Sep 2020 11:46:50 +0100")
      result must contain("Content-Type" -> "application/xml")
      result must contain("Accept" -> "application/xml")
      result must contain("X-Message-Type" -> "IE015")
      result must contain("X-Message-Sender" -> "MDTP-000000000000000000000000011-01")
    }
  }
}
