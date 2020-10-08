/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.pensions.update

import controllers.routes
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class ConfirmationSpec extends TaiViewSpec {
  override def view: Html = views.html.pensions.update.confirmation()

  "Confirmation View" must {

    behave like pageWithTitle(messages("tai.updatePension.confirmation.heading"))
    behave like pageWithHeader(messages("tai.confirmation.heading"))
    behave like haveReturnToSummaryButtonWithUrl(routes.TaxAccountSummaryController.onPageLoad())

  }
}
