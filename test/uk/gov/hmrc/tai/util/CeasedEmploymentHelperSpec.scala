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
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.{CeasedEmploymentDetails, TaiRoot}
import TaiConstants._
import controllers.FakeTaiPlayApplication
import play.api.i18n.{I18nSupport, Messages, MessagesApi}

class CeasedEmploymentHelperSpec extends PlaySpec with FakeTaiPlayApplication  with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "fetch isDeceased indicator" should {
    "return true if indicator is true" in {
      sut.isDeceased(Some(TaiRoot(deceasedIndicator = Some(true)))) mustBe true
    }

    "return false if TaiRoot is None" in {
      sut.isDeceased(None) mustBe false
    }

    "return false if TaiRoot is false" in {
      sut.isDeceased(Some(TaiRoot(deceasedIndicator = Some(false)))) mustBe false
    }

    "return false if deceasedIndicator is None" in {
      sut.isDeceased(Some(TaiRoot(deceasedIndicator = Some(false)))) mustBe false
    }
  }

  "getCeasedMsg" should {
    val date = new LocalDate(2016, 6, 9)
    val ceasedEmployment = (isPension: Boolean, ceasedStatus: String) => CeasedEmploymentDetails(Some(date), Some(isPension), Some(ceasedStatus), None)
    "return the correct ceased message" when {
      "employee is CY-1 ceased and is employee" in {
        sut.getCeasedMsg(ceasedEmployment(false, CEASED_MINUS_ONE)) mustBe Some(Messages("tai.paye.ceased.emp.CY-1", date.toString("d MMMM yyyy")))
      }

      "employee is CY-1 ceased and has pension" in {
        sut.getCeasedMsg(ceasedEmployment(true, CEASED_MINUS_ONE)) mustBe Some(Messages("tai.paye.ceased.pension.CY-1", date.toString("d MMMM yyyy")))
      }

      "employee is CY-2 ceased and is employee" in {
        sut.getCeasedMsg(ceasedEmployment(false, CEASED_MINUS_TWO)) mustBe Some(Messages("tai.paye.ceased.emp.CY-2", date.toString("d MMMM yyyy")))
      }

      "employee is CY-2 ceased and has pension" in {
        sut.getCeasedMsg(ceasedEmployment(true, CEASED_MINUS_TWO)) mustBe Some(Messages("tai.paye.ceased.pension.CY-2", date.toString("d MMMM yyyy")))
      }

      "employee is CY-3 ceased and is employee" in {
        sut.getCeasedMsg(ceasedEmployment(false, CeasedMinusThree)) mustBe Some(Messages("tai.paye.ceased.emp.contact", date.toString("d MMMM yyyy")))
      }

      "employee is CY-3 ceased and has pension" in {
        sut.getCeasedMsg(ceasedEmployment(true, CeasedMinusThree)) mustBe Some(Messages("tai.paye.ceased.pension.contact", date.toString("d MMMM yyyy")))
      }


    }
  }
  def sut = CeasedEmploymentHelper
}

