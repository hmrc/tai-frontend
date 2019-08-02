/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.tai.forms

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages, MessagesApi}

class IncomeCalculatorFormSpec extends PlaySpec with OneAppPerSuite with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "PayPeriodForm" must {
    "return no errors" when {
      Seq("monthly", "weekly", "fortnightly").foreach(period => {
        s"provided with valid data payPeriod: $period" in {
          val payPeriodForm = PayPeriodForm.createForm(None).bind(Map("payPeriod" -> period))
          payPeriodForm.errors mustBe empty
        }
      })

      "provided with a 'other' payPeriod and a valid number of days" in {
        val validDaysMap = Map("payPeriod" -> "other", "otherInDays" -> "3")
        val payPeriodForm = PayPeriodForm.createForm(None, Some("other")).bind(validDaysMap)
        payPeriodForm.errors mustBe empty
      }
    }

    "return a mandatory field error" when {
      "no payPeriod is sent" in {
        val payPeriodForm = PayPeriodForm.createForm(None).bind(Map("payPeriod" -> ""))
        payPeriodForm.errors must contain(
          FormError("payPeriod", List(Messages("tai.payPeriod.error.form.incomes.radioButton.mandatory"))))
      }

      "invalid payPeriod is sent" in {
        val payPeriodForm = PayPeriodForm.createForm(None).bind(Map("payPeriod" -> "Nope"))
        payPeriodForm.errors must contain(
          FormError("payPeriod", List(Messages("tai.payPeriod.error.form.incomes.radioButton.mandatory"))))
      }

      "provided with a 'other' payPeriod with no number of days passed" in {
        val invalidDaysMap = Map("otherInDays" -> "")
        val payPeriodForm = PayPeriodForm.createForm(None, Some("other")).bind(invalidDaysMap)
        payPeriodForm.errors must contain(
          FormError("otherInDays", List(Messages("tai.payPeriod.error.form.incomes.other.mandatory"))))
      }

      Seq("Nope", "123A", "A123", "2232.00", "Ten", "3 Days").foreach(invalidInput => {
        s"provided with a 'other' payPeriod with invalid input '$invalidInput' for number of days" in {
          val invalidDaysMap = Map("payPeriod" -> "other", "otherInDays" -> invalidInput)
          val payPeriodForm = PayPeriodForm.createForm(None, Some("other")).bind(invalidDaysMap)
          payPeriodForm.errors must contain(
            FormError("otherInDays", List(Messages("tai.payPeriod.error.form.incomes.other.invalid"))))
        }
      })
    }
  }
}
