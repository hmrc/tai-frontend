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

package views.html.incomes.nextYear

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.MoneyPounds
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.MonetaryUtil.withPoundPrefixAndSign
import uk.gov.hmrc.tai.util.TaxYearRangeUtil

class UpdateIncomeCYPlus1SameViewSpec extends TaiViewSpec {

  val employerName = "Employer Name"
  val employmentID = 1
  val newAmount = 1234
  private val updateIncomeCYPlus1Same = inject[UpdateIncomeCYPlus1SameView]

  override def view: Html = updateIncomeCYPlus1Same(employerName, employmentID, newAmount)

  "CYPlus1 Same Page" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeTaxComparisonController.onPageLoad.url))
    behave like pageWithCombinedHeaderNewFormat(
      messages("tai.updateIncome.CYPlus1.preheading", employerName),
      messages(
        "tai.updateIncome.CYPlus1.same.heading",
        TaxYearRangeUtil.futureTaxYearRange(1).replaceAll("\u00A0", " "))
    )

    "contain the correct content when new estimated pay equals current estimated pay" in {
      val document = doc(view)

      document.getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.confirm.paragraph"))
      document.getElementsByTag("p").text must include(
        messages(
          "tai.updateEmployment.incomeSame.description",
          employerName,
          TaxYearRangeUtil.futureTaxYearRange(1).replaceAll("\u00A0", " ")))
    }
  }
}
