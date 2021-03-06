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

import models.RoutingOption.{Gb, Xi}

trait Office {
  def value: String
  def getRoutingOption: RoutingOption = value match {
    case v if v.startsWith(Xi.toString.toUpperCase()) => Xi
    case _ => Gb
  }
}

final case class DepartureOffice(value: String) extends Office
final case class PresentationOffice(value: String) extends Office
