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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsValue, Json}

import java.time.{LocalDateTime, ZoneOffset}

class MovementSpec extends AnyFreeSpec with Matchers {

  val movement: Movement = Movement(
    "TEST-ID", "IE015", LocalDateTime.ofEpochSecond(1638349126L, 0, ZoneOffset.UTC), "GB"
  )

  val movementJson: JsValue = Json.obj("id" -> "TEST-ID", "messageCode" -> "IE015", "timestamp" -> "2021-12-01T08:58:46", "office" -> "GB")

  "Movement" - {
    "must convert from Model to Json" in {
      Json.toJson(movement) mustBe movementJson
    }

    "must convert from Json to Model" in {
      movementJson.as[Movement] mustBe movement
    }
  }
}
