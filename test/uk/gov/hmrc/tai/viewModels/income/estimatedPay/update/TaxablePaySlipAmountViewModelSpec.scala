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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.forms.{PayslipForm, TaxablePayslipForm}
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants

class TaxablePaySlipAmountViewModelSpec extends PlaySpec
  with FakeTaiPlayApplication
  with I18nSupport
  with EditIncomePayPeriodConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val errorMessage = messagesApi("tai.taxablePayslip.error.form.incomes.radioButton.mandatory")
  val form = TaxablePayslipForm.createForm()
  val employerId = 1
  val employerName = "employer name"

  "TaxablePaySlipAmountViewModel" must {
    "have a monthly title for a monthly pay period" in {
      val payPeriod = Some(MONTHLY)
      val viewModel = TaxablePaySlipAmountViewModel(form, payPeriod, None, employerId, employerName)

      viewModel.title mustBe messagesApi("tai.taxablePayslip.title.month")
    }

    "have a weekly title for a weekly pay period" in {
      val payPeriod = Some(WEEKLY)
      val viewModel = TaxablePaySlipAmountViewModel(form, payPeriod, None, employerId, employerName)

      viewModel.title mustBe messagesApi("tai.taxablePayslip.title.week")
    }

    "have a X-day title for a X-pay period" in {
      val payPeriod = Some(OTHER)
      val days = Some("123")
      val viewModel = TaxablePaySlipAmountViewModel(form, payPeriod, days, employerId, employerName)

      viewModel.title mustBe messagesApi("tai.taxablePayslip.title.days", days.getOrElse(""))
    }

    "throw an exception if there is no pay period defined" in {
      val exception = intercept[RuntimeException]{
        val payPeriod = None
        TaxablePaySlipAmountViewModel(form, payPeriod, Some("123"), employerId, employerName)
      }

      exception.getMessage mustBe "No pay period found"
    }

    "throw an exception if there is no pay period in days defined" in {
      val exception = intercept[RuntimeException]{
        val payPeriod = Some(OTHER)
        val payPeriodInDays = None
        TaxablePaySlipAmountViewModel(form, payPeriod, payPeriodInDays, employerId, employerName)
      }

      exception.getMessage mustBe "No days found for pay period"
    }
  }
}
