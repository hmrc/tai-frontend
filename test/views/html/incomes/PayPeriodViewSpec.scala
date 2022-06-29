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

package views.html.incomes

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.PayPeriodForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class PayPeriodViewSpec extends TaiViewSpec {

  val id = 1
  val employerName = "Employer"

  "Pay period view" should {
    behave like pageWithBackLinkNew
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeController.cancel(id).url))
    behave like pageWithCombinedHeaderNewTemplate(
      messages("tai.payPeriod.preHeading", employerName),
      messages("tai.payPeriod.heading"),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
  }

  private def payPeriod = inject[PayPeriodView]
  override def view: Html = payPeriod(PayPeriodForm.createForm(None), id, employerName, true)
}
