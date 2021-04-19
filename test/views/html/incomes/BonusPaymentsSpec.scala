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

import play.api.libs.json.Json
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.{BonusPaymentsForm, YesNoForm}
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.TaxYearRangeUtil

class BonusPaymentsSpec extends TaiViewSpec with FormValuesConstants {

  val employer = IncomeSource(id = 1, name = "Employer")

  val emptySelectionErrorMessage = messages(
    "tai.bonusPayments.error.form.incomes.radioButton.mandatory",
    TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited)
  val bonusPaymentsForm = BonusPaymentsForm.createForm
  val choice = YesNoForm.YesNoChoice

  private val bonusPayments = inject[bonusPayments]

  override def view: Html = bonusPayments(bonusPaymentsForm, employer)

  "Bonus payments view" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeController.cancel(employer.id).url))
    behave like pageWithCombinedHeader(
      messages("tai.bonusPayments.preHeading", employer.name),
      messages("tai.bonusPayments.title", TaxYearRangeUtil.currentTaxYearRangeSingleLineBetweenDelimited)
    )
    behave like pageWithTitle(messages("tai.bonusPayments.title", TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited))
    behave like pageWithContinueButtonForm("/check-income-tax/update-income/bonus-payments")

    "return no errors with valid 'yes' choice" in {
      val validYesChoice = Json.obj(choice -> YesValue)
      val validatedForm = bonusPaymentsForm.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe YesNoForm(Some(YesValue))
    }

    "return no errors with valid 'no' choice" in {
      val validNoChoice = Json.obj(choice -> NoValue)
      val validatedForm = bonusPaymentsForm.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe YesNoForm(Some(NoValue))
    }

    "display an error for invalid choice" in {
      val invalidChoice = Json.obj(choice -> "")
      val invalidatedForm = bonusPaymentsForm.bind(invalidChoice)

      val errorView = bonusPayments(invalidatedForm, employer)
      doc(errorView) must haveErrorLinkWithText(messages(emptySelectionErrorMessage))
      doc(errorView) must haveClassWithText(messages(emptySelectionErrorMessage), "error-message")
    }
  }
}
