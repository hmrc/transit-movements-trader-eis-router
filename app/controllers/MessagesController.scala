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

package controllers

import config.AppConfig
import controllers.actions.ChannelAction
import models.requests.ChannelRequest
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import services.RoutingService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton()
class MessagesController @Inject() (
  appConfig: AppConfig,
  cc: ControllerComponents,
  channelAction: ChannelAction,
  routingService: RoutingService
) extends BackendController(cc) {

  def post(): Action[NodeSeq] = (Action andThen channelAction).async(parse.xml) {
    request: ChannelRequest[NodeSeq] =>
      val headerCarrier = HeaderCarrierConverter.fromRequest(request)

      routingService.submitMessage(request.body, request.channel, headerCarrier) match {
        case Left(error) =>
          Future.successful(BadRequest(error.message))
        case Right(response) =>
          response.map(_.status match {
            case ACCEPTED                                => Accepted
            case FORBIDDEN                               => InternalServerError
            case GATEWAY_TIMEOUT | INTERNAL_SERVER_ERROR => BadGateway
            case status                                  => Status(status)
          })
      }
  }
}
