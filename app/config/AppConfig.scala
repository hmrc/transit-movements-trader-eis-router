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

package config

import javax.inject.{Inject, Singleton}
import models.RoutingOption
import models.RoutingOption.{Gb, Xi}
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String     = config.get[String]("microservice.metrics.graphite.host")

  private val eisBaseUrl: String = servicesConfig.baseUrl("eis")
  val eisniUrl: String             = eisBaseUrl ++ config.get[String]("microservice.services.eis.ni.uri")
  val eisniBearerToken: String     = config.get[String]("microservice.services.eis.ni.headers.bearerToken")
  val eisgbUrl: String             = eisBaseUrl ++ config.get[String]("microservice.services.eis.gb.uri")
  val eisgbBearerToken: String     = config.get[String]("microservice.services.eis.gb.headers.bearerToken")

  val apiXiRoute: RoutingOption = RoutingOption.parseRoutingOption(config.get[String]("microservice.features.apiXIRoute"))
  val webXiRoute: RoutingOption = RoutingOption.parseRoutingOption(config.get[String]("microservice.features.webXIRoute"))
  val apiGbRoute: RoutingOption = RoutingOption.parseRoutingOption(config.get[String]("microservice.features.apiGBRoute"))
  val webGbRoute: RoutingOption = RoutingOption.parseRoutingOption(config.get[String]("microservice.features.webGBRoute"))
}
