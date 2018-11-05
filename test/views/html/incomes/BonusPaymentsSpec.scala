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
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.YesNoForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.{FormValuesConstants, TaxYearRangeUtil}

class BonusPaymentsSpec extends TaiViewSpec with MockitoSugar with FormValuesConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private val Id = 1
  private val employerName = "Employer"
  private val emptySelectionErrorMessage = messages("tai.bonusPayments.error.form.incomes.radioButton.mandatory",
    TaxYearRangeUtil.currentTaxYearRangeHtmlNonBreak)
  private val bonusPaymentsForm = YesNoForm.form(emptySelectionErrorMessage)
  private val choice = YesNoForm.YesNoChoice

  "Bonus payments view" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeSourceSummaryController.onPageLoad(Id).url))
    behave like pageWithCombinedHeader(
      messages("tai.bonusPayments.preHeading", employerName),
      messages("tai.bonusPayments.title", TaxYearRangeUtil.currentTaxYearRangeHtmlNonBreakBetween))
    behave like pageWithTitle(messages("tai.bonusPayments.title", TaxYearRangeUtil.currentTaxYearRangeHtmlNonBreakBetween))
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

    "return an error for invalid choice" in {
      val invalidChoice = Json.obj(choice -> "")
      val invalidatedForm = bonusPaymentsForm.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(emptySelectionErrorMessage)
      invalidatedForm.value mustBe None
    }

  }

  override def view: Html = views.html.incomes.bonusPayments(bonusPaymentsForm,Id, employerName)
}
