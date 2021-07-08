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

sealed abstract class MessageType(val code: String, val rootNode: String) extends Product with Serializable

object MessageType {
  case object ArrivalNotification             extends MessageType("IE007", "CC007A")
  case object UnloadingRemarks                extends MessageType("IE044", "CC044A")
  case object DepartureDeclaration            extends MessageType("IE015", "CC015B")
  case object DeclarationCancellationRequest  extends MessageType("IE014", "CC014A")

  val values: Seq[MessageType] = Seq(
    ArrivalNotification,
    UnloadingRemarks,
    DepartureDeclaration,
    DeclarationCancellationRequest
  )

  val departureValues: Seq[MessageType] = Seq(DepartureDeclaration, DeclarationCancellationRequest)

  val arrivalValues: Seq[MessageType] = Seq(ArrivalNotification, UnloadingRemarks)

  val validMessages: Seq[MessageType] = departureValues ++ arrivalValues

  val allMessages: Seq[MessageType] = departureValues ++ arrivalValues
}
