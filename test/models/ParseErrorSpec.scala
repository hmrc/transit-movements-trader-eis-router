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

import models.ParseError.InvalidMessageCode
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ParseErrorSpec extends AnyFreeSpec with ParseHandling with Matchers with GuiceOneAppPerSuite with OptionValues with ScalaFutures with MockitoSugar with BeforeAndAfterEach {

  "sequenceErrors" - {
    "must uplift a seq of ParseHandlers to expose an error if it contains one" in {
      val inputSequence = Seq(Right("test1"), Right("test2"), Left(InvalidMessageCode("test3")))
      val result = ParseError.sequenceErrors(inputSequence)

      result mustBe a[Left[InvalidMessageCode, _]]
    }

    "must uplift a seq of Parsehandlers exposing no error if there are none" in {
      val inputSequence = Seq(Right("test1"), Right("test2"), Right("test3"))
      val result = ParseError.sequenceErrors(inputSequence)

      result mustBe a[Right[_, Seq[String]]]
      result.right.get mustBe Seq("test1", "test2", "test3")
    }
  }

}
