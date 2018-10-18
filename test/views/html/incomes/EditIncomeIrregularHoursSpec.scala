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

package views.html.incomes

import org.scalatest.mock.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.EditIncomeIrregularHoursForm
import uk.gov.hmrc.tai.util.ViewModelHelper.currentTaxYearRangeHtmlNonBreak
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.EditIncomeIrregularHoursViewModel


class EditIncomeIrregularHoursSpec extends TaiViewSpec with MockitoSugar {

  private val employerName = "employerName"
  private val currentAmount = 1000
  private val employmentId = 1

  "Edit income Irregular Hours view" should {
    behave like pageWithBackLink
    behave like pageWithTitle(messages("tai.irregular.mainHeadingText"))
    behave like pageWithCombinedHeader(
      messages("tai.estimatedPay.preHeading", employerName),
      messages("tai.irregular.mainHeadingText", currentTaxYearRangeHtmlNonBreak))
    behave like pageWithContinueButtonForm(controllers.routes.IncomeUpdateCalculatorController.handleIncomeIrregularHours(employmentId).url)


    "have the correct content" in {
      doc(view) must haveParagraphWithText(messages("tai.irregular.introduction"))
      doc(view) must haveHeadingH2WithText(messages("tai.irregular.secondaryHeading"))
      doc(view) must haveParagraphWithText(messages("tai.irregular.estimateAnnualAverage"))
      doc(view) must haveParagraphWithText(messages("tai.irregular.instruction.wholePounds"))
    }

    "display the users current estimated income" in {
      doc(view) must haveParagraphWithText(messages("tai.irregular.currentAmount"))
      doc(view) must haveParagraphWithText("£" + currentAmount)
    }

    "have an input box for user to enter new amount" in {
      doc(view) must haveSpanWithText(
        messages("tai.irregular.newAmount") + " " + messages("tai.inPounds")
      )
      doc(view).getElementsByClass("edit-income__input").size() mustBe employmentId
    }
  }

  private val viewModel = EditIncomeIrregularHoursViewModel(employmentId, employerName, currentAmount)
  private val editIncomeForm = EditIncomeIrregularHoursForm.createForm()
  override def view: Html = views.html.incomes.editIncomeIrregularHours(editIncomeForm, viewModel)
}
