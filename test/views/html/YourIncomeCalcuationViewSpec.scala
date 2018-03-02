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

package views.html

import controllers.routes
import uk.gov.hmrc.tai.viewModels.YourIncomeCalculationViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.EditableDetails
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class YourTaxableIncomeIncomesSpec extends TaiViewSpec {

  override def view: Html = views.html.incomes.yourIncomeCalculation(yourIncomeCalculationViewModel, empId)

  private val customView = (yourIncomeCalculationViewModel: YourIncomeCalculationViewModel, employmentId: Option[Int]) =>
    views.html.incomes.yourIncomeCalculation(yourIncomeCalculationViewModel, employmentId)

  "YourIncomeCalculationView" should {
    "show update message" when {
      "employee is potentially ceased pension" in {
        val view: Html = customView(yourIncomeCalculationViewModel, empId)
        val expectedMessage = messages("tai.income.calculation.potentailly.ceased", 2000)
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveHeadingH4WithText(expectedMessage)
      }

      "employee is potentially ceased employment" in {
        val view: Html = customView(yourIncomeCalculationViewModel.copy(isPension = false), empId)
        val expectedMessage = messages("tai.income.calculation.potentailly.ceased", 2000)
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveHeadingH4WithText(expectedMessage)
      }

    }
    "have link to the Income Tax Summary" in {
      doc must haveLinkWithUrlWithID("returnToSummary", routes.TaxAccountSummaryController.onPageLoad.url)
    }
  }


  val empId = Some(1)
  val yourIncomeCalculationViewModel = YourIncomeCalculationViewModel(employerName = "test",
    employmentPayments = Nil, isPension = true, incomeCalculationMsg = "", incomeCalculationEstimateMsg = Some("We’ve used an estimated amount of £2,000 until we know the actual amount."), empId = 1, hasPrevious = true,
    editableDetails = EditableDetails(payRollingBiks = false, isEditable = true), rtiDown = false,
    employmentStatus = Some(2), endDate = None)

}