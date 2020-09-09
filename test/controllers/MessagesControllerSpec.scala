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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import config.AppConfig

class MessagesControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val requestXmlBody = <CC007A>
    <SynIdeMES1>UNOC</SynIdeMES1>
    <SynVerNumMES2>3</SynVerNumMES2>
    <MesRecMES6>NCTS</MesRecMES6>
    <DatOfPreMES9>20200204</DatOfPreMES9>
    <TimOfPreMES10>1302</TimOfPreMES10>
    <IntConRefMES11>WE202002046</IntConRefMES11>
    <AppRefMES14>NCTS</AppRefMES14>
    <TesIndMES18>0</TesIndMES18>
    <MesIdeMES19>1</MesIdeMES19>
    <MesTypMES20>GB007A</MesTypMES20>
    <HEAHEA>
      <DocNumHEA5>99IT9876AB88901209</DocNumHEA5>
      <CusSubPlaHEA66>EXAMPLE1</CusSubPlaHEA66>
      <ArrNotPlaHEA60>NW16XE</ArrNotPlaHEA60>
      <ArrNotPlaHEA60LNG>EN</ArrNotPlaHEA60LNG>
      <ArrAgrLocOfGooHEA63LNG>EN</ArrAgrLocOfGooHEA63LNG>
      <SimProFlaHEA132>0</SimProFlaHEA132>
      <ArrNotDatHEA141>20200204</ArrNotDatHEA141>
    </HEAHEA>
    <TRADESTRD>
      <NamTRD7>EXAMPLE2</NamTRD7>
      <StrAndNumTRD22>Baker Street</StrAndNumTRD22>
      <PosCodTRD23>NW16XE</PosCodTRD23>
      <CitTRD24>London</CitTRD24>
      <CouTRD25>GB</CouTRD25>
      <NADLNGRD>EN</NADLNGRD>
      <TINTRD59>EXAMPLE3</TINTRD59>
    </TRADESTRD>
    <CUSOFFPREOFFRES>
      <RefNumRES1>GB000128</RefNumRES1>
    </CUSOFFPREOFFRES>
  </CC007A>

  private val fakeRequest = FakeRequest("POST", "/messages").withXmlBody(requestXmlBody)
  private val fakeEmptyRequest = FakeRequest("POST", "/messages")

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  private val controller = new MessagesController(appConfig, Helpers.stubControllerComponents())

  "POST any XML" should {
    "should return 202 Accepted" in {
      val result = controller.post()(fakeRequest)
      status(result) shouldBe ACCEPTED
    }
  }

}
