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

package views.html.incomes

import org.scalatest.mockito.MockitoSugar
import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.TaxablePayslipForm
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.TaxablePaySlipAmountViewModel

class TaxablePaySlipAmountSpec extends TaiViewSpec with MockitoSugar with EditIncomePayPeriodConstants {


  val id = 1
  val employerName = "Employer"
  val taxablePayslipViewModel = createViewModel()

  "Taxable Pay slip amount view" should {
    behave like pageWithTitle(messages("tai.taxablePayslip.title.month", MONTHLY))
    behave like pageWithCombinedHeader(messages("tai.howToUpdate.preHeading", employerName), messages("tai.taxablePayslip.title.month", MONTHLY))
    behave like pageWithBackLink
    behave like pageWithCancelLink(controllers.routes.IncomeSourceSummaryController.onPageLoad(taxablePayslipViewModel.id))
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
      val errorView = views.html.incomes.taxablePayslipAmount(viewModelError)
      doc(errorView) must haveErrorLinkWithText(messages("tai.taxablePayslip.error.form.incomes.radioButton.mandatory"))
    }

  }

  def createViewModel(form: Form[TaxablePayslipForm] = TaxablePayslipForm.createForm(None, Some(MONTHLY), None)) =
    TaxablePaySlipAmountViewModel(form, Some(MONTHLY), None, id, employerName)

  override def view: Html = views.html.incomes.taxablePayslipAmount(taxablePayslipViewModel)
}
