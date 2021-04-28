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

package views.html.incomes

import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.TaxablePayslipForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.TaxablePaySlipAmountViewModel

class TaxablePayslipAmountViewSpec extends TaiViewSpec with EditIncomePayPeriodConstants {

  private val employerName = "Employer"
  private val employer = IncomeSource(id = 1, employerName)
  private val taxablePayslipViewModel = createViewModel()

  def createViewModel(form: Form[TaxablePayslipForm] = TaxablePayslipForm.createForm(None, Some(MONTHLY), None)) =
    TaxablePaySlipAmountViewModel(form, Some(MONTHLY), None, employer)

  private val template = inject[TaxablePayslipAmountView]

  override def view: Html = template(taxablePayslipViewModel)

  "Taxable Pay slip amount view" should {
    behave like pageWithTitle(messages("tai.taxablePayslip.title.month", MONTHLY))
    behave like pageWithCombinedHeader(
      messages("tai.howToUpdate.preHeading", employerName),
      messages("tai.taxablePayslip.title.month", MONTHLY))
    behave like pageWithBackLink
    behave like pageWithCancelLink(controllers.routes.IncomeController.cancel(taxablePayslipViewModel.employer.id))
    behave like pageWithButtonForm("/check-income-tax/update-income/taxable-payslip-amount", messages("tai.submit"))
  }

  "display two explanation paragraphs" in {
    doc must haveParagraphWithText(messages("tai.taxablePayslip.taxablePay.explanation"))
    doc must haveParagraphWithText(messages("tai.taxablePayslip.shownOnPayslip"))
  }

  "display error message" when {
    "the taxable pay has not been entered" in {
      val formWithErrors = TaxablePayslipForm.createForm(None, Some("blah"), None).bind(Map("taxablePay" -> ""))
      val viewModelError = createViewModel(formWithErrors)
      val errorView = template(viewModelError)
      doc(errorView) must haveErrorLinkWithText(messages("tai.taxablePayslip.error.form.incomes.radioButton.mandatory"))
    }

  }
}
