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

///*
// * Copyright 2019 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package views.html.employments
//
//import play.api.data.Form
//import play.api.libs.json.Json
//import play.twirl.api.Html
//import uk.gov.hmrc.tai.forms.YesNoForm
//import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
//import uk.gov.hmrc.tai.util.constants.FormValuesConstants
//import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
//
//class DuplicateSubmissionWarningSpec extends TaiViewSpec with FormValuesConstants {
//  val employmentName = "Employment Name"
//  val empId = 1
//  val duplicateSubmissionWarningForm: Form[YesNoForm] = DuplicateSubmissionWarningForm.createForm
//  val choice = YesNoForm.YesNoChoice
//
//  "duplicateSubmissionWarning" must {
//    behave like pageWithTitle(messages("tai.employment.warning.customGaTitle"))
//    behave like pageWithBackLink
//    behave like pageWithCombinedHeader(
//      preHeaderText = messages("tai.employment.warning.preHeading"),
//      mainHeaderText = messages("tai.employment.warning.heading", employmentName))
//
//    behave like pageWithYesNoRadioButton(
//      s"$YesNoChoice-yes",
//      s"$YesNoChoice-no",
//      messages("tai.employment.warning.radio1", employmentName),
//      messages("tai.employment.warning.radio2"))
//
//    behave like pageWithContinueButtonForm("/check-income-tax/update-remove-employment/warning")
//    behave like pageWithCancelLink(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
//
//    "return no errors with valid 'yes' choice" in {
//      val validYesChoice = Json.obj(choice -> YesValue)
//      val validatedForm = duplicateSubmissionWarningForm.bind(validYesChoice)
//
//      validatedForm.errors mustBe empty
//      validatedForm.value.get mustBe YesNoForm(Some(YesValue))
//    }
//
//    "return no errors with valid 'no' choice" in {
//      val validNoChoice = Json.obj(choice -> NoValue)
//      val validatedForm = duplicateSubmissionWarningForm.bind(validNoChoice)
//
//      validatedForm.errors mustBe empty
//      validatedForm.value.get mustBe YesNoForm(Some(NoValue))
//    }
//
//    "display an error for invalid choice" in {
//      val invalidChoice = Json.obj(choice -> "")
//      val invalidatedForm = duplicateSubmissionWarningForm.bind(invalidChoice)
//      val emptySelectionErrorMessage = messages("tai.employment.warning.error")
//
//      val errorView = views.html.employments.duplicateSubmissionWarning(invalidatedForm,employmentName,empId)
//      doc(errorView) must haveErrorLinkWithText(messages(emptySelectionErrorMessage))
//      doc(errorView) must haveClassWithText(messages(emptySelectionErrorMessage),"error-message")
//    }
//  }
//
//  override def view: Html = views.html.employments.duplicateSubmissionWarning(duplicateSubmissionWarningForm,employmentName,empId)
//}