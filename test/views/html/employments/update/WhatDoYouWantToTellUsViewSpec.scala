/*
 * Copyright 2022 HM Revenue & Customs
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

package views.html.employments.update

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.UpdateEmploymentDetailsForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.employments.EmploymentViewModel

class WhatDoYouWantToTellUsViewSpec extends TaiViewSpec {

  private val employerName = "testEmployer"
  val empId = 1

  private val template = inject[WhatDoYouWantToTellUsView]

  override def view: Html =
    template(EmploymentViewModel(employerName, empId), UpdateEmploymentDetailsForm.form)

  "whatDoYouWantToTellUs" must {
    behave like pageWithTitle(Messages("tai.updateEmployment.whatDoYouWantToTellUs.pagetitle"))
    behave like pageWithCombinedHeaderNewTemplate(
      Messages("tai.updateEmployment.whatDoYouWantToTellUs.preHeading"),
      Messages("tai.updateEmployment.whatDoYouWantToTellUs.heading", employerName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like pageWithContinueButtonFormNew("/check-income-tax/update-employment/what-do-you-want-to-tell-us/1")
    behave like pageWithCancelLink(controllers.employments.routes.UpdateEmploymentController.cancel(empId))
    behave like pageWithBackLink

  }
}
