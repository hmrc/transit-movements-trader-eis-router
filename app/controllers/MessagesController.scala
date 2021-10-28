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
import play.api.mvc.BodyParser
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.mvc.Request
import play.api.mvc.AnyContent
import models.ChannelType
import play.api.mvc.Result
import play.api.Logging

@Singleton()
class MessagesController @Inject() (
  cc: ControllerComponents,
  routingService: RoutingService
) extends BackendController(cc) with Logging {

  val stream = BodyParser[Source[ByteString, _]]("stream") { _ =>
    Accumulator
      .source[ByteString]
      .map(Right.apply)
  }

  private def requireChannelHeader[A](
    result: ChannelType => Future[Result]
  )(implicit request: Request[A]): Future[Result] =
    request.headers
      .get("channel")
      .flatMap(ChannelType.withName)
      .map(result)
      .getOrElse {
        Future.successful(
          BadRequest("Missing channel header or incorrect value specified in channel header")
        )
      }

  def post(): Action[Source[ByteString, _]] = Action.async(stream) {
    implicit request: Request[Source[ByteString, _]] =>
      requireChannelHeader { channel =>
        routingService
          .submitMessage(request.body, channel, HeaderCarrierConverter.fromRequest(request))
          .map {
            case Left(error) =>
              logger.error(error.message)
              BadRequest(error.message)
            case Right(response) =>
              response.status match {
                case ACCEPTED                                => Accepted
                case FORBIDDEN                               => InternalServerError
                case GATEWAY_TIMEOUT | INTERNAL_SERVER_ERROR => BadGateway
                case status                                  => Status(status)
              }
          }
      }
  }
}
