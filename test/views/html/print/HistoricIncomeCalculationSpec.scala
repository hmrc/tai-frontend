/*
 * Copyright 2024 HM Revenue & Customs
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

package views.html.print

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.HistoricIncomeCalculationViewModel

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoricIncomeCalculationSpec extends TaiViewSpec {

  private val historicIncomePrintView = inject[HistoricIncomePrintView]

  val dateFormatPattern = "d MMMM yyyy"

  val samplePaymentWithoutNic: Payment = Payment(
    date = LocalDate.of(2016, 4, 7),
    amount = 111,
    amountYearToDate = 150,
    taxAmount = 0,
    taxAmountYearToDate = 0,
    nationalInsuranceAmount = 0,
    nationalInsuranceAmountYearToDate = 0,
    payFrequency = Monthly
  )
  val samplePaymentWithNic: Payment = Payment(
    date = LocalDate.of(2017, 4, 7),
    amount = 222,
    amountYearToDate = 150,
    taxAmount = 0,
    taxAmountYearToDate = 0,
    nationalInsuranceAmount = 100,
    nationalInsuranceAmountYearToDate = 200,
    payFrequency = Annually
  )
  val samplePayments: Seq[Payment] = Seq(samplePaymentWithoutNic, samplePaymentWithNic)

  def createHistoricIncomeCalculationVM(
    payments: Seq[Payment],
    eyuMessage: Seq[String],
    realTimeStatus: RealTimeStatus,
    year: TaxYear
  ): HistoricIncomeCalculationViewModel =
    HistoricIncomeCalculationViewModel(
      employerName = Some("Foo"),
      employmentId = 1,
      payments = payments,
      endOfTaxYearUpdateMessages = eyuMessage,
      realTimeStatus = realTimeStatus,
      year
    )

  private def customView(
    payments: Seq[Payment] = Nil,
    eyuMessage: Seq[String] = Nil,
    realTimeStatus: RealTimeStatus = Available,
    year: TaxYear = TaxYear().prev
  ): HtmlFormat.Appendable = {
    val historicIncomeCalculationVM: HistoricIncomeCalculationViewModel =
      createHistoricIncomeCalculationVM(payments, eyuMessage, realTimeStatus, year)
    historicIncomePrintView(historicIncomeCalculationVM)
  }

  val historicIncomeCalculationVM: HistoricIncomeCalculationViewModel =
    createHistoricIncomeCalculationVM(Nil, Nil, Unavailable, TaxYear().prev)
  override def view: Html = historicIncomePrintView(historicIncomeCalculationVM)

  "The previous year income calculation print page" should {

    behave like pageWithTitle(s"${messages("tai.yourIncome.heading")} - ${messages("tai.service.navTitle")} - GOV.UK")

    "have a small font heading" in {
      doc must haveHeadingH2WithText(messages("tai.income.calculation.TaxableIncomeDetails", "Foo"))
    }

    "have a back link" in {
      doc must haveElementAtPathWithId("a", "back-link")
    }

    "show the necessary print buttons" in {
      doc.getElementsByClass("print-button").toString must include("printLink")
    }

    "have informative text when payment data is not available" when {
      "there is no RTI data for previous year" in {
        val view: Html = customView(realTimeStatus = Unavailable, year = TaxYear().prev)
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveParagraphWithText(
          messages(
            "tai.income.calculation.noRtiDataPreviousYear",
            TaxYear(TaxYear().prev.year).end.format(DateTimeFormatter.ofPattern(dateFormatPattern))
          )
        )
      }

      "there is no RTI data for cy-2" in {
        val view: Html = customView(realTimeStatus = Unavailable, year = TaxYear(TaxYear().prev.year - 1))
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveParagraphWithText(
          messages(
            "tai.income.calculation.noRtiDataPreviousYear",
            TaxYear(TaxYear().prev.year - 1).end.format(DateTimeFormatter.ofPattern(dateFormatPattern))
          )
        )
      }

      "RTI is available but payment data is not available" in {
        val view: Html = customView(realTimeStatus = Available)
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveParagraphWithText(
          messages(
            "tai.income.calculation.noRtiDataPreviousYear",
            TaxYear(TaxYear().year - 1).end.format(DateTimeFormatter.ofPattern(dateFormatPattern))
          )
        )
      }
    }

    "display payment information" when {
      "payment information is available on previous year summary message" in {
        val view: Html = customView(payments = samplePayments)
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveParagraphWithText(
          messages(
            "tai.income.calculation.summary.previous",
            samplePayments.head.date.format(DateTimeFormatter.ofPattern(dateFormatPattern)),
            samplePayments.last.date.format(DateTimeFormatter.ofPattern(dateFormatPattern))
          )
        )
      }

      "payment information, employer name but no EYU messages are available" in {
        val view: Html = customView(payments = samplePayments)
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveParagraphWithText(messages("tai.income.calculation.previous", "Foo"))
      }

      "payment information, employer name and EYU messages are available" in {
        val view: Html = customView(payments = samplePayments, eyuMessage = Seq("EyuMessage1", "EyuMessag2"))
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveParagraphWithText(messages("tai.income.calculation.eyu.previous", "Foo"))
      }
    }

    "display RTI table" when {
      "payment information is available and should have valid table headings" in {
        val view: Html = customView(payments = samplePayments)
        val doc: Document = Jsoup.parse(view.toString)
        doc.getElementById("taxable-income-table").text must include(
          messages("tai.income.calculation.incomeTable.dateHeader")
        )
        doc.getElementById("taxable-income-table").text must include(
          messages("tai.income.calculation.incomeTable.print.incomeHeader")
        )
        doc.getElementById("taxable-income-table").text must include(
          messages("tai.income.calculation.incomeTable.print.taxPaidHeader")
        )
        doc.getElementById("taxable-income-table").text must include(
          messages("tai.income.calculation.incomeTable.print.nationalInsuranceHeader")
        )
      }

      "payment information is available and should have valid table footers with no NIC paid" in {
        val view: Html = customView(payments = samplePayments)
        val doc: Document = Jsoup.parse(view.toString)
        doc.getElementById("taxable-income-table").text must include(messages("tai.taxFree.total"))
        doc.getElementById("taxable-income-table").text must include(f"${samplePayments.last.amountYearToDate}%,.2f")
        doc.getElementById("taxable-income-table").text must include(f"${samplePayments.last.taxAmountYearToDate}%,.2f")
      }

      "payment information is available and should have valid table footers with NIC paid" in {
        val view: Html = customView(payments = samplePayments)
        val doc: Document = Jsoup.parse(view.toString)
        doc.getElementById("taxable-income-table").text must include(messages("tai.taxFree.total"))
        doc.getElementById("taxable-income-table").text must include(
          f"${samplePaymentWithNic.nationalInsuranceAmountYearToDate}%,.2f"
        )
      }

      "payment information is available and should have valid content in table with no NIC paid" in {
        val view: Html = customView(payments = samplePayments)
        val doc: Document = Jsoup.parse(view.toString)
        doc.getElementById("taxable-income-table").text must include(
          TaxYearRangeUtil.formatDateAbbrMonth(samplePaymentWithoutNic.date)
        )
        doc.getElementById("taxable-income-table").text must include(f"${samplePaymentWithoutNic.amount}%,.2f")
        doc.getElementById("taxable-income-table").text must include(f"${samplePaymentWithoutNic.taxAmount}%,.2f")
      }

      "payment information is available and should have valid content in table without NIC paid" in {
        val view: Html = customView(payments = samplePayments)
        val doc: Document = Jsoup.parse(view.toString)
        doc.getElementById("taxable-income-table").text must include(
          TaxYearRangeUtil.formatDateAbbrMonth(samplePaymentWithNic.date)
        )
        doc.getElementById("taxable-income-table").text must include(f"${samplePaymentWithNic.amount}%,.2f")
        doc.getElementById("taxable-income-table").text must include(f"${samplePaymentWithNic.taxAmount}%,.2f")
        doc.getElementById("taxable-income-table").text must include(
          f"${samplePaymentWithNic.nationalInsuranceAmount}%,.2f"
        )
      }
    }

    "display rti eyu messages" when {
      "there is only one eyu message" in {
        val view: Html = customView(payments = samplePayments, eyuMessage = Seq("EyuMessage1"))
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveParagraphWithText(messages("tai.income.calculation.eyu.summary.single", "Foo"))
        doc must haveParagraphWithText("EyuMessage1")
      }

      "there are multiple eyu messages" in {
        val view: Html = customView(payments = samplePayments, eyuMessage = Seq("EyuMessage1", "EyuMessage2"))
        val doc: Document = Jsoup.parse(view.toString)
        doc must haveParagraphWithText(messages("tai.income.calculation.eyu.summary.multi", "Foo"))
        doc must haveBulletPointWithText("EyuMessage1")
        doc must haveBulletPointWithText("EyuMessage2")
      }
    }

    "contain the user's name" in {
      doc must haveStrongWithText("Firstname Surname")
    }
  }

}
