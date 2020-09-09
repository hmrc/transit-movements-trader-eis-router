/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import config.AppConfig
import models.{ArrivalId, DepartureId}

import scala.concurrent.Future

class MessagesController @Inject()(appConfig: AppConfig, cc: ControllerComponents)
    extends BackendController(cc) {

  def submitArrival(arrivalId: ArrivalId): Action[AnyContent] = Action.async { implicit request =>
      Future.successful(Accepted ("Message accepted") )
  }

  def submitDeparture(departureId: DepartureId): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Accepted ("Message accepted") )
  }
}
