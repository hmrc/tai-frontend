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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain._

class YourIncomeCalculationViewModelNewSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Your Income Calculation View Model" must {
    "return employment details" when {
      "employment type is Employment Income" in {
        val model = incomeCalculationViewModel()

        model.empId mustBe 2
        model.employerName mustBe "test employment"
        model.payments mustBe Seq(
          PaymentDetailsViewModel(new LocalDate().minusWeeks(1), 100, 50, 25),
          PaymentDetailsViewModel(new LocalDate().minusWeeks(4), 100, 50, 25)
        )
        model.latestPayment mustBe Some(
          LatestPayment(new LocalDate().minusWeeks(1), 400, 50, 25, Irregular)
        )
        model.endDate mustBe None
        model.isPension mustBe false
        model.rtiStatus mustBe Available
        model.employmentStatus mustBe Live
      }

      "employment type is Pension Income" in {
        val model = incomeCalculationViewModel(employmentType = PensionIncome)
        model.isPension mustBe true
      }

      "tax code income is not present" in {
        val model = incomeCalculationViewModel(hasTaxCodeIncome = false)

        model.empId mustBe 2
        model.employerName mustBe "test employment"
        model.payments mustBe Seq(
          PaymentDetailsViewModel(new LocalDate().minusWeeks(1), 100, 50, 25),
          PaymentDetailsViewModel(new LocalDate().minusWeeks(4), 100, 50, 25)
        )
        model.latestPayment mustBe Some(
          LatestPayment(new LocalDate().minusWeeks(1), 400, 50, 25, Irregular)
        )
        model.endDate mustBe None
        model.isPension mustBe false
        model.rtiStatus mustBe Available
        model.employmentStatus mustBe Ceased
      }
    }

    "show message" when {
      "total is not equal for employment" in {
        val model = incomeCalculationViewModel()

        model.messageWhenTotalNotEqual mustBe Some(Messages("tai.income.calculation.totalNotMatching.emp.message"))
      }

      "total is not equal for pension" in {
        val model = incomeCalculationViewModel(employmentType = PensionIncome)

        model.messageWhenTotalNotEqual mustBe Some(Messages("tai.income.calculation.totalNotMatching.pension.message"))
      }
    }

    "doesn't show message" when {
      "total is equal" in {
        val model = incomeCalculationViewModel(payments = Seq(firstPayment))

        model.messageWhenTotalNotEqual mustBe None
      }
    }

    "show income calculation and estimation message" when {
      "tax code income is present and frequency is irregular" in {
        val model = incomeCalculationViewModel()

        model.incomeCalculationMessage mustBe ""
        model.incomeCalculationEstimateMessage mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", 1111))
      }

      "tax code income is present and frequency is monthly" in {
        val model = incomeCalculationViewModel(payments = Seq(firstPayment))

        model.incomeCalculationMessage mustBe Messages("tai.income.calculation.rti.midYear.weekly", LocalDate.now().toString("d MMMM yyyy"),
          firstPayment.date.toString("d MMMM yyyy"), MoneyPounds(firstPayment.amountYearToDate, 2).quantity)
        model.incomeCalculationEstimateMessage mustBe Some(Messages("tai.income.calculation.rti.emp.estimate", 1111))
      }

      "employment is ceased and end date and cessation pay is defined" in {
        val model = incomeCalculationViewModel(payments = Seq(firstPayment), cessationPay = Some(100), employmentStatus = Ceased)

        model.incomeCalculationMessage mustBe Messages("tai.income.calculation.rti.ceased.emp", model.endDate.get.toString("d MMMM yyyy"),
          firstPayment.date.toString("d MMMM yyyy"), MoneyPounds(firstPayment.amountYearToDate, 2).quantity)
        model.incomeCalculationEstimateMessage mustBe None
      }
    }

    "doesn't not show income calculation and estimate message" when {
      "tax code income is not present" in {
        val model = incomeCalculationViewModel(hasTaxCodeIncome = false)

        model.incomeCalculationMessage mustBe ""
        model.incomeCalculationEstimateMessage mustBe None
      }
    }
  }


  lazy val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  lazy val latestPayment = Payment(new LocalDate().minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)

  private def incomeCalculationViewModel(realTimeStatus: RealTimeStatus = Available,
                                         payments: Seq[Payment] = Seq(latestPayment, firstPayment),
                                         employmentStatus: TaxCodeIncomeSourceStatus = Live,
                                         employmentType: TaxCodeIncomeComponentType = EmploymentIncome,
                                         hasTaxCodeIncome : Boolean = true,
                                         cessationPay: Option[BigDecimal] = None) = {
    val annualAccount = AnnualAccount("KEY", uk.gov.hmrc.tai.model.tai.TaxYear(), realTimeStatus, payments, Nil)
    val employment = Employment("test employment", Some("EMPLOYER1"), LocalDate.now(),
      if(employmentStatus == Ceased) Some(LocalDate.parse("2017-08-08")) else None, Seq(annualAccount), "", "", 2, cessationPay, false)
    val taxCodeIncome = if(hasTaxCodeIncome){
      Some(TaxCodeIncome(employmentType, Some(2), 1111, "employment2", "150L", "test employment", Week1Month1BasisOperation, employmentStatus))
    } else {
      None
    }
    YourIncomeCalculationViewModelNew(taxCodeIncome, employment)
  }

}
