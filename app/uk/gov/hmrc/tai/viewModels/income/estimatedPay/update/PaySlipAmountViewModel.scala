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
import uk.gov.hmrc.tai.forms.PayslipForm
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants

case class PaySlipAmountViewModel(payslipForm: Form[PayslipForm], payPeriodTitle: String, id: Int, employerName: String)

object PaySlipAmountViewModel extends EditIncomePayPeriodConstants {

  def apply(payslipForm: Form[PayslipForm],
            payPeriod: Option[String],
            payPeriodInDays: Option[String],
            id: Int,
            employerName: String)(implicit message: Messages): PaySlipAmountViewModel = {

    val title = payPeriod match {
      case Some(MONTHLY) => message("tai.payslip.title.month")
      case Some(WEEKLY) => message("tai.payslip.title.week")
      case Some(FORTNIGHTLY) => message("tai.payslip.title.2week")
      case Some(OTHER) => dayPeriodTitle(payPeriodInDays)
      case _ => throw new RuntimeException("No pay period found")
    }

    PaySlipAmountViewModel(payslipForm, title, id, employerName)
  }

  private def dayPeriodTitle(payPeriodInDays: Option[String])
                       (implicit message: Messages): String = {
    payPeriodInDays match {
      case Some(days) => message("tai.payslip.title.days", days)
      case _ => throw new RuntimeException("No days found for pay period")
    }
  }
}