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

package views.html.incomes.nextYear

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.EditIncomeIrregularHoursForm
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.util.ViewModelHelper.currentTaxYearRangeHtmlNonBreak
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class updateIncomeCYPlus1EditSpec extends TaiViewSpec {

  val employerName = "Employer Name"
  val employmentID = 1
  val currentEstPay = 1234

  val model = UpdateNextYearsIncomeCacheModel(employerName, employmentID, currentEstPay)

  "CYPlus1 Start Page" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET",controllers.routes.IncomeTaxComparisonController.onPageLoad.url))
    behave like pageWithCombinedHeader(
      messages("tai.updateIncome.CYPlus1.preheading", employerName),
      messages("tai.updateIncome.CYPlus1.edit.heading", currentTaxYearRangeHtmlNonBreak))
    behave like pageWithContinueInputForm("/check-income-tax/update-income/next-year/income/1/update")

    "contain the correct content when income is from employment" in {
      doc(view).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.edit.paragraph1"))
      doc(view).getElementsByTag("p").text must include(messages("tai.incomes.edit.howTo.grossAmount"))
      doc(view).getElementsByTag("li").text must include(messages("tai.incomes.edit.howTo.contribution"))
      doc(view).getElementsByTag("li").text must include(messages("tai.incomes.edit.howTo.charity"))
      doc(view).getElementsByTag("li").text must include(messages("tai.incomes.edit.howTo.expenses"))
      //doc(view) must haveSubmitButton(messages("tai.continue"))
    }

  }


  override def view: Html = views.html.incomes.nextYear.updateIncomeCYPlus1Edit(model, EditIncomeIrregularHoursForm.createForm(taxablePayYTD = Some(currentEstPay)))
}
