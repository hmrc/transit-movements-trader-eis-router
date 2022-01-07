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

import models.DepartureOffice
import models.GuaranteeReference
import models.ParseError._
import models.PresentationOffice
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class XmlParserSpec extends AnyFreeSpec with Matchers with OptionValues {

  "getValidRoot" - {
    "must return None if no valid message can be found" in {
      val input =
        <transitRequest>
        <abc>
        </abc>
        </transitRequest>

      XmlParser.getValidRoot(input).isDefined mustBe false
    }

    "must return Some(NodeSeq) if valid message found" in {
      val input =
        <transitRequest>
        <CC015B>
        </CC015B>
        </transitRequest>

      XmlParser.getValidRoot(input).isDefined mustBe true
    }
  }

  "guaranteeReference" - {
    "must return Left(GuaranteeReferenceEmpty) if guarantee reference missing" in {
      val input =
        <CD034A>
        </CD034A>

      XmlParser.guaranteeReference(input) mustBe a[Left[GuaranteeReferenceEmpty, _]]
    }

    "must return Right(GuaranteeReference) if valid message found" in {
      val input =
        <CD034A>
          <GUAREF2>
            <GuaRefNumGRNREF21>20XI0000010000GX1</GuaRefNumGRNREF21>
          </GUAREF2>
        </CD034A>

      XmlParser.guaranteeReference(input) mustBe a[Right[_, GuaranteeReference]]
    }
  }

  "officeOfDeparture" - {
    "must return Left(DepartureEmpty) if office of departure missing" in {
      val input =
        <CC015B>
        </CC015B>

      XmlParser.officeOfDeparture(input) mustBe a[Left[DepartureEmpty, _]]
    }

    "must return Right(DepartureOffice) if departure office found" in {
      val input =
        <CC015B>
        <CUSOFFDEPEPT>
        <RefNumEPT1>
          test
        </RefNumEPT1>
        </CUSOFFDEPEPT>
        </CC015B>

      XmlParser.officeOfDeparture(input) mustBe a[Right[_, DepartureOffice]]
    }
  }

  "officeOfPresentation" - {
    "must return Left(PresentationEmpty) if office of presentation missing" in {
      val input =
        <CC007A>
        </CC007A>

      XmlParser.officeOfPresentation(input) mustBe a[Left[PresentationEmpty, _]]
    }

    "must return Right(PresentationOffice) if presentation office found" in {
      val input =
        <CC007A>
          <CUSOFFPREOFFRES>
            <RefNumRES1>
              test
            </RefNumRES1>
          </CUSOFFPREOFFRES>
        </CC007A>

      XmlParser.officeOfPresentation(input) mustBe a[Right[_, PresentationOffice]]
    }
  }

}
