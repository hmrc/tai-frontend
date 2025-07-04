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

import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.AddEmploymentFirstPayForm
import uk.gov.hmrc.tai.forms.pensions.AddPensionProviderFirstPayForm
import uk.gov.hmrc.tai.util.constants.{AddEmploymentFirstPayChoiceConstants, AddPensionFirstPayChoiceConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import views.html.employments.AddEmploymentFirstPayFormView

class AddPensionReceivedFirstPayViewSpec extends TaiViewSpec {

  "Add first pay form page" must {
    behave like pageWithTitle(messages("tai.addPensionProvider.firstPay.pagetitle"))
    behave like pageWithCombinedHeaderNewTemplateNew(
      messages("add.missing.pension"),
      messages("tai.addPensionProvider.firstPay.title", pensionProviderName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like haveLinkWithUrlWithID(
      "backLink",
      controllers.pensions.routes.AddPensionProviderController.addPensionProviderName().url
    )
    behave like pageWithContinueButtonForm("/check-income-tax/add-pension-provider/received-first-payment")
    behave like pageWithYesNoRadioButton(
      AddPensionFirstPayChoiceConstants.FirstPayChoice,
      AddPensionFirstPayChoiceConstants.FirstPayChoice + "-2"
    )
    behave like pageWithCancelLink(controllers.pensions.routes.AddPensionProviderController.cancel())

    "display an error notification" when {
      "no user choice is made" in {
        val noPayrollNumberChooseError           = messages("tai.error.chooseOneOption")
        val expectedErrorMessage                 = messages("tai.error.message") + " " + messages("tai.error.chooseOneOption")
        val formWithErrors: Form[Option[String]] =
          AddEmploymentFirstPayForm.form
            .withError(AddEmploymentFirstPayChoiceConstants.FirstPayChoice, noPayrollNumberChooseError)
        val add_employment_first_pay_form        = inject[AddEmploymentFirstPayFormView]
        def view: Html                           = add_employment_first_pay_form(formWithErrors, pensionProviderName)

        val errorMessage = doc(view).select(".govuk-error-message").text
        errorMessage mustBe expectedErrorMessage
      }
    }
  }

  private lazy val pensionProviderName = "Allied Oatcakes"

  private val pensionFirstPayForm: Form[Option[String]] = AddPensionProviderFirstPayForm.form.bind(
    Map(
      AddPensionFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.YesValue
    )
  )

  private val addPensionReceivedFirstPay = inject[AddPensionReceivedFirstPayView]
  override def view: Html                = addPensionReceivedFirstPay(pensionFirstPayForm, pensionProviderName)
}
