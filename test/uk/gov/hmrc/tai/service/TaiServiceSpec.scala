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

import org.joda.time.LocalDate

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random
import builders.UserBuilder
import uk.gov.hmrc.tai.connectors.TaiConnector
import controllers.FakeTaiPlayApplication
import controllers.auth.TaiUser
import uk.gov.hmrc.tai.forms.{BonusOvertimeAmountForm, PayPeriodForm, PayslipForm, TaxablePayslipForm}
import uk.gov.hmrc.tai.model._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.util.TaiConstants
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._


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

  "updateSessionData" should {
    "update the cache from provided session data" in {
      
      val sut = createSut
      val nino = generateNino
      val taxSummaryDetails = TaxSummaryDetails(nino.nino, 1)
      val sessionData = SessionData(nino = nino.nino, taxSummaryDetailsCY = taxSummaryDetails)
      when(sut.taiClient.updateTaiData(any())(any())).thenReturn(Future.successful(sessionData))

      val result = Await.result(sut.updateTaiSession(sessionData), testTimeout)
      result mustBe sessionData
      verify(sut.taiClient, times(1)).updateTaiData(any())(any())
    }
  }

  "getIncome" should {
    "return None" when {
      "Increases Tax doesn't have any incomes" in {
        val sut = createSut
        val taxSummaryDetails = basicTaxSummaryDetails.copy(increasesTax = Some(IncreasesTax(total = 0)))
        sut.getIncome(taxSummaryDetails, 1) mustBe None
      }

      "TaxCode incomes has only taxable state benefit income with the same employment id provided" in {
        val sut = createSut

        val taxCodeIncomes: TaxCodeIncomes = basicTaxCodeIncomes.copy(
          taxableStateBenefitIncomes = Some(basicTaxCodeIncomeTotal.copy(
            taxCodeIncomes = List(basicTaxCodeIncomeSummary.copy(employmentId = Some(employmentId)))
          ))
        )
        val taxSummaryDetails = taxSummaryDetailsWithTaxCodeIncomes(taxCodeIncomes)
        sut.getIncome(taxSummaryDetails, employmentId) mustBe None
      }

      "TaxCode incomes has employments with different employment id" in {
        val sut = createSut

        val taxCodeIncomeSummary = basicTaxCodeIncomeSummary.copy(employmentId = Some(employmentId + 1))
        val employments = Some(basicTaxCodeIncomeTotal.copy(taxCodeIncomes = List(taxCodeIncomeSummary)))

        val taxCodeIncomes: TaxCodeIncomes = basicTaxCodeIncomes.copy(employments = employments)
        val taxSummaryDetails = taxSummaryDetailsWithTaxCodeIncomes(taxCodeIncomes)

        sut.getIncome(taxSummaryDetails, employmentId) mustBe None
      }
    }

    "return Tax code income summary" when {
      "TaxCode incomes has employment with matching employment id" in {
        val sut = createSut

        val taxCodeIncomeSummaryMatched = basicTaxCodeIncomeSummary.copy(employmentId = Some(employmentId))
        val taxCodeIncomeSummaryNotMatched = basicTaxCodeIncomeSummary.copy(employmentId = Some(employmentId + 1))
        val employments = Some(basicTaxCodeIncomeTotal.copy(taxCodeIncomes = List(taxCodeIncomeSummaryMatched, taxCodeIncomeSummaryNotMatched)))

        val taxCodeIncomes: TaxCodeIncomes = basicTaxCodeIncomes.copy(employments = employments)
        val taxSummaryDetails = taxSummaryDetailsWithTaxCodeIncomes(taxCodeIncomes)

        sut.getIncome(taxSummaryDetails, employmentId) mustBe Some(taxCodeIncomeSummaryMatched)
      }

      "TaxCode incomes has occupationalPension with matching employment id" in {
        val sut = createSut

        val taxCodeIncomeSummaryMatched = basicTaxCodeIncomeSummary.copy(employmentId = Some(employmentId))
        val taxCodeIncomeSummaryNotMatched = basicTaxCodeIncomeSummary.copy(employmentId = Some(employmentId + 1))
        val occupationalPensions = Some(basicTaxCodeIncomeTotal.copy(taxCodeIncomes = List(taxCodeIncomeSummaryMatched, taxCodeIncomeSummaryNotMatched)))

        val taxCodeIncomes: TaxCodeIncomes = basicTaxCodeIncomes.copy(occupationalPensions = occupationalPensions)
        val taxSummaryDetails = taxSummaryDetailsWithTaxCodeIncomes(taxCodeIncomes)

        sut.getIncome(taxSummaryDetails, employmentId) mustBe Some(taxCodeIncomeSummaryMatched)
      }

      "TaxCode incomes has ceasedEmployment with matching employment id" in {
        val sut = createSut

        val taxCodeIncomeSummaryMatched = basicTaxCodeIncomeSummary.copy(employmentId = Some(employmentId))
        val taxCodeIncomeSummaryNotMatched = basicTaxCodeIncomeSummary.copy(employmentId = Some(employmentId + 1))
        val ceasedEmployments = Some(basicTaxCodeIncomeTotal.copy(taxCodeIncomes = List(taxCodeIncomeSummaryMatched, taxCodeIncomeSummaryNotMatched)))

        val taxCodeIncomes: TaxCodeIncomes = basicTaxCodeIncomes.copy(ceasedEmployments = ceasedEmployments)
        val taxSummaryDetails = taxSummaryDetailsWithTaxCodeIncomes(taxCodeIncomes)

        sut.getIncome(taxSummaryDetails, employmentId) mustBe Some(taxCodeIncomeSummaryMatched)
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

  "incomeForEdit" should {
    "return None" when {
      "getIncome returns None" in {
        val sut = createSut
        val taxSummaryDetails = basicTaxSummaryDetails.copy(increasesTax = Some(IncreasesTax(total = 0)))
        sut.incomeForEdit(taxSummaryDetails, 1) mustBe None
      }
    }

    "return EmploymentAmount" when {
      "getIncome returns some TaxCodeIncomeSummary" in {
        val sut = createSut

        val taxCodeIncomeSummary = basicTaxCodeIncomeSummary.copy(employmentId = Some(employmentId))
        val employments = Some(basicTaxCodeIncomeTotal.copy(taxCodeIncomes = List(taxCodeIncomeSummary)))
        val taxCodeIncomes: TaxCodeIncomes = basicTaxCodeIncomes.copy(employments = employments)
        val taxSummaryDetails = taxSummaryDetailsWithTaxCodeIncomes(taxCodeIncomes)
        val expectedEmploymentAmount = EmploymentAmount("", " ", employmentId, 0, 0, None, None, None, None, false, false)
        sut.incomeForEdit(taxSummaryDetails, employmentId) mustBe Some(expectedEmploymentAmount)
      }
    }
  }

  "updateIncome" should {
    "throw IllegalArgumentException" when {
      "there is no paye account" in {
        val sut = createSut
        val userBuilt = UserBuilder()
        val accountWithoutPaye: Accounts = userBuilt.authContext.principal.accounts.copy(paye = None)
        implicit val user: TaiUser = userBuilt.copy(authContext = userBuilt.authContext.copy(principal = userBuilt.authContext.principal.copy(accounts = accountWithoutPaye)))
        val thrown = the[IllegalArgumentException] thrownBy sut.updateIncome(generateNino, 2017, 1, basicEmploymentAmount)
        thrown.getMessage mustBe "Cannot find tai user authority"
      }
    }

    "return IabdUpdateEmploymentsResponse" when {
      "provided with TaiUser, employmentAmount, nino, version and taxyear" in {
        val sut = createSut
        implicit val user = UserBuilder()

        val nino = generateNino
        val Year = 2017
        val taxSummaryDetails = TaxSummaryDetails(nino.nino, 1)
        val sessionData = SessionData(nino = nino.nino, taxSummaryDetailsCY = taxSummaryDetails)
        val iabdUpdateEmploymentsResponse = IabdUpdateEmploymentsResponse(TransactionId(""), 1, 1, Nil)

        when(sut.taiClient.updateEmployments(any(), any(), any())(any())).thenReturn(Future.successful(iabdUpdateEmploymentsResponse))
        when(sut.taiClient.taxSummary(any(), any())(any())).thenReturn(Future.successful(taxSummaryDetails))
        when(sut.taiClient.root(any())(any())).thenReturn(Future.successful(TaiRoot()))
        when(sut.taiClient.getTaiData(any())(any())).thenReturn(Future.successful(SessionData("", None, taxSummaryDetails, None, None)))

        val result = Await.result(sut.updateIncome(nino, Year, 1, basicEmploymentAmount), testTimeout)

        result mustBe iabdUpdateEmploymentsResponse
        verify(sut.taiClient, times(1)).getTaiData(any())(any())
      }
    }

    "return future failure and should not save data to the session " when {
      "updateEmployments fails" in {
        val sut = createSut
        implicit val user = UserBuilder()

        val nino = generateNino
        val Year = 2017
        val taxSummaryDetails = TaxSummaryDetails(nino.nino, 1)
        val sessionData = SessionData(nino = nino.nino, taxSummaryDetailsCY = taxSummaryDetails)
        val exceptionMessage = "fake exception"

        when(sut.taiClient.updateEmployments(any(), any(), any())(any())).thenReturn(Future.failed(exception = new RuntimeException(exceptionMessage)))

        when(sut.taiClient.taxSummary(any(), any())(any())).thenReturn(Future.successful(taxSummaryDetails))
        when(sut.taiClient.root(any())(any())).thenReturn(Future.successful(TaiRoot()))

        val result = sut.updateIncome(generateNino, 2017, 1, basicEmploymentAmount)
        ScalaFutures.whenReady(result.failed) {
          e => e.getMessage mustBe exceptionMessage
        }

        verify(sut.taiClient, never).taxSummary(any(), any())(any())
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
