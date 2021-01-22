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

import com.typesafe.config.Config
import config.AppConfig
import play.api.{ConfigLoader, Configuration}

sealed trait RoutingOption

object RoutingOption extends Enumerable.Implicits {
  case object Gb extends RoutingOption
  case object Xi extends RoutingOption
  case object Reject extends RoutingOption

  val values: Seq[RoutingOption] = Seq(Gb, Xi, Reject)

  implicit val enumerable: Enumerable[RoutingOption] =
    Enumerable(values.map(v => v.toString -> v): _*)

  def parseRoutingOption[A](input: String): RoutingOption =
    input match {
      case ro if ro.equals(Gb.toString) => Gb
      case ro if ro.equals(Xi.toString) => Xi
      case _ => Reject
    }
}

