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

import com.google.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class RoutingConfig @Inject() (config: Configuration) {

  val apiXi: Boolean = config.get[Boolean]("microservice.features.routing.api.xi")
  val webXi: Boolean = config.get[Boolean]("microservice.features.routing.web.xi")
  val apiGb: Boolean = config.get[Boolean]("microservice.features.routing.api.gb")
  val webGb: Boolean = config.get[Boolean]("microservice.features.routing.web.gb")

}
