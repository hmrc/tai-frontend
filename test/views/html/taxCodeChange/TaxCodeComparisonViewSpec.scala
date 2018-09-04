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
import org.fluentlenium.core.filter.matcher.ContainsWordMatcher
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOperation
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.taxCodeChange.TaxCodeChangeViewModel
import uk.gov.hmrc.time.TaxYearResolver

class TaxCodeComparisonViewSpec extends TaiViewSpec {

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("1185L", startDate, startDate.plusMonths(1), OtherBasisOperation, "Employer 1", true, Some("1234"), true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)
  val taxCodeRecord3 = taxCodeRecord1.copy(taxCode = "BR", startDate = startDate.plusDays(3), endDate = TaxYearResolver.endOfCurrentTaxYear, pensionIndicator = false, payrollNumber = Some("Payroll Number"))
  val taxCodeChange: TaxCodeChange = TaxCodeChange(Seq(taxCodeRecord1, taxCodeRecord3), Seq(taxCodeRecord2, taxCodeRecord3))
  val viewModel: TaxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange, Map[String, BigDecimal]())

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

    "displays the previous tax code section title" in {
      doc(view) must haveHeadingH2WithText (Messages("taxCode.change.yourTaxCodeChanged.previousTaxCodes"))
    }

    "displays the current tax code section title" in {
      doc(view) must haveHeadingH2WithText (Messages("taxCode.change.yourTaxCodeChanged.currentTaxCodes"))
    }

    "display the previous tax codes" in {
      taxCodeChange.previous.foreach(record => {
        doc(view) must haveHeadingH3WithText(record.employerName)
        doc(view) must haveClassWithText(Messages("taxCode.change.yourTaxCodeChanged.from", Dates.formatDate(record.startDate)), "tax-code-change__date")
        doc(view).toString must include(record.taxCode)

        doc(view) must haveSpanWithText(Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", record.taxCode))

        for (explanation <- TaxCodeChangeViewModel.getTaxCodeExplanations(record, Map[String, BigDecimal]()).descriptionItems) {
          doc(view) must haveSpanWithText(Messages("taxCode.change.yourTaxCodeChanged.understand", record.taxCode))
          doc(view) must haveSpanWithText(explanation._1)
          doc(view) must haveClassWithText(Messages("tai.taxCode.definition.announce") + " " + {explanation._2}, "fixed-small")
        }
      })
    }

    "display the current tax codes" in {
      taxCodeChange.current.foreach(record => {
        doc(view) must haveHeadingH3WithText(record.employerName)
        doc(view) must haveClassWithText(Messages("taxCode.change.yourTaxCodeChanged.from", Dates.formatDate(record.startDate)), "tax-code-change__date")
        doc(view).toString must include(record.taxCode)

        doc(view) must haveSpanWithText(Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", record.taxCode))

        for (explanation <- TaxCodeChangeViewModel.getTaxCodeExplanations(record, Map[String, BigDecimal]()).descriptionItems) {
          doc(view) must haveSpanWithText(Messages("taxCode.change.yourTaxCodeChanged.understand", record.taxCode))
          doc(view) must haveSpanWithText(explanation._1)
          doc(view) must haveClassWithText(Messages("tai.taxCode.definition.announce") + " " + {explanation._2}, "fixed-small")
        }
      })
    }

    "display a button linking to the 'check your tax-free amount page" in {
      doc(view) must haveLinkElement("check-your-tax-button", routes.TaxCodeChangeController.yourTaxFreeAmount().url.toString, Messages("taxCode.change.yourTaxCodeChanged.checkYourTaxButton"))
    }

    "display the pension number" when {
      "a pension" in {
        val expectedText = Messages("tai.pensionNumber") + ": 1234 " + Messages("tai.pension.income.details.pensionNumber.screenReader", "1234")
        doc(view) must haveClassWithText(expectedText, "tax-code-change__payroll")
      }
    }

    "display the employment number" when {
      "a employment" in {
        val expectedText = Messages("tai.payRollNumber") + ": Payroll Number " + Messages("tai.employment.income.details.payrollNumber.screenReader", "Payroll Number")
        doc(view) must haveClassWithText(expectedText, "tax-code-change__payroll")
      }
    }

    "does not display payroll or pension number when the payrollNumber does not exist" in {

    }
  }
}
