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

package controllers

import builders.RequestBuilder
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service.{EmploymentService, PaymentsService, PersonService, TaxAccountService}
import utils.BaseSpec
import views.html.incomes.{HistoricIncomeCalculationView, YourIncomeCalculationView}

import java.time.LocalDate
import scala.concurrent.Future

class YourIncomeCalculationControllerSpec extends BaseSpec {

  val firstPayment: Payment = Payment(LocalDate.now.minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  val secondPayment: Payment = Payment(LocalDate.now.minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  val thirdPayment: Payment = Payment(LocalDate.now.minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  val latestPayment: Payment = Payment(LocalDate.now.minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)

  val annualAccount: AnnualAccount = AnnualAccount(
    7,
    uk.gov.hmrc.tai.model.TaxYear(),
    Available,
    Seq(latestPayment, secondPayment, thirdPayment, firstPayment),
    Nil
  )
  val employment: Employment = Employment(
    "test employment",
    Live,
    Some("EMPLOYER1"),
    Some(LocalDate.now()),
    None,
    Seq(annualAccount),
    "",
    "",
    2,
    None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false,
    EmploymentIncome
  )

  val sampleEmployment: Seq[Employment] = Seq(
    Employment(
      "employer1",
      Live,
      None,
      Some(LocalDate.of(2016, 6, 9)),
      None,
      Seq(AnnualAccount(7, TaxYear().prev, Available, Nil, Nil)),
      "taxNumber",
      "payeNumber",
      1,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    ),
    Employment(
      "employer2",
      Live,
      None,
      Some(LocalDate.of(2016, 7, 9)),
      None,
      Seq(AnnualAccount(7, TaxYear().prev, Available, Nil, Nil)),
      "taxNumber",
      "payeNumber",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    )
  )

  val sampleEmploymentForRtiUnavailable: Seq[Employment] = Seq(
    Employment(
      "employer1",
      Live,
      None,
      Some(LocalDate.of(2016, 6, 9)),
      None,
      Seq(AnnualAccount(7, TaxYear().prev, TemporarilyUnavailable, Nil, Nil)),
      "taxNumber",
      "payeNumber",
      1,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    ),
    Employment(
      "employer2",
      Live,
      None,
      Some(LocalDate.of(2016, 7, 9)),
      None,
      Seq(AnnualAccount(7, TaxYear().prev, TemporarilyUnavailable, Nil, Nil)),
      "taxNumber",
      "payeNumber",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    )
  )

  val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "pension", Week1Month1BasisOfOperation, Live)
  )

  val personService: PersonService = mock[PersonService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]

  lazy val paymentsService: PaymentsService = inject[PaymentsService]

  def sut: YourIncomeCalculationController =
    new YourIncomeCalculationController(
      taxAccountService,
      employmentService,
      paymentsService,
      mockAuthJourney,
      mcc,
      inject[HistoricIncomeCalculationView],
      inject[YourIncomeCalculationView],
      inject[ErrorPagesHandler]
    )

  "Your Income Calculation" must {
    "return rti details page" when {
      "rti details are present" in {

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.yourIncomeCalculationPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.income.calculation.TaxableIncomeDetails", employment.name))
      }
    }

    "return internal server error" when {
      "employment details are not present" in {

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.yourIncomeCalculationPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR

      }

      "tax code details are not present" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Error")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.yourIncomeCalculationPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code details for passed employment is not present" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Error")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.yourIncomeCalculationPage(3)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "Your income calculation" should {

    "show historic data" when {
      "historic data has been passed" in {
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(sampleEmployment))
        val result =
          sut.yourIncomeCalculationHistoricYears(TaxYear().prev, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val content = contentAsString(result)
        val doc = Jsoup.parse(content)

        doc.select(".govuk-back-link").text() mustBe Messages("tai.back-link.upper")
      }
    }

    "throw internal server error" when {
      "RTI throws service unavailable" in {
        when(employmentService.employments(any(), any())(any()))
          .thenReturn(Future.successful(sampleEmploymentForRtiUnavailable))
        val result =
          sut.yourIncomeCalculationHistoricYears(TaxYear().prev, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR

      }
    }

    "throw bad request" when {
      "next year has been passed" in {
        val result =
          sut.yourIncomeCalculationHistoricYears(TaxYear().next, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR

      }
    }

  }
}
