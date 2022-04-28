/*
 * Copyright 2022 HM Revenue & Customs
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
import java.time.LocalDate
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.util.HtmlFormatter
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.taxCodeChange.TaxCodeChangeViewModel

class TaxCodeComparisonViewSpec extends TaiViewSpec {

  val startDate: LocalDate = TaxYear().start
  val taxCodeRecord1: TaxCodeRecord = TaxCodeRecord(
    "1185L",
    startDate,
    startDate.plusMonths(1),
    OtherBasisOfOperation,
    "Employer 1",
    pensionIndicator = true,
    Some("1234"),
    primary = true)
  val taxCodeRecord2: TaxCodeRecord =
    taxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYear().end, payrollNumber = None)
  val taxCodeRecord3: TaxCodeRecord = taxCodeRecord1.copy(
    taxCode = "BR",
    startDate = startDate.plusDays(3),
    endDate = TaxYear().end,
    pensionIndicator = false,
    payrollNumber = Some("Payroll Number"))
  val taxCodeChange: TaxCodeChange =
    TaxCodeChange(List(taxCodeRecord1, taxCodeRecord3), List(taxCodeRecord2, taxCodeRecord3))
  val viewModel: TaxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange, Map[String, BigDecimal]())

  private val taxCodeComparison = inject[TaxCodeComparisonView]
  override def view: HtmlFormat.Appendable = taxCodeComparison(viewModel, appConfig)

  def testTaxCodeRecordFormat(record: TaxCodeRecord): Unit = {
    doc must haveHeadingH3WithText(record.employerName)
    doc must haveClassWithText(
      Messages("taxCode.change.yourTaxCodeChanged.from", Dates.formatDate(record.startDate)),
      "tax-code-change__date")
    doc(view).toString must include(record.taxCode)

    doc must haveSpanWithText(Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", record.taxCode))

    for (explanation <- TaxCodeChangeViewModel
                         .getTaxCodeExplanations(record, Map[String, BigDecimal](), "current", appConfig)
                         .descriptionItems) {
      doc must haveSpanWithText(Messages("taxCode.change.yourTaxCodeChanged.understand", record.taxCode))
      doc must haveClassWithText(explanation._1, "tax-code-change__part")

      doc must haveClassWithText(explanation._2, "tax-code-change__part-definition")
      doc must haveClassWithText(Messages("tai.taxCode.part.announce", explanation._1), "govuk-visually-hidden")
      doc must haveClassWithText(Messages("tai.taxCode.definition.announce"), "govuk-visually-hidden")
    }
  }

  "tax code comparison" should {
    behave like pageWithBackLinkNew

    behave like pageWithTitle(Messages("taxCode.change.journey.preHeading"))

    behave like pageWithCombinedHeaderNewFormat(
      preHeaderText = Messages("taxCode.change.journey.preHeading"),
      mainHeaderText = Messages(
        "taxCode.change.yourTaxCodeChanged.h1",
        HtmlFormatter.htmlNonBroken(Dates.formatDate(viewModel.changeDate)))
    )

    "displays the previous tax code section title" in {
      doc must haveHeadingH2WithText(Messages("taxCode.change.yourTaxCodeChanged.previousTaxCodes"))
    }

    "displays the current tax code section title" in {
      doc must haveHeadingH2WithText(Messages("taxCode.change.yourTaxCodeChanged.currentTaxCodes"))
    }

    "display the previous tax codes" in {
      taxCodeChange.previous.foreach(testTaxCodeRecordFormat)
    }

    "display the current tax codes" in {
      taxCodeChange.current.foreach(testTaxCodeRecordFormat)
    }

    "display a button linking to the 'check your tax-free amount page" in {
      doc must haveLinkElement(
        "" +
          "check-your-tax-button",
        routes.TaxCodeChangeController.yourTaxFreeAmount().url,
        Messages("taxCode.change.yourTaxCodeChanged.checkYourTaxButton")
      )
    }

    "display the pension number" when {
      "a pension" in {
        val expectedText = Messages("tai.pensionNumber") + ": 1234 " + Messages(
          "tai.pension.income.details.pensionNumber.screenReader",
          "1234")
        doc must haveClassWithText(expectedText, "tax-code-change__payroll")
      }
    }

    "display the employment number" when {
      "a employment" in {
        val expectedText = Messages("tai.payRollNumber") + ": Payroll Number " + Messages(
          "tai.employment.income.details.payrollNumber.screenReader",
          "Payroll Number")
        doc must haveClassWithText(expectedText, "tax-code-change__payroll")
      }
    }

    "does not display payroll or pension number when the payrollNumber does not exist" in {
      doc must haveClassCount("tax-code-change__payroll", 4)
      doc must haveClassCount("tax-code-change__payroll-number", 3)
    }

    "display tax code change reasons" when {
      "primary employments have changed" in {
        val viewModel: TaxCodeChangeViewModel =
          TaxCodeChangeViewModel(taxCodeChange, Map.empty, Seq("a reason", "another reason"), isAGenericReason = false)

        val view: HtmlFormat.Appendable = taxCodeComparison(viewModel, appConfig)
        doc(view) must haveClassCount("tax-code-reason", 2)
      }

      "display a generic tax code reason" in {
        val viewModel: TaxCodeChangeViewModel =
          TaxCodeChangeViewModel(taxCodeChange, Map.empty, Seq("a reason", "another reason"))

        val view: HtmlFormat.Appendable = taxCodeComparison(viewModel, appConfig)
        doc(view) must haveClassCount("tax-code-reason", 1)
      }
    }
  }
}
