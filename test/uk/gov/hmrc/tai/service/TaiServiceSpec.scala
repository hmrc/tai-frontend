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

package uk.gov.hmrc.tai.service

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaiConnector
import uk.gov.hmrc.tai.forms.{BonusOvertimeAmountForm, PayPeriodForm, PayslipForm, TaxablePayslipForm}
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.util.TaiConstants

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random


class TaiServiceSpec extends PlaySpec
    with MockitoSugar
    with I18nSupport
    with FakeTaiPlayApplication {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  private implicit val hc = HeaderCarrier()

  "withoutSuffix" should {
    "fetch the string value of nino without suffix" when {
      "a valid nino is provided" in {
        val nino = generateNino
        val expectedResult = nino.nino.take(TaiConstants.NinoWithoutSuffixLength)
        createSut.withoutSuffix(nino) mustBe expectedResult
      }
    }
  }

  "employmentDescriptionFromIncomeType" should {
    "return description with empty employment status" when {
      "Income Type Employment is None" in {
        val sut = createSut
        val expectedResult = " " + Messages(s"tai.incomes.type-${TaiConstants.IncomeTypeDummy}")
        sut.employmentDescriptionFromIncome(basicTaxCodeIncomeSummary) mustBe expectedResult
      }

      "Income Type Employment is not 0" in {
        val sut = createSut
        val expectedResult = " " + Messages(s"tai.incomes.type-${TaiConstants.IncomeTypeDummy}")
        val incomeTaxSummary = basicTaxCodeIncomeSummary.copy(incomeType = Some(TaiConstants.IncomeTypeDummy))
        sut.employmentDescriptionFromIncome(incomeTaxSummary) mustBe expectedResult
      }
    }

    "return description with correct employment status" when {
      "Income Type Employment is 0" in {
        val sut = createSut
        val employmentStatus = 0
        val expectedResult = Messages(s"tai.incomes.status-$employmentStatus") +
          " " + Messages(s"tai.incomes.type-${TaiConstants.IncomeTypeEmployment}")
        val incomeTaxSummary = basicTaxCodeIncomeSummary.copy(incomeType = Some(TaiConstants.IncomeTypeEmployment), employmentStatus = Some(employmentStatus))
        sut.employmentDescriptionFromIncome(incomeTaxSummary) mustBe expectedResult
      }
    }
  }

  "createEmploymentAmount" should {
    "return Employment Amount instance" when {
      "basic TaxCodeIncomeSummary(with minimum fields) is provided" in {
        val sut = createSut
        val expectedEmploymentAmount = EmploymentAmount("", " ", 0, 0, 0, None, None, None, None, false, false)
        sut.createEmploymentAmount(basicTaxCodeIncomeSummary) mustBe expectedEmploymentAmount
      }

      "TaxCodeIncomeSummary with income, employment id" in {
        val sut = createSut
        val taxCodeIncomeSummary = basicTaxCodeIncomeSummary.copy(employmentId = Some(2), income = Some(3000), isLive = true)
        val expectedEmploymentAmount = EmploymentAmount("", " ", 2, 3000, 3000, None, None, None, None, true, false)
        sut.createEmploymentAmount(taxCodeIncomeSummary) mustBe expectedEmploymentAmount
      }
    }
  }

  "calculateEstimatedPay" should {
    "return calculate pay and call calculateEstimatedPay with valid PayDetails" when {
      "basic income calculation(with minimum fields) is provided" in {
        val sut = createSut
        val calculatedPay = CalculatedPay(Some(0), Some(0))
        val expectedPayDetails = PayDetails("", Some(0), None, Some(0))

        when(sut.taiClient.calculateEstimatedPay(any())(any())).thenReturn(Future.successful(calculatedPay))
        val result = sut.calculateEstimatedPay(IncomeCalculation(), None)

        Await.result(result, testTimeout) mustBe calculatedPay
        verify(sut.taiClient, times(1)).calculateEstimatedPay(expectedPayDetails)(hc)
      }

      "full income calculation is provided" in {
        val sut = createSut
        val Amount = 100
        val fakePeriod = "fakePeriod"
        val otherInDays = 1

        val incomeCalculation = IncomeCalculation(
          payPeriodForm = Some(PayPeriodForm(Some(fakePeriod), Some(otherInDays))),
          payslipForm = Some(PayslipForm(Some(Amount.toString))),
          taxablePayslipForm = Some(TaxablePayslipForm(Some(Amount.toString))),
          bonusOvertimeAmountForm = Some(BonusOvertimeAmountForm(Some(Amount.toString)))
        )

        val calculatedPay = CalculatedPay(Some(0), Some(0))
        val expectedPayDetails = PayDetails(
          paymentFrequency = fakePeriod,
          pay = Some(Amount),
          taxablePay = Some(Amount),
          days = Some(otherInDays),
          bonus = Some(Amount),
          startDate = None
        )

        when(sut.taiClient.calculateEstimatedPay(any())(any())).thenReturn(Future.successful(calculatedPay))
        val result = sut.calculateEstimatedPay(incomeCalculation, None)

        Await.result(result, testTimeout) mustBe calculatedPay
        verify(sut.taiClient, times(1)).calculateEstimatedPay(expectedPayDetails)(hc)
      }
    }
  }

  "personDetails" should {
    "expose core customer details in TaiRoot form" in {
      val sut = createSut
      val nino = generateNino

      val taiRoot = TaiRoot(nino.nino, 0, "mr", "ggg", Some("reginald"), "ppp", "ggg ppp", false, None)
      when(sut.taiClient.root(any())(any())).thenReturn(Future.successful(taiRoot))

      val result = sut.personDetails("dummy/root/uri")
      Await.result(result, testTimeout) mustBe taiRoot
    }
  }

  val testTimeout = 5 seconds
  val employmentId = 21

  val basicTaxSummaryDetails = TaxSummaryDetails(generateNino.nino, 1)
  val basicTaxCodeIncomes = TaxCodeIncomes(
    hasDuplicateEmploymentNames = false,
    totalIncome = 100,
    totalTaxableIncome = 100,
    totalTax = 100
  )
  val basicNoneTaxCodeIncomes = NoneTaxCodeIncomes(totalIncome = 0)

  val basicTaxCodeIncomeSummary = TaxCodeIncomeSummary(
    name = "",
    taxCode = "",
    tax = Tax()
  )
  val basicTaxCodeIncomeTotal = TaxCodeIncomeTotal(
    taxCodeIncomes = Nil,
    totalIncome = 0,
    totalTax = 0,
    totalTaxableIncome = 0
  )
  val basicEmploymentAmount = EmploymentAmount("", " ", 0, 0, 0)

  def taxSummaryDetailsWithTaxCodeIncomes(taxCodeIncomes: TaxCodeIncomes): TaxSummaryDetails = basicTaxSummaryDetails.copy(
    increasesTax = Some(IncreasesTax(
      total = 0,
      incomes = Some(Incomes(
        taxCodeIncomes = taxCodeIncomes,
        noneTaxCodeIncomes = basicNoneTaxCodeIncomes,
        total = 0
      ))
    ))
  )

  def generateNino: Nino = new Generator(new Random).nextNino

  def createSut = new TaiServiceTest

  class TaiServiceTest extends TaiService {
    override val taiClient: TaiConnector = mock[TaiConnector]
  }

}
