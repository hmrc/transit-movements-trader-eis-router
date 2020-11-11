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
import connectors.MessageConnector
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.NodeSeq

@Singleton()
class MessagesController @Inject()(appConfig: AppConfig, cc: ControllerComponents, connector: MessageConnector)
  extends BackendController(cc) {

  def post(): Action[NodeSeq] = Action.async(parse.xml) { implicit request: Request[NodeSeq] =>
    connector.post(request.body.toString()).map(response => response.status match {
      case ACCEPTED => Accepted ("Message accepted")
      case INTERNAL_SERVER_ERROR => BadGateway
      case GATEWAY_TIMEOUT => BadGateway
      case _ => Status(response.status) })
  }
}
