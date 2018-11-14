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
import play.api.libs.json.Json
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.{BonusPaymentsForm, YesNoForm}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.TaxYearRangeUtil

class BonusPaymentsSpec extends TaiViewSpec with MockitoSugar with FormValuesConstants {

  private val Id = 1
  private val employerName = "Employer"
  private val emptySelectionErrorMessage = messages("tai.bonusPayments.error.form.incomes.radioButton.mandatory",
    TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited)
  private val bonusPaymentsForm = BonusPaymentsForm.createForm
  private val choice = YesNoForm.YesNoChoice

  "Bonus payments view" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeSourceSummaryController.onPageLoad(Id).url))
    behave like pageWithCombinedHeader(
      messages("tai.bonusPayments.preHeading", employerName),
      messages("tai.bonusPayments.title", TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited))
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

      val errorView = views.html.incomes.bonusPayments(invalidatedForm,Id, employerName)
      doc(errorView) must haveErrorLinkWithText(messages(emptySelectionErrorMessage))
      doc(errorView) must haveClassWithText(messages(emptySelectionErrorMessage),"error-message")
    }

  }

  override def view: Html = views.html.incomes.bonusPayments(bonusPaymentsForm,Id, employerName)
}
