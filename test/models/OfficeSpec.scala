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

package models

import models.RoutingOption.Gb
import models.RoutingOption.Xi
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class OfficeSpec extends AnyFreeSpec with Matchers {

  "Offices" - {
    "getRoutingOption" - {
      "must return Gb when the office starts with GB" in {
        DepartureOffice("GB123").getRoutingOption mustBe Gb
      }

      "must return Xi when the office starts with XI" in {
        DepartureOffice("XI123").getRoutingOption mustBe Xi
      }

      "must return Gb when the office starts with anything else" in {
        DepartureOffice("AB123").getRoutingOption mustBe Gb
      }
    }
  }
}
