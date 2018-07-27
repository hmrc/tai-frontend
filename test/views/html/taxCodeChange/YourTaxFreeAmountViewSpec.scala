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

package views.html.taxCodeChange

import controllers.routes
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel

class YourTaxFreeAmountViewSpec extends TaiViewSpec {

  "your tax free amount" should {
    behave like pageWithBackLink

    behave like pageWithTitle(Messages("taxCode.change.yourTaxFreeAmount.title"))

    behave like pageWithCombinedHeader(Messages("taxCode.change.journey.preHeading"), Messages("taxCode.change.yourTaxFreeAmount.title"))

    "have explanation of tax-free amount" in {
      doc must haveParagraphWithText(Messages("taxCode.change.yourTaxFreeAmount.desc"))
    }

    "have h2 heading showing the date period for tax-free amount" in {
      val fromDate = new LocalDate()
      val toDate = TaxYearResolver.endOfCurrentTaxYear
      val expectedDateRange = ViewModelHelper.dynamicDateRangeHtmlNonBreak(fromDate, toDate)
      val expectedMessage = Messages("taxCode.change.yourTaxFreeAmount.dates", expectedDateRange)

      doc(viewP2Date) must haveH2HeadingWithText(expectedMessage)

      def viewP2Date: Html = views.html.taxCodeChange.yourTaxFreeAmount(createViewModel(expectedDateRange))
    }


    "display figure for Your tax-free amount" in {
      val taxFreeAmount = "£11,500"

      doc(viewTaxFreeAmount) must haveSpanWithText(taxFreeAmount)

      def viewTaxFreeAmount: Html = views.html.taxCodeChange.yourTaxFreeAmount(createViewModel(annualTaxFreeAmount = taxFreeAmount))
    }

    "have h2 heading for how tax-free amount is calculated" in {

      doc must haveH2HeadingWithText(Messages("taxCode.change.yourTaxFreeAmount.summaryHeading"))

    }

//    "display a detail section" which {
//
//      "contains one group per summary category view model" in {
//        doc.select(".govuk-check-your-answers").size() mustBe 4
//        doc must haveElementWithId("summaryTable1")
//        doc must haveElementWithId("summaryTable2")
//        doc must haveElementWithId("summaryTable3")
//        doc must haveElementWithId("summaryTable4")
//      }
//    }

    "have a 'check what happens next' button" in {

      doc must haveLinkElement(
        "checkWhatHappensNext",
        routes.TaxCodeChangeController.whatHappensNext.url,
        messages("taxCode.change.yourTaxFreeAmount.whatHappensNext.link"))
    }

  }

  private def createViewModel(taxCodeDateRange: String = "", annualTaxFreeAmount: String = ""): YourTaxFreeAmountViewModel = {
    YourTaxFreeAmountViewModel(taxCodeDateRange, annualTaxFreeAmount)
  }

  override def view = views.html.taxCodeChange.yourTaxFreeAmount(createViewModel())
}
