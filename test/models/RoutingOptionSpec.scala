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

package models

import models.RoutingOption.{Gb, Reject, Xi}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class RoutingOptionSpec extends AnyFreeSpec with Matchers {

  "parseRoutingOption" - {
    "must return Gb if input is \"Gb\"" in {
      RoutingOption.parseRoutingOption("Gb") mustBe Gb
    }

    "must return Xi if input is \"Xi\"" in {
      RoutingOption.parseRoutingOption("Xi") mustBe Xi
    }

    "must return Reject if input is \"Reject\"" in {
      RoutingOption.parseRoutingOption("Reject") mustBe Reject
    }

    "must throw an exception for any other input" in {
      assertThrows[Exception](RoutingOption.parseRoutingOption("Apples"))
    }
  }
}
