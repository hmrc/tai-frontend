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

package views.html.print

import controllers.routes
import org.joda.time.LocalDate
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.util.CeasedEmploymentHelper
import uk.gov.hmrc.tai.util.TaiConstants._
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.YourIncomeCalculationViewModelNew
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.urls.Link

class yourIncomeCalculationNewSpec extends TaiViewSpec {

  "YourIncomeCalculationView" must {

    behave like pageWithBackLink
    behave like haveBackButtonWithUrl(controllers.routes.YourIncomeCalculationControllerNew.yourIncomeCalculationPage(model.empId).url)


    "show details for potentially ceased employment" when {
      "payments are empty" in {
        val model = incomeCalculationViewModel(
          payments = Seq.empty[Payment],
          employmentStatus = PotentiallyCeased)
        def potentiallyCeasedView = views.html.incomes.yourIncomeCalculationNew(model)

        doc(potentiallyCeasedView) must haveH2HeadingWithText(
          messages("tai.income.calculation.heading",s"${TaxYearResolver.startOfCurrentTaxYear.toString(dateFormatPattern)}",
            s"${TaxYearResolver.endOfCurrentTaxYear.toString(dateFormatPattern)}")
        )
      }

      "payments are present" in {
        val model = incomeCalculationViewModel(employmentStatus = PotentiallyCeased)
        def potentiallyCeasedView = views.html.incomes.yourIncomeCalculationNew(model)

        doc(potentiallyCeasedView) must haveH2HeadingWithText(
          messages("tai.income.calculation.heading.withRti", model.latestPayment.get.date.toString(dateFormatPattern))
        )
        doc(potentiallyCeasedView) must haveParagraphWithText(messages("tai.income.calculation.potentially.ceased.lede"))
      }
    }

    "show details for ceased employment" when {
      "payments are empty" in {
        val model = incomeCalculationViewModel(
          payments = Seq.empty[Payment],
          employmentStatus = Ceased)
        def ceasedView = views.html.incomes.yourIncomeCalculationNew(model)

        doc(ceasedView) must haveH2HeadingWithText(
          messages("tai.income.calculation.heading",s"${TaxYearResolver.startOfCurrentTaxYear.toString(dateFormatPattern)}",
            s"${TaxYearResolver.endOfCurrentTaxYear.toString(dateFormatPattern)}")
        )
      }

      "payments are present" in {
        val model = incomeCalculationViewModel(employmentStatus = Ceased)
        def ceasedView = views.html.incomes.yourIncomeCalculationNew(model)

        doc(ceasedView) must haveH2HeadingWithText(
          messages("tai.income.calculation.ceased.heading", model.latestPayment.get.date.toString(dateFormatPattern))
        )
        doc(ceasedView) must haveParagraphWithText(messages("tai.income.calculation.rti.ceased.emp",
          s"${CeasedEmploymentHelper.toDisplayFormat(model.endDate)}"))
      }
    }


    "show details for live employment" when {
      "payments are empty" in {
        val model = incomeCalculationViewModel(payments = Seq.empty[Payment])
        def liveView = views.html.incomes.yourIncomeCalculationNew(model)
        doc(liveView) must haveH2HeadingWithText(
          messages("tai.income.calculation.heading",s"${TaxYearResolver.startOfCurrentTaxYear.toString(dateFormatPattern)}",
            s"${TaxYearResolver.endOfCurrentTaxYear.toString(dateFormatPattern)}")
        )
      }

      "payments are present" in {
        val model = incomeCalculationViewModel()
        def liveView = views.html.incomes.yourIncomeCalculationNew(model)
        doc(liveView) must haveH2HeadingWithText(
          messages("tai.income.calculation.heading.withRti", model.latestPayment.get.date.toString(dateFormatPattern))
        )
      }

      "rti is down" in {
        val model = incomeCalculationViewModel(realTimeStatus = TemporarilyUnavailable)
        def liveView = views.html.incomes.yourIncomeCalculationNew(model)
        doc(liveView) must haveParagraphWithText(messages("tai.income.calculation.rtiUnavailableCurrentYear.message"))
        doc(liveView) must haveParagraphWithText(messages("tai.income.calculation.rtiUnavailableCurrentYear.message.contact"))
      }

    }

    "show payment details" in {
      doc(view) must haveThWithText(messages("tai.income.calculation.incomeTable.dateHeader"))
      doc(view) must haveThWithText(messages("tai.income.calculation.incomeTable.print.incomeHeader"))
      doc(view) must haveThWithText(messages("tai.income.calculation.incomeTable.print.taxPaidHeader"))
      doc(view) must haveThWithText(messages("tai.income.calculation.incomeTable.print.nationalInsuranceHeader"))

      doc(view) must haveTdWithText(messages("tai.taxFree.total"))
      doc(view) must haveTdWithText(messages("£ " + f"${model.latestPayment.get.amountYearToDate}%,.2f"))
      doc(view) must haveTdWithText(messages("£ " + f"${model.latestPayment.get.taxAmountYearToDate}%,.2f"))
      doc(view) must haveTdWithText(messages("£ " + f"${model.latestPayment.get.nationalInsuranceAmountYearToDate}%,.2f"))

      model.payments.foreach { payment =>
        doc(view) must haveTdWithText(payment.date.toString("d MMM yyyy"))
        doc(view) must haveTdWithText("£ " + f"${payment.taxableIncome}%,.2f")
        doc(view) must haveTdWithText("£ " + f"${payment.taxAmount}%,.2f")
        doc(view) must haveTdWithText("£ " + f"${payment.nationalInsuranceAmount}%,.2f")
      }

    }


  }



  lazy val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  lazy val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  lazy val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  lazy val latestPayment = Payment(new LocalDate().minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)
  lazy val dateFormatPattern = "d MMMM yyyy"

  private def incomeCalculationViewModel(realTimeStatus: RealTimeStatus = Available,
                                         payments: Seq[Payment] = Seq(latestPayment, secondPayment, thirdPayment, firstPayment),
                                         employmentStatus: TaxCodeIncomeSourceStatus = Live,
                                         employmentType: TaxCodeIncomeComponentType = EmploymentIncome) = {
    val annualAccount = AnnualAccount("KEY", uk.gov.hmrc.tai.model.tai.TaxYear(), realTimeStatus, payments, Nil)
    val employment = Employment("test employment", Some("EMPLOYER1"), LocalDate.now(),
      if(employmentStatus == Ceased) Some(LocalDate.parse("2017-08-08")) else None, Seq(annualAccount), "", "", 2)
    val taxCodeIncome = TaxCodeIncome(employmentType, Some(2), 1111, "employment2", "150L", "EMPLOYER1", Week1Month1BasisOperation, employmentStatus)
    YourIncomeCalculationViewModelNew(taxCodeIncome, employment)
  }

  lazy val model = incomeCalculationViewModel()
  override def view: Html = views.html.print.yourIncomeCalculationNew(model)
}
