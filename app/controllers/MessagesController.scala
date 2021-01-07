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

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, ControllerComponents, Request}
import services.RoutingService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton()
class MessagesController @Inject()(appConfig: AppConfig, cc: ControllerComponents, routingService: RoutingService)
  extends BackendController(cc) {

  def post(): Action[NodeSeq] = Action.async(parse.xml) { implicit request: Request[NodeSeq] =>
    routingService.submitMessage(request.body) match {
      case Left(message) => Future.successful(BadRequest(message))
      case Right(response) => response.map {
        r =>
          r.status match {
            case ACCEPTED => Accepted("Message accepted")
            case _ => Status(r.status)
          }
      }
    }
  }
}