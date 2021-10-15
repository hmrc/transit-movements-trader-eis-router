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

package services

import models.DepartureOffice
import models.GuaranteeReference
import models.MessageType
import models.ParseError
import models.ParseError._
import models.PresentationOffice

import scala.xml.NodeSeq

object XmlParser {
  type ParseHandler[A] = Either[ParseError, A]

  case class RootNode(messageType: MessageType, message: NodeSeq)

  def getValidRoot(xml: NodeSeq): Option[RootNode] =
    MessageType.values.collectFirst {
      case messageType if (xml \ messageType.rootNode).nonEmpty =>
        RootNode(messageType, xml \ messageType.rootNode)
    }

  def guaranteeReference(xml: NodeSeq): ParseHandler[GuaranteeReference] =
    (xml \\ "GUAREF2" \ "GuaRefNumGRNREF21").text match {
      case grn if grn.isEmpty => Left(GuaranteeReferenceEmpty("Guarantee Reference Empty"))
      case grn => Right(GuaranteeReference(grn))
    }

  def officeOfDeparture(xml: NodeSeq): ParseHandler[DepartureOffice] =
    (xml \\ "CUSOFFDEPEPT" \ "RefNumEPT1").text match {
      case departure if departure.isEmpty => Left(DepartureEmpty("Departure Empty"))
      case departure                      => Right(DepartureOffice(departure))
    }

  def officeOfPresentation(xml: NodeSeq): ParseHandler[PresentationOffice] =
    (xml \ "CUSOFFPREOFFRES" \ "RefNumRES1").text match {
      case presentation if presentation.isEmpty => Left(PresentationEmpty("Presentation Empty"))
      case presentation                         => Right(PresentationOffice(presentation))
    }
}
