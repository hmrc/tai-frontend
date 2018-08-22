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
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodePairs, TaxCodeRecord}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.taxCodeChange.TaxCodeChangeViewModel
import uk.gov.hmrc.time.TaxYearResolver

class TaxCodeComparisonViewSpec extends TaiViewSpec {

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("A1111", startDate, startDate.plusMonths(1),"Employer 1", false, "1234", true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)
  val taxCodeRecord3 = taxCodeRecord1.copy(taxCode = "B175", startDate = startDate.plusDays(3), endDate = TaxYearResolver.endOfCurrentTaxYear, pensionIndicator = false)
  val taxCodeChange: TaxCodeChange = TaxCodeChange(Seq(taxCodeRecord1, taxCodeRecord3), Seq(taxCodeRecord2, taxCodeRecord3))
  val viewModel: TaxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange)

  override def view = views.html.taxCodeChange.taxCodeComparison(viewModel)

  "tax code comparison" should {
    behave like pageWithBackLink

    behave like pageWithTitle(Messages("taxCode.change.journey.preHeading"))

    behave like pageWithCombinedHeader(
      preHeaderText = Messages("taxCode.change.journey.preHeading"),
      mainHeaderText = Messages("taxCode.change.yourTaxCodeChanged.h1", Dates.formatDate(viewModel.changeDate)))

    "display the correct paragraphs" in {
      doc(view) must haveParagraphWithText(Messages("taxCode.change.yourTaxCodeChanged.paragraph"))
    }

    "display the previous tax codes" in {
      // TODO: Move foreach to helper method
      taxCodeChange.previous.foreach(record => {
        doc(view) must haveHeadingH2WithText(record.employerName)
//        doc(view) must haveParagraphWithText(Messages("tai.incomeTaxSummary.payrollNumber.prefix", record.payrollNumber))
        doc(view) must haveHeadingH3WithText(Messages("tai.taxCode.title.pt2", Dates.formatDate(record.startDate), Dates.formatDate(record.endDate)))
        doc(view).toString must include(record.taxCode)
      })
    }

    "display the current tax codes" in {
      // TODO: Move foreach to helper method
      taxCodeChange.current.foreach(record => {
        doc(view) must haveHeadingH2WithText(record.employerName)
//        doc(view) must haveParagraphWithText(Messages("tai.incomeTaxSummary.payrollNumber.prefix", record.payrollNumber))
        doc(view) must haveHeadingH3WithText(Messages("tai.taxCode.title.pt2", Dates.formatDate(record.startDate), Dates.formatDate(record.endDate)))
        doc(view).toString must include(record.taxCode)
      })
    }

    "display a button linking to the 'check your tax-free amount page" in {
      doc(view) must haveLinkElement("check-your-tax-button", routes.TaxCodeChangeController.yourTaxFreeAmount().url.toString, Messages("taxCode.change.yourTaxCodeChanged.checkYourTaxButton"))
    }
  }
}
