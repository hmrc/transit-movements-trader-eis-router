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

import cats.implicits._

trait FailureMessage {
  def message: String
}

final case class RejectionMessage(message: String) extends FailureMessage

sealed trait ParseError extends FailureMessage

object ParseError extends ParseHandling {

  final case class InvalidMessageCode(message: String) extends ParseError
  final case class PresentationEmpty(message: String)   extends ParseError
  final case class DepartureEmpty(message: String)     extends ParseError

  def sequenceErrors[A](input: Seq[ParseHandler[A]]): ParseHandler[Seq[A]] = {
    input.toList.sequence.map { _.toSeq }
  }

}