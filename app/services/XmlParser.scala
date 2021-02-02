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

import cats.data.ReaderT
import models.ParseError.{DepartureEmpty, PresentationEmpty}
import models.{DepartureOffice, MessageType, ParseHandling, PresentationOffice}

import scala.xml.NodeSeq

object XmlParser extends ParseHandling {

  def getValidRoot(xml: NodeSeq): Option[(NodeSeq, MessageType)] = {
    MessageType.validMessages.map { m =>
      val result = (xml \\ m.rootNode)
      (result, m)
    }.filterNot(pair => pair._1 == NodeSeq.Empty).headOption
  }

  val officeOfDeparture: ReaderT[ParseHandler, NodeSeq, DepartureOffice] =
    ReaderT[ParseHandler, NodeSeq, DepartureOffice](xml => {
      (xml \\ "CUSOFFDEPEPT" \ "RefNumEPT1").text match {
        case departure if departure.isEmpty => Left(DepartureEmpty("Departure Empty"))
        case departure => Right(DepartureOffice(departure))
      }
    })

  val officeOfPresentation: ReaderT[ParseHandler, NodeSeq, PresentationOffice] =
    ReaderT[ParseHandler, NodeSeq, PresentationOffice](xml => {
      (xml \ "CUSOFFPREOFFRES" \ "RefNumRES1").text match {
        case presentation if presentation.isEmpty => Left(PresentationEmpty("Presentation Empty"))
        case presentation => Right(PresentationOffice(presentation))
      }
    })
}
