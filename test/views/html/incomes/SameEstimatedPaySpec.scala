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

package views.html.incomes

import org.scalatest.mockito.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.TaxYearRangeUtil.currentTaxYearRange
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import uk.gov.hmrc.tai.util.ViewModelHelper.currentTaxYearRangeHtmlNonBreak
import uk.gov.hmrc.tai.viewModels.SameEstimatedPayViewModel

class SameEstimatedPaySpec extends TaiViewSpec with MockitoSugar {

  private val employerName = "employerName"
  private val estimatedAmount = 1000
  private val employmentId = 1

  "Same Estimated Pay view" should {
    behave like pageWithBackLink
    behave like pageWithTitle(messages("tai.incomes.confirm.save.title", currentTaxYearRange))
    behave like pageWithCombinedHeader(
      messages("tai.incomes.edit.preHeading", employerName),
      messages("tai.incomes.confirm.save.heading", currentTaxYearRange))

    "display the users current estimated income" in {
      doc(view) must haveParagraphWithText(messages("tai.incomes.confirm.save.messageEquals", "£1,000"))
    }

    "display a message explaining the results of changing the estimated pay" in {
      doc(view) must haveParagraphWithText(messages("tai.incomes.confirm.save.messageEquals.details", employerName))
    }

    "display a return button" in {
      doc(view) must haveLinkElement(
        id = "returnToEmploymentDetails",
        href = controllers.routes.TaxAccountSummaryController.onPageLoad.url,
        text = messages("tai.updateEmployment.incomeSame.return.link")
      )
    }

  }

  val vm = SameEstimatedPayViewModel(employerName, estimatedAmount)
  override lazy val view: Html = views.html.incomes.sameEstimatedPay(vm)
}
