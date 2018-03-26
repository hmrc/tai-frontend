/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util

import org.joda.time.LocalDate
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.rti.{PayFrequency, RtiEyu, RtiPayment}
import uk.gov.hmrc.tai.util.YourIncomeCalculationHelper._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import TaiConstants._
import controllers.FakeTaiPlayApplication

class YourIncomeCalculationHelperSpec extends UnitSpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val date = new LocalDate(2016, 6,1)
  val rtiEyuPayDeltaNI = RtiEyu(Some(100.0), Some(0), Some(-200.0), date)
  val rtiEyuPayDelta = RtiEyu(Some(100.0), Some(0), Some(0), date)
  val rtiEyuTaxDelta = RtiEyu(Some(0), Some(300.0), Some(0), date)
  val rtiEyuNI = RtiEyu(Some(0), Some(0), Some(400.0), date)
  val rtiEyu = RtiEyu(None, None, None, date)
  val employerName = "employer"

  /*"EYU message" should {
    "have 4 messages" in {
      val rtiEyuList = List(rtiEyuPayDeltaNI, rtiEyuTaxDelta, rtiEyuNI)
      val messages = getEyuMessage(rtiEyuList, employerName)
      messages shouldNot be(Nil)
      messages.size should be(4)
      Messages(messages(0)) should be(Messages("tai.income.calculation.eyu.multi.taxableincome", date.toString(EYU_DATE_FORMAT), "100.0 more"))
      Messages(messages(1)) should be(Messages("tai.income.calculation.eyu.multi.nationalInsurance", date.toString(EYU_DATE_FORMAT), "200.0 less"))
    }

    "have tax paid message" in {
      val rtiEyuList = List(rtiEyuTaxDelta)
      val messages = getEyuMessage(rtiEyuList, employerName)
      messages shouldNot be(Nil)
      messages.size should be(1)
      Messages(messages(0)) should be(Messages("tai.income.calculation.eyu.single.taxPaid", date.toString(EYU_DATE_FORMAT), "300.0 more"))
    }

    "have 2 messages for single RtiEyu object" in {
      val rtiEyuList = List(rtiEyuPayDeltaNI)
      val messages = getEyuMessage(rtiEyuList, employerName)
      messages shouldNot be(Nil)
      messages.size should be(2)
      Messages(messages(0)) should be(Messages("tai.income.calculation.eyu.multi.taxableincome", date.toString(EYU_DATE_FORMAT), "100.0 more"))
      Messages(messages(1)) should be(Messages("tai.income.calculation.eyu.multi.nationalInsurance", date.toString(EYU_DATE_FORMAT), "200.0 less"))
    }

    "have NI message" in {
      val rtiEyuList = List(rtiEyuNI)
      val messages = getEyuMessage(rtiEyuList, employerName)
      messages shouldNot be(Nil)
      messages.size should be(1)
      Messages(messages(0)) should be(Messages("tai.income.calculation.eyu.single.nationalInsurance", date.toString(EYU_DATE_FORMAT), "400.0 more"))
    }

    "have pay delta message" in {
      val rtiEyuList = List(rtiEyuPayDelta)
      val messages = getEyuMessage(rtiEyuList, employerName)
      messages shouldNot be(Nil)
      messages.size should be(1)
      Messages(messages(0)) should be(Messages("tai.income.calculation.eyu.single.taxableincome", date.toString(EYU_DATE_FORMAT) ,"100.0 more"))
    }

    "not have any message as there are no EYU properties" in {
      val rtiEyuList = List(rtiEyu)
      val messages = getEyuMessage(rtiEyuList, employerName)
      messages should be(Nil)
    }

    "not have any message as list is Nil" in {
      val rtiEyuList = Nil
      val messages = getEyuMessage(rtiEyuList, employerName)
      messages should be(Nil)
    }
  }*/

}
