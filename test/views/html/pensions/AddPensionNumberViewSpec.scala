/*
 * Copyright 2023 HM Revenue & Customs
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

package views.html.pensions

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.pensions.AddPensionProviderNumberForm
import uk.gov.hmrc.tai.util.constants.{AddPensionNumberConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.pensions.PensionNumberViewModel

class AddPensionNumberViewSpec extends TaiViewSpec {

  private val addPensionNumber = inject[AddPensionNumberView]
  override def view: Html      = addPensionNumber(pensionNumberForm, pensionNumberViewModel)

  "Add payroll number form page" must {
    behave like pageWithTitle(messages("tai.addPensionProvider.pensionNumber.pagetitle"))
    behave like pageWithCombinedHeaderNewTemplateNew(
      messages("add.missing.pension"),
      messages("tai.addPensionProvider.pensionNumber.title", pensionProviderName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like pageWithBackLink()
    behave like pageWithContinueButtonForm("/check-income-tax/add-pension-provider/pension-number")
    behave like pageWithYesNoRadioButton(
      AddPensionNumberConstants.PayrollNumberChoice,
      AddPensionNumberConstants.PayrollNumberChoice + "-2"
    )
    behave like pageWithCancelLink(controllers.pensions.routes.AddPensionProviderController.cancel())

    "have gone back to firstPayChoice page" in {
      def view: Html    = addPensionNumber(pensionNumberForm, pensionNumberViewModel)
      def doc: Document = Jsoup.parse(view.toString())
      doc must haveBackLink
    }

    "have an input field for payroll number" in {
      doc.getElementById("payrollNumberEntry") must not be null
    }

    "have an error message with the form inputs" when {
      "no payroll number choice is selected" in {
        val noPayrollNumberChooseError                         = messages("tai.addPensionProvider.pensionNumber.error.selectOption")
        val expectedErrorMessage                               =
          messages("tai.error.message") + " " + messages("tai.addPensionProvider.pensionNumber.error.selectOption")
        val formWithErrors: Form[AddPensionProviderNumberForm] = AddPensionProviderNumberForm.form
          .withError(AddPensionNumberConstants.PayrollNumberChoice, noPayrollNumberChooseError)
        def view: Html                                         = addPensionNumber(formWithErrors, pensionNumberViewModel)

        val errorMessage = doc(view).select(".govuk-error-message").text
        errorMessage mustBe expectedErrorMessage
      }

      "no payroll number is provided" in {
        val noPayrollNumberChooseError                         = messages("tai.addPensionProvider.pensionNumber.error.blank")
        val expectedErrorMessage                               =
          messages("tai.error.message") + " " + messages("tai.addPensionProvider.pensionNumber.error.blank")
        val formWithErrors: Form[AddPensionProviderNumberForm] = AddPensionProviderNumberForm.form
          .withError(AddPensionNumberConstants.PayrollNumberEntry, noPayrollNumberChooseError)
        def view: Html                                         = addPensionNumber(formWithErrors, pensionNumberViewModel)

        val errorMessage = doc(view).select(".govuk-error-message").text
        errorMessage mustBe expectedErrorMessage
      }
    }
  }

  private val pensionNumberForm: Form[AddPensionProviderNumberForm] = AddPensionProviderNumberForm.form.bind(
    Map(
      AddPensionNumberConstants.PayrollNumberChoice -> FormValuesConstants.NoValue
    )
  )

  private lazy val pensionProviderName    = "Aviva"
  private lazy val pensionNumberViewModel = PensionNumberViewModel(pensionProviderName, firstPayChoice = false)
}
