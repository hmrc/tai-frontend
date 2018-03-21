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
      }

      "employment type is Pension Income" in {
        val model = incomeCalculationViewModel(employmentType = PensionIncome)
        model.isPension mustBe true
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
  }


  lazy val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  lazy val latestPayment = Payment(new LocalDate().minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)

  private def incomeCalculationViewModel(realTimeStatus: RealTimeStatus = Available,
                                         payments: Seq[Payment] = Seq(latestPayment, firstPayment),
                                         employmentStatus: TaxCodeIncomeSourceStatus = Live,
                                         employmentType: TaxCodeIncomeComponentType = EmploymentIncome) = {
    val annualAccount = AnnualAccount("KEY", uk.gov.hmrc.tai.model.tai.TaxYear(), realTimeStatus, payments, Nil)
    val employment = Employment("test employment", Some("EMPLOYER1"), LocalDate.now(),
      if(employmentStatus == Ceased) Some(LocalDate.parse("2017-08-08")) else None, Seq(annualAccount), "", "", 2, None, false)
    val taxCodeIncome = TaxCodeIncome(employmentType, Some(2), 1111, "employment2", "150L", "test employment", Week1Month1BasisOperation, employmentStatus)
    YourIncomeCalculationViewModelNew(Some(taxCodeIncome), employment)
  }

}
