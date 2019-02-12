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

package views.html.taxCodeChange

import controllers.routes
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.taxCodeChange.TaxCodeChangeViewModel


class TaxCodeComparisonViewSpec extends TaiViewSpec {

  val startDate = TaxYear().start
  val taxCodeRecord1 = TaxCodeRecord("1185L", startDate, startDate.plusMonths(1), OtherBasisOfOperation, "Employer 1", true, Some("1234"), true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYear().end, payrollNumber = None)
  val taxCodeRecord3 = taxCodeRecord1.copy(taxCode = "BR", startDate = startDate.plusDays(3), endDate = TaxYear().end, pensionIndicator = false, payrollNumber = Some("Payroll Number"))
  val taxCodeChange: TaxCodeChange = TaxCodeChange(Seq(taxCodeRecord1, taxCodeRecord3), Seq(taxCodeRecord2, taxCodeRecord3))
  val viewModel: TaxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange, Map[String, BigDecimal]())

  override def view = views.html.taxCodeChange.taxCodeComparison(viewModel)

  def testTaxCodeRecordFormat(record: TaxCodeRecord) = {
    doc must haveHeadingH3WithText(record.employerName)
    doc must haveClassWithText(Messages("taxCode.change.yourTaxCodeChanged.from", Dates.formatDate(record.startDate)), "tax-code-change__date")
    doc(view).toString must include(record.taxCode)

    doc must haveSpanWithText(Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", record.taxCode))

    for (explanation <- TaxCodeChangeViewModel.getTaxCodeExplanations(record, Map[String, BigDecimal](), "current").descriptionItems) {
      doc must haveSpanWithText(Messages("taxCode.change.yourTaxCodeChanged.understand", record.taxCode))
      doc must haveClassWithText(explanation._1, "tax-code-change__part")

      doc must haveClassWithText(explanation._2, "tax-code-change__part-definition")
      doc must haveClassWithText(Messages("tai.taxCode.part.announce", explanation._1), "visuallyhidden")
      doc must haveClassWithText(Messages("tai.taxCode.definition.announce"), "visuallyhidden")
    }
  }

  "tax code comparison" should {
    behave like pageWithBackLink

    behave like pageWithTitle(Messages("taxCode.change.journey.preHeading"))

    behave like pageWithCombinedHeader(
      preHeaderText = Messages("taxCode.change.journey.preHeading"),
      mainHeaderText = Messages("taxCode.change.yourTaxCodeChanged.h1", Dates.formatDate(viewModel.changeDate)))


    "displays the previous tax code section title" in {
      doc must haveHeadingH2WithText (Messages("taxCode.change.yourTaxCodeChanged.previousTaxCodes"))
    }

    "displays the current tax code section title" in {
      doc must haveHeadingH2WithText (Messages("taxCode.change.yourTaxCodeChanged.currentTaxCodes"))
    }

    "display the previous tax codes" in {
      taxCodeChange.previous.foreach(testTaxCodeRecordFormat)
    }

    "display the current tax codes" in {
      taxCodeChange.current.foreach(testTaxCodeRecordFormat)
    }

    "display a button linking to the 'check your tax-free amount page" in {
      doc must haveLinkElement("" +
        "check-your-tax-button",
        routes.TaxCodeChangeController.yourTaxFreeAmount().url.toString,
        Messages("taxCode.change.yourTaxCodeChanged.checkYourTaxButton")
      )
    }

    "display the pension number" when {
      "a pension" in {
        val expectedText = Messages("tai.pensionNumber") + ": 1234 " + Messages("tai.pension.income.details.pensionNumber.screenReader", "1234")
        doc must haveClassWithText(expectedText, "tax-code-change__payroll")
      }
    }

    "display the employment number" when {
      "a employment" in {
        val expectedText = Messages("tai.payRollNumber") + ": Payroll Number " + Messages("tai.employment.income.details.payrollNumber.screenReader", "Payroll Number")
        doc must haveClassWithText(expectedText, "tax-code-change__payroll")
      }
    }

    "does not display payroll or pension number when the payrollNumber does not exist" in {
      doc must haveClassCount("tax-code-change__payroll", 4)
      doc must haveClassCount("tax-code-change__payroll-number", 3)
    }

    "display tax code change reasons" when {
      def createTaxCodeRecord(employerName: String): TaxCodeRecord = {
        TaxCodeRecord("1185L", startDate, startDate.plusMonths(1), OtherBasisOfOperation, employerName, true, Some("1234"), true)
      }

      "primary employments have changed" in {
        val previous = createTaxCodeRecord("Employer 1")
        val current = createTaxCodeRecord("Employer 2")
        val taxCodeChange: TaxCodeChange = TaxCodeChange(Seq(previous), Seq(current))
        val viewModel: TaxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange, Map[String, BigDecimal]())

        val view = views.html.taxCodeChange.taxCodeComparison(viewModel)
        doc(view) must haveClassCount("tax-code-reason", 2)
      }

      "display a generic tax code reason" when {
        "there are more than 4 reasons" in {
          val fivePreviousJobs: Seq[TaxCodeRecord] = Seq.tabulate(5)(i => createTaxCodeRecord(s"Employer+$i"))
          val current = createTaxCodeRecord("a new job")
          val taxCodeChange: TaxCodeChange = TaxCodeChange(fivePreviousJobs, Seq(current))
          val viewModel: TaxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange, Map[String, BigDecimal]())

          val view = views.html.taxCodeChange.taxCodeComparison(viewModel)
          println(view.toString())
          doc(view) must haveClassCount("tax-code-reason", 1)
        }
      }
    }
  }
}
