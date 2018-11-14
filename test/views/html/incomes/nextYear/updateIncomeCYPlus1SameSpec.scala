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

package views.html.incomes.nextYear

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.ViewModelHelper.currentTaxYearRangeHtmlNonBreak
import uk.gov.hmrc.tai.util.MonetaryUtil.withPoundPrefixAndSign
class updateIncomeCYPlus1SameSpec extends TaiViewSpec {

  val employerName = "Employer Name"
  val employmentID = 1
  val newAmount = 1234

  "CYPlus1 Same Page" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET",controllers.routes.IncomeTaxComparisonController.onPageLoad.url))
    behave like pageWithCombinedHeader(
      messages("tai.updateIncome.CYPlus1.preheading", employerName),
      messages("tai.updateIncome.CYPlus1.same.heading", currentTaxYearRangeHtmlNonBreak))

    "contain the correct content when new estimated pay equals current estimated pay" in {
      doc(view).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.same.paragraph1", withPoundPrefixAndSign(MoneyPounds(newAmount, 0))))
      doc(view).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.same.paragraph2", employerName))
    }

  }

  override def view: Html = views.html.incomes.nextYear.updateIncomeCYPlus1Same(employerName, employmentID, newAmount)
}
