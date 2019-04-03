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

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.tai.forms.TaxablePayslipForm
import uk.gov.hmrc.tai.model.domain.income.Employer
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.PaySlipAmountViewModel._

case class TaxablePaySlipAmountViewModel(form: Form[TaxablePayslipForm], title: String, employer: Employer)

object TaxablePaySlipAmountViewModel extends EditIncomePayPeriodConstants {

  def apply(taxablePayslipForm: Form[TaxablePayslipForm],
            payPeriod: Option[String],
            payPeriodInDays: Option[String],
            employer: Employer)
           (implicit message: Messages): TaxablePaySlipAmountViewModel = {

    val messages = Map(MONTHLY -> "tai.taxablePayslip.title.month",
                       WEEKLY -> "tai.taxablePayslip.title.week",
                       FORTNIGHTLY -> "tai.taxablePayslip.title.2week",
                       OTHER -> "tai.taxablePayslip.title.days")

    val title = dynamicTitle(payPeriod, payPeriodInDays, messages)

    TaxablePaySlipAmountViewModel(taxablePayslipForm, title, employer)
  }

}