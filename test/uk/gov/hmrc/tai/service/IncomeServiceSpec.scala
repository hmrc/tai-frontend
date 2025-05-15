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

package uk.gov.hmrc.tai.service

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.income.{UpdateIncomePayToDatePage, UpdatedIncomeDatePage}
import uk.gov.hmrc.tai.connectors.TaiConnector
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.{CalculatedPay, EmploymentAmount, PayDetails, TaxYear, UserAnswers}
import uk.gov.hmrc.tai.util.constants.journeyCache._
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class IncomeServiceSpec extends BaseSpec {
  val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  def employmentWithAccounts(accounts: List[AnnualAccount]): Employment =
    Employment(
      "ABCD",
      Live,
      Some("ABC123"),
      Some(LocalDate.of(2000, 5, 20)),
      None,
      accounts,
      "",
      "",
      8,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    )
  def paymentOnDate(date: LocalDate): Payment =
    Payment(
      date = date,
      amountYearToDate = 2000,
      taxAmountYearToDate = 200,
      nationalInsuranceAmountYearToDate = 100,
      amount = 1000,
      taxAmount = 100,
      nationalInsuranceAmount = 50,
      payFrequency = Monthly,
      duplicate = None
    )

  def createSUT = new SUT

  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taiConnector: TaiConnector = mock[TaiConnector]

  class SUT
      extends IncomeService(
        taxAccountService,
        employmentService,
        taiConnector
      )

  "employmentAmount" must {
    "return employment amount" when {
      "valid inputs are passed" in {
        val sut = createSUT

        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = Await.result(sut.employmentAmount(nino, 1), 5.seconds)

        result mustBe EmploymentAmount(
          "employment",
          "(Current employer)",
          1,
          Some(1111),
          None,
          None,
          Some(LocalDate.of(2000, 5, 20)),
          None
        )

      }
    }

    "return an error" when {
      "employment details not found" in {
        val sut = createSUT

        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val ex = the[RuntimeException] thrownBy Await.result(sut.employmentAmount(nino, 1), 5.seconds)
        ex.getMessage mustBe "Not able to found employment with id 1"
      }

      "income not found" in {
        val sut = createSUT

        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val ex = the[RuntimeException] thrownBy Await.result(sut.employmentAmount(nino, 1), 5.seconds)
        ex.getMessage mustBe "Exception while reading employment and tax code details"
      }

      "employment not found" in {
        val sut = createSUT

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val ex = the[RuntimeException] thrownBy Await.result(sut.employmentAmount(nino, 1), 5.seconds)
        ex.getMessage mustBe "Exception while reading employment and tax code details"
      }
    }
  }

  "latestPayment" must {
    "return latest payment" when {
      "valid inputs are passed" in {
        val sut = createSUT

        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        Await.result(sut.latestPayment(nino, 1), 5.seconds) mustBe Some(payment)
      }
    }

    "return none" when {
      "employment details are not found" in {
        val sut = createSUT

        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        Await.result(sut.latestPayment(nino, 1), 5.seconds) mustBe None
      }

      "payments details are not present" in {
        val sut = createSUT

        val annualAccount = AnnualAccount(TaxYear(), Available, Seq.empty[Payment], Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        Await.result(sut.latestPayment(nino, 1), 5.seconds) mustBe None
      }
    }
  }

  "calculateEstimatedPay" must {
    "return calculated pay" when {
      "cache is empty and start date is not available" in {
        val sut = createSUT

        val payDetails = PayDetails("", Some(0), None, Some(0), None, None)

        when(taiConnector.calculateEstimatedPay(payDetails)).thenReturn(Future.successful(CalculatedPay(None, None)))
        Await.result(sut.calculateEstimatedPay(Map.empty[String, String], None), 5.seconds) mustBe CalculatedPay(
          None,
          None
        )
      }

      "cache is not empty" in {
        val sut = createSUT

        val cache = Map(
          UpdateIncomeConstants.PayPeriodKey           -> "monthly",
          UpdateIncomeConstants.TotalSalaryKey         -> "£100",
          UpdateIncomeConstants.TaxablePayKey          -> "£100",
          UpdateIncomeConstants.OtherInDaysKey         -> "10",
          UpdateIncomeConstants.BonusOvertimeAmountKey -> "£100"
        )

        val payDetails = PayDetails("monthly", Some(100), Some(100), Some(10), Some(100), None)

        when(taiConnector.calculateEstimatedPay(payDetails)).thenReturn(Future.successful(CalculatedPay(None, None)))
        Await.result(sut.calculateEstimatedPay(cache, None), 5.seconds) mustBe CalculatedPay(None, None)
      }
    }
  }

  "editableIncome" must {
    "return editable incomes" when {
      "provided with sequence of tax code income" in {
        val sut = createSUT

        val taxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live
          ),
          TaxCodeIncome(
            PensionIncome,
            Some(4),
            4444,
            "employment",
            "BR",
            "employer4",
            Week1Month1BasisOfOperation,
            Live
          ),
          TaxCodeIncome(
            JobSeekerAllowanceIncome,
            Some(5),
            5555,
            "employment",
            "1150L",
            "employer5",
            OtherBasisOfOperation,
            Live
          ),
          TaxCodeIncome(
            JobSeekerAllowanceIncome,
            Some(6),
            6666,
            "employment",
            "BR",
            "employer6",
            Week1Month1BasisOfOperation,
            Live
          ),
          TaxCodeIncome(OtherIncome, Some(7), 7777, "employment", "1150L", "employer7", OtherBasisOfOperation, Live),
          TaxCodeIncome(OtherIncome, Some(8), 8888, "employment", "BR", "employer8", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(
            EmploymentIncome,
            Some(9),
            1111,
            "employment",
            "1150L",
            "employer9",
            OtherBasisOfOperation,
            PotentiallyCeased
          ),
          TaxCodeIncome(
            EmploymentIncome,
            Some(10),
            2222,
            "employment",
            "BR",
            "employer10",
            Week1Month1BasisOfOperation,
            Ceased
          ),
          TaxCodeIncome(
            PensionIncome,
            Some(11),
            1111,
            "employment",
            "1150L",
            "employer11",
            OtherBasisOfOperation,
            PotentiallyCeased
          ),
          TaxCodeIncome(
            PensionIncome,
            Some(12),
            2222,
            "employment",
            "BR",
            "employer12",
            Week1Month1BasisOfOperation,
            Ceased
          )
        )

        val expectedTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live
          ),
          TaxCodeIncome(
            PensionIncome,
            Some(4),
            4444,
            "employment",
            "BR",
            "employer4",
            Week1Month1BasisOfOperation,
            Live
          ),
          TaxCodeIncome(
            EmploymentIncome,
            Some(9),
            1111,
            "employment",
            "1150L",
            "employer9",
            OtherBasisOfOperation,
            PotentiallyCeased
          ),
          TaxCodeIncome(
            PensionIncome,
            Some(11),
            1111,
            "employment",
            "1150L",
            "employer11",
            OtherBasisOfOperation,
            PotentiallyCeased
          )
        )

        sut.editableIncomes(taxCodeIncomes) mustBe expectedTaxCodeIncomes
      }
    }
  }

  "getSingularIncomeId" must {
    "return singular income employment id" when {
      "income size is 1" in {
        val sut = createSUT
        val taxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live
          ),
          TaxCodeIncome(
            PensionIncome,
            Some(4),
            4444,
            "employment",
            "BR",
            "employer4",
            Week1Month1BasisOfOperation,
            Ceased
          )
        )

        sut.singularIncomeId(taxCodeIncomes) mustBe Some(1)
      }
    }

    "return none" when {
      "income size is not 1" in {
        val sut = createSUT
        val taxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live
          ),
          TaxCodeIncome(
            PensionIncome,
            Some(4),
            4444,
            "employment",
            "BR",
            "employer4",
            Week1Month1BasisOfOperation,
            Live
          )
        )

        sut.singularIncomeId(taxCodeIncomes) mustBe None
      }
    }
  }

  "cachePaymentForRegularIncome" must {
    "update UserAnswers correctly" when {
      "payment is None" in {
        val sut = createSUT
        val userAnswers = UserAnswers("testSessionId", nino.nino)

        val updatedAnswers = sut.cachePaymentForRegularIncome(None, userAnswers)
        updatedAnswers.get(UpdateIncomePayToDatePage) mustBe Some("0")
        updatedAnswers.get(UpdatedIncomeDatePage) mustBe None
      }

      "payment has value" in {
        val sut = createSUT
        val userAnswers = UserAnswers("testSessionId", nino.nino)
        val payment = paymentOnDate(LocalDate.of(2017, 9, 6))

        val updatedAnswers = sut.cachePaymentForRegularIncome(Some(payment), userAnswers)
        updatedAnswers.get(UpdateIncomePayToDatePage) mustBe Some("2000")
        updatedAnswers.get(UpdatedIncomeDatePage) mustBe Some(payment.date.toString)
      }
    }
  }
}
