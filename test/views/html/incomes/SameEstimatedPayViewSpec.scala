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

package views.html.incomes

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.SameEstimatedPayViewModel

import java.time.LocalDate

class SameEstimatedPayViewSpec extends TaiViewSpec {

  val employerName = "Employer"
  val amount = "£20,000"
  val amountAsInt = 20000
  val employerId = 1
  val url = "some url"

  private val template = inject[SameEstimatedPayView]

  override def view: Html = template(createViewModel())

  def createViewModel(employmentStartDate: Option[LocalDate] = None) =
    SameEstimatedPayViewModel(employerName, employerId, amount = amountAsInt, isPension = false, url)

  "Same estimated pay page" must {
    behave like pageWithBackLink
    behave like pageWithTitle(messages("tai.updateEmployment.incomeSame.title", TaxYearRangeUtil.currentTaxYearRange))
    behave like pageWithHeader(
      messages("tai.updateEmployemnt.incomeSame.heading", TaxYearRangeUtil.currentTaxYearRange))

    "display the new estimated income" in {
      val newEstimateMessage = messages("tai.updateEmployment.incomeSame.newEstimate.text")
      doc must haveParagraphWithText(messages(s"$newEstimateMessage $amount"))
    }

    "display a paragraph" in {

      doc must haveParagraphWithText(
        messages("tai.updateEmployment.incomeSame.description", employerName, TaxYearRangeUtil.currentTaxYearRange))
    }

    "display return to employment details link" in {
      doc must haveLinkElement(
        "returnToEmploymentDetails",
        url,
        messages("tai.updateEmployment.incomeSame.employment.return.link")
      )
    }
  }
}
