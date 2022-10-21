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

import models.ChannelType.Api
import models.ChannelType.Web
import models.RoutingOption.Gb
import models.RoutingOption.Xi
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.mockito.Mockito.when

class RouteCheckerSpec extends AnyFreeSpec with MockitoSugar with Matchers with ScalaCheckDrivenPropertyChecks {

  val routingOptionGen = Gen.oneOf[Boolean](true, false)

  "canForward" - {
    "when input route is Xi and channel is web, must return the RoutingOption for webNi in the appConfig" in {
      forAll(routingOptionGen) {
        opt =>
          val mockConfig = mock[RoutingConfig]
          when(mockConfig.webXi).thenReturn(opt)

          new RouteChecker(mockConfig).canForward(Xi, Web) mustBe opt
      }
    }

    "when input route is Xi and channel is api, must return the RoutingOption for apiXi in the appConfig" in {
      forAll(routingOptionGen) {
        opt =>
          val mockConfig = mock[RoutingConfig]
          when(mockConfig.apiXi).thenReturn(opt)

          new RouteChecker(mockConfig).canForward(Xi, Api) mustBe opt
      }
    }

    "when input route is Gb and channel is web, must return the RoutingOption for webGb in the appConfig" in {
      forAll(routingOptionGen) {
        opt =>
          val mockConfig = mock[RoutingConfig]
          when(mockConfig.webGb).thenReturn(opt)

          new RouteChecker(mockConfig).canForward(Gb, Web) mustBe opt
      }
    }

    "when input route is Gb and channel is api, must return the RoutingOption for apiGb in the appConfig" in {
      forAll(routingOptionGen) {
        opt =>
          val mockConfig = mock[RoutingConfig]
          when(mockConfig.apiGb).thenReturn(opt)

          new RouteChecker(mockConfig).canForward(Gb, Api) mustBe opt
      }
    }
  }

}
