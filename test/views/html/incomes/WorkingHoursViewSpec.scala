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

package views.html.incomes

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.HoursWorkedForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class WorkingHoursViewSpec extends TaiViewSpec {

  val empId = 1
  val employerName = "Employer"

  "How to update view" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeSourceSummaryController.onPageLoad(empId).url))
    behave like pageWithCombinedHeader(
      messages("tai.workingHours.preHeading", employerName),
      messages("tai.workingHours.heading"))
  }

  private val template = inject[WorkingHoursView]

  override def view: Html = template(HoursWorkedForm.createForm(), empId, employerName)
}
