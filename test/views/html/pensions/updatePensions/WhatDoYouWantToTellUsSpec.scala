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

package views.html.pensions.updatePensions

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.UpdateEmploymentDetailsForm
import uk.gov.hmrc.tai.forms.pensions.WhatDoYouWantToTellUsForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.employments.EmploymentViewModel

class WhatDoYouWantToTellUsSpec extends TaiViewSpec {

  private val pensionName = "testPension"
  private val pensionId = 1

  override def view: Html =
    views.html.pensions.update.whatDoYouWantToTellUs(pensionName, pensionId, WhatDoYouWantToTellUsForm.form)

  "whatDoYouWantToTellUs" must {
    behave like pageWithTitle(Messages("tai.updatePension.whatDoYouWantToTellUs.pagetitle"))
    behave like pageWithCombinedHeader(
      Messages("tai.updatePension.preHeading"),
      Messages("tai.updatePension.whatDoYouWantToTellUs.heading", pensionName))
    behave like pageWithContinueButtonForm("/check-income-tax/incorrect-pension/what-do-you-want-to-tell-us")
    behave like pageWithCancelLink(controllers.pensions.routes.UpdatePensionProviderController.cancel(pensionId))
    behave like pageWithBackLink

    "display a text area to collect further information" in {
      doc must haveElementAtPathWithAttribute("textarea", "name", "pensionDetails")
      doc must haveElementAtPathWithAttribute("textarea", "maxlength", "500")
    }

  }
}
