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

package controllers.actions

import controllers.routes
import models.ChannelType.{Api, Web}
import models.requests.ChannelRequest
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.http.Status
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.{Headers, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

class ChannelActionSpec extends AnyFreeSpec with Matchers with ScalaFutures with EitherValues {

  private class Harness extends ChannelAction {
    def call[A](request: Request[A]): Future[Either[Result, ChannelRequest[A]]] =
      refine(request)
  }

  "ChannelAction" - {
    "returns BadRequest if the channel header is missing" in {
      val futureResult = new Harness().call(FakeRequest("POST", routes.MessagesController.post().url))

      whenReady(futureResult) {
        result =>
        status(Future.successful(result.left.get)) mustBe BAD_REQUEST
      }
    }

    "returns BadRequest if the channel header is malformed" in {
      val futureResult = new Harness().call(FakeRequest("POST", routes.MessagesController.post().url, Headers("channel" -> "abc"), NodeSeq.Empty))

      whenReady(futureResult) {
        result =>
          status(Future.successful(result.left.get)) mustBe BAD_REQUEST
      }
    }

    "returns ChannelRequest if channel header present (web)" in {
      val futureResult = new Harness().call(FakeRequest("POST", routes.MessagesController.post().url, Headers("channel" -> Web.toString), NodeSeq.Empty))

      whenReady(futureResult) {
        result =>
          result.right.get.channel mustBe Web
      }
    }

    "returns ChannelRequest if channel header present (api)" in {
      val futureResult = new Harness().call(FakeRequest("POST", routes.MessagesController.post().url, Headers("channel" -> Api.toString), NodeSeq.Empty))

      whenReady(futureResult) {
        result =>
          result.right.get.channel mustBe Api
      }
    }
  }

}
