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
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.util.DateHelper
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{LatestPayment, PaymentDetailsViewModel, YourIncomeCalculationViewModel}
import views.html.includes.link

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class YourIncomeCalculationViewSpec extends TaiViewSpec {

  "YourIncomeCalculationView" must {

    behave like pageWithTitle(messages("tai.income.calculation.TaxableIncomeDetails", model.employerName))
    behave like haveHeadingWithText(messages("tai.income.calculation.TaxableIncomeDetails", model.employerName))
    behave like pageWithBackLink()

    "show details for potentially ceased employment" when {
      "payments are empty" in {
        val model = incomeCalculationViewModel(
          payments = Seq.empty[PaymentDetailsViewModel],
          employmentStatus = PotentiallyCeased
        )

        def potentiallyCeasedView = template(model)

        doc(potentiallyCeasedView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading",
            s"${TaxYear().start.format(DateTimeFormatter.ofPattern(dateFormatPattern))}",
            s"${TaxYear().end.format(DateTimeFormatter.ofPattern(dateFormatPattern))}"
          )
        )
        doc(potentiallyCeasedView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading.employerInfo",
            s"${model.employerName}"
          )
        )
      }

      "payments are present" in {
        val model = incomeCalculationViewModel(employmentStatus = PotentiallyCeased)

        def potentiallyCeasedView = template(model)

        doc(potentiallyCeasedView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading.withRti",
            model.latestPayment.get.date.format(DateTimeFormatter.ofPattern(dateFormatPattern))
          )
        )
        doc(potentiallyCeasedView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading.employerInfo",
            s"${model.employerName}"
          )
        )
        doc(potentiallyCeasedView) must haveParagraphWithText(
          messages("tai.income.calculation.potentially.ceased.lede")
        )
      }
    }

    "show details for ceased employment" when {
      "payments are empty" in {
        val model = incomeCalculationViewModel(payments = Seq.empty[PaymentDetailsViewModel], employmentStatus = Ceased)

        def ceasedView = template(model)

        doc(ceasedView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading",
            s"${TaxYear().start.format(DateTimeFormatter.ofPattern(dateFormatPattern))}",
            s"${TaxYear().end.format(DateTimeFormatter.ofPattern(dateFormatPattern))}"
          )
        )
        doc(ceasedView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading.employerInfo",
            s"${model.employerName}"
          )
        )
      }

      "payments are present" in {
        val model = incomeCalculationViewModel(employmentStatus = Ceased)

        def ceasedView = template(model)

        doc(ceasedView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.ceased.heading",
            model.latestPayment.get.date.format(DateTimeFormatter.ofPattern(dateFormatPattern))
          )
        )
        doc(ceasedView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading.employerInfo",
            s"${model.employerName}"
          )
        )
        doc(ceasedView) must haveParagraphWithText(
          messages("tai.income.calculation.rti.ceased.emp", s"${DateHelper.toDisplayFormat(model.endDate)}")
        )
      }
    }

    "show details for live employment" when {
      "payments are empty" in {
        val model = incomeCalculationViewModel(payments = Seq.empty[PaymentDetailsViewModel])

        def liveView = template(model)

        doc(liveView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading",
            s"${TaxYear().start.format(DateTimeFormatter.ofPattern(dateFormatPattern))}",
            s"${TaxYear().end.format(DateTimeFormatter.ofPattern(dateFormatPattern))}"
          )
        )
        doc(liveView) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading.employerInfo",
            s"${model.employerName}"
          )
        )
      }

      "payments are present" in {
        doc(view) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading.withRti",
            model.latestPayment.get.date.format(DateTimeFormatter.ofPattern(dateFormatPattern))
          )
        )
        doc(view) must haveParagraphWithText(
          messages(
            "tai.income.calculation.heading.employerInfo",
            s"${model.employerName}"
          )
        )
      }

      "employment type is pension" in {
        val model = incomeCalculationViewModel(employmentType = PensionIncome)

        def liveView = template(model)

        doc(liveView).select("#pensionUpdateLink").html().replaceAll("\\s+", "") mustBe Html(
          messages(
            "tai.income.calculation.update.pension",
            link(
              url = controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
                .onPageLoad(model.empId)
                .url,
              copy = messages("tai.income.calculation.updateLink.regular")
            )
          )
        ).body.replaceAll("\\s+", "")
      }

      "employment type is Employment Income" in {
        doc(view).select("#regularUpdateLink").html().replaceAll("\\s+", "") mustBe Html(
          messages(
            "tai.income.calculation.update.regular",
            link(
              url = controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
                .onPageLoad(model.empId)
                .url,
              copy = messages("tai.income.calculation.updateLink.regular")
            )
          )
        ).body.replaceAll("\\s+", "")
      }
    }

    "show payment details" in {
      doc(view) must haveThWithText(messages("tai.income.calculation.incomeTable.dateHeader"))
      doc(view) must haveThWithText(messages("tai.income.calculation.incomeTable.incomeHeader"))
      doc(view) must haveThWithText(messages("tai.income.calculation.incomeTable.taxPaidHeader"))
      doc(view) must haveThWithText(messages("tai.income.calculation.incomeTable.nationalInsuranceHeader"))

      doc(view) must haveTdWithText(messages("tai.taxFree.total"))
      doc(view) must haveTdWithText(messages(f"${model.latestPayment.get.amountYearToDate}%,.2f"))
      doc(view) must haveTdWithText(messages(f"${model.latestPayment.get.taxAmountYearToDate}%,.2f"))
      doc(view) must haveTdWithText(messages(f"${model.latestPayment.get.nationalInsuranceAmountYearToDate}%,.2f"))

      model.payments.foreach { payment =>
        doc(view) must haveTdWithText(payment.date.format(DateTimeFormatter.ofPattern(dateFormatPattern)))
        doc(view) must haveTdWithText(f"${payment.taxableIncome}%,.2f")
        doc(view) must haveTdWithText(f"${payment.taxAmount}%,.2f")
        doc(view) must haveTdWithText(f"${payment.nationalInsuranceAmount}%,.2f")
      }
    }

    "show total not equal message" when {
      "total not equal message is present" in {
        val model             = incomeCalculationViewModel(totalNotEqualMessage = Some("Test"))
        def totalNotEqualView = template(model)

        doc(totalNotEqualView) must haveParagraphWithText(model.messageWhenTotalNotEqual.get)
        doc(totalNotEqualView) must haveParagraphWithText(messages("tai.income.calculation.totalNotMatching.message"))
      }
    }

    "show income calculation estimate message" when {

      "employment is live" in {
        val model = incomeCalculationViewModel(
          incomeCalculationMessage = "TEST",
          incomeCalculationEstimateMessage = Some("ESTIMATE")
        )

        def incomeMessagesView = template(model)

        doc(incomeMessagesView) must haveHeadingH2WithText(model.incomeCalculationEstimateMessage.get)
      }

      "employment is ceased" in {
        val model = incomeCalculationViewModel(
          employmentStatus = Ceased,
          incomeCalculationMessage = "TEST",
          incomeCalculationEstimateMessage = Some("ESTIMATE")
        )

        def incomeMessagesView = template(model)

        doc(incomeMessagesView) must haveHeadingH3WithText(model.incomeCalculationEstimateMessage.get)
      }

      "employment is potentially ceased" in {
        val model = incomeCalculationViewModel(
          employmentStatus = PotentiallyCeased,
          incomeCalculationMessage = "TEST",
          incomeCalculationEstimateMessage = Some("ESTIMATE")
        )

        def incomeMessagesView = template(model)

        doc(incomeMessagesView) must haveHeadingH3WithText(model.incomeCalculationEstimateMessage.get)
      }
    }

    "show payroll messages" when {
      "hasPayrolled benefit is true" in {
        val model = incomeCalculationViewModel(hasPayrolledBenefit = true)

        def payrolledView = template(model)

        doc(payrolledView) must haveParagraphWithText(messages("tai.income.calculation.payrollingBik.message1"))
        doc(payrolledView) must haveParagraphWithText(messages("tai.income.calculation.payrollingBik.message2"))
      }
    }

  }

  lazy val defaultPayments   = Seq(
    PaymentDetailsViewModel(LocalDate.now.minusWeeks(1), 100, 50, 25),
    PaymentDetailsViewModel(LocalDate.now.minusWeeks(2), 100, 50, 25),
    PaymentDetailsViewModel(LocalDate.now.minusWeeks(3), 100, 50, 25),
    PaymentDetailsViewModel(LocalDate.now.minusWeeks(4), 100, 50, 25)
  )
  lazy val dateFormatPattern = "d MMMM yyyy"
  lazy val model             = incomeCalculationViewModel()

  private val template = inject[YourIncomeCalculationView]

  override def view: Html = template(model)

  private def incomeCalculationViewModel(
    payments: Seq[PaymentDetailsViewModel] = defaultPayments,
    employmentStatus: TaxCodeIncomeSourceStatus = Live,
    employmentType: TaxCodeIncomeComponentType = EmploymentIncome,
    totalNotEqualMessage: Option[String] = Some(""),
    incomeCalculationMessage: String = "",
    incomeCalculationEstimateMessage: Option[String] = None,
    hasPayrolledBenefit: Boolean = false
  ) = {

    val latestPayment =
      if (payments.isEmpty) None else Some(LatestPayment(LocalDate.now.minusWeeks(4), 400, 50, 25, Weekly))
    YourIncomeCalculationViewModel(
      2,
      "test employment",
      payments,
      employmentStatus,
      latestPayment,
      if (employmentStatus == Ceased) Some(LocalDate.parse("2017-08-08")) else None,
      employmentType == PensionIncome,
      totalNotEqualMessage,
      incomeCalculationMessage,
      incomeCalculationEstimateMessage,
      hasPayrolledBenefit,
      "test user"
    )
  }
}
