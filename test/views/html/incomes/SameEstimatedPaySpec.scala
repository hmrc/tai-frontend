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

import org.joda.time.LocalDate
import controllers.routes
import org.scalatest.mockito.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.SameEstimatedPayViewModel

class SameEstimatedPaySpec extends TaiViewSpec with MockitoSugar{


  val employerName = "Employer"
  val amount = "£20,000"

  "Same estimated pay page" must{
    behave like pageWithBackLink
    behave like pageWithTitle(messages("tai.updateEmployment.incomeSame.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
    behave like pageWithHeader(messages("tai.updateEmployemnt.incomeSame.heading", TaxYearRangeUtil.currentTaxYearRangeSingleLine))

    "display the new estimated income" in {
      val newEstimateMessage = messages("tai.updateEmployment.incomeSame.newEstimate.text")
      doc must haveParagraphWithText(messages(s"$newEstimateMessage $amount"))
    }

    "display a paragraph" in {

      doc must haveParagraphWithText(messages("tai.updateEmployment.incomeSame.description", employerName,TaxYearRangeUtil.currentTaxYearRangeSingleLine))
    }

    "display return to employment details link" in {
      doc must haveLinkElement("returnToEmploymentDetails",
        routes.TaxAccountSummaryController.onPageLoad().url,
        messages("tai.updateEmployment.incomeSame.employment.return.link"))
    }
  }

  override def view: Html = views.html.incomes.sameEstimatedPay(createViewModel())

  def createViewModel(employmentStartDate:Option[LocalDate] = None) = {

    val employerName = "Employer"
    val amount = 20000
    SameEstimatedPayViewModel(employerName,amount, false)
  }
}
