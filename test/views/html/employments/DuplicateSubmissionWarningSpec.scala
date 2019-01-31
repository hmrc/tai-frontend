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

package views.html.employments

import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.YesNoForm
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class DuplicateSubmissionWarningSpec extends TaiViewSpec with FormValuesConstants {
  val employmentName = "Employment Name"
  val empId = 1
  val duplicateSubmissionWarningForm: Form[YesNoForm] = DuplicateSubmissionWarningForm.createForm



  "duplicateSubmissionWarning" must {
    behave like pageWithTitle(messages("tai.employment.warning.customGaTitle"))
    behave like pageWithBackLink
    behave like pageWithCombinedHeader(
      preHeaderText = messages("tai.employment.warning.preHeading"),
      mainHeaderText = messages("tai.employment.warning.heading", employmentName))

    behave like pageWithYesNoRadioButton(
      s"$YesNoChoice-yes",
      s"$YesNoChoice-no",
      messages("tai.employment.warning.radio1", employmentName),
      messages("tai.employment.warning.radio2", employmentName))

    behave like pageWithContinueButtonForm("/check-income-tax/update-remove-employment/decision/1")

    behave like pageWithCancelLink(controllers.routes.IncomeSourceSummaryController.onPageLoad(1))
  }

  override def view: Html = views.html.employments.duplicateSubmissionWarning(duplicateSubmissionWarningForm,employmentName,empId)
}