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

import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.HowToUpdateForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class howToUpdateSpec extends TaiViewSpec {

  "How to update view" should {
    behave like pageWithBackButton(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId: Int))
    behave like pageWithCombinedHeader(
      messages("tai.howToUpdate.preHeading"),
      messages("tai.howToUpdate.title"))
  }

  val empId = 1

  override def view: Html = views.html.incomes.howToUpdate(HowToUpdateForm.createForm(), empId, None)
}
