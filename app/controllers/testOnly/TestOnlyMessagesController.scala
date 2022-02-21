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

package controllers.testOnly

import controllers.MessagesController
import controllers.actions.ChannelAction
import models.requests.ChannelRequest
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton()
class TestOnlyMessagesController @Inject()(
  cc: ControllerComponents,
  channelAction: ChannelAction,
  messagesController: MessagesController
) extends BackendController(cc) {

  def post(): Action[NodeSeq] = (Action andThen channelAction).async(parse.xml) {
    request: ChannelRequest[NodeSeq] =>

      (request.body \\ "_" \ "MesSenMES3").text match {
        case "SYST17B-NCTS_TIMEOUT" => Future.successful(GatewayTimeout)
        case _ => messagesController.post()(request)
      }
  }
}
