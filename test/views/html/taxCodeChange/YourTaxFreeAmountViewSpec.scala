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
      doc(view) must haveParagraphWithText(Messages("taxCode.change.yourTaxFreeAmount.desc"))
    }

    "have h2 heading showing the date period for tax-free amount" in {
      val fromDate = new LocalDate()
      val toDate = TaxYearResolver.endOfCurrentTaxYear

      doc(viewP2Date) must haveH2HeadingWithText(Messages("taxCode.change.yourTaxFreeAmount.dates",
        ViewModelHelper.dynamicDateRangeHtmlNonBreak(fromDate, toDate)))

      def viewP2Date: Html = views.html.taxCodeChange.yourTaxFreeAmount(createViewModel(fromDate))
    }






  }

  private def createViewModel(p2IssuedDate:LocalDate = new LocalDate(), codingComponents:Seq[CodingComponent] =
                              Seq.empty[CodingComponent]):YourTaxFreeAmountViewModel = {

    YourTaxFreeAmountViewModel(p2IssuedDate,codingComponents)
  }

  override def view = views.html.taxCodeChange.yourTaxFreeAmount(createViewModel())
}
