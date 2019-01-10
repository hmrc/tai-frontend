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

package controllers

import builders.{AuthBuilder, RequestBuilder}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService}

import scala.concurrent.Future
import scala.util.Random

class YourIncomeCalculationControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport {
  override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Your Income Calculation" must {
    "return rti details page" when {
      "rti details are present" in {
        val sut = createSUT
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.yourIncomeCalculationPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.income.calculation.TaxableIncomeDetails", employment.name))

      }
    }

    "return internal server error" when {
      "employment details are not present" in {
        val sut = createSUT
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.yourIncomeCalculationPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR

      }

      "tax code details are not present" in {
        val sut = createSUT
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Error")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.yourIncomeCalculationPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }


      "tax code details for passed employment is not present" in {
        val sut = createSUT
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Error")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.yourIncomeCalculationPage(3)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "Your income calculation" should {

    "show historic data" when {
      "historic data has been passed" in {
        val sut = createSUT
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(sampleEmployment))
        val result = sut.yourIncomeCalculationHistoricYears(TaxYear().prev, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val content = contentAsString(result)
        val doc = Jsoup.parse(content)

        doc.select("#backLink").text() mustBe Messages("tai.back-link.upper")
      }
    }

    "throw bad request" when {
      "next year has been passed" in {
        val sut = createSUT

        val result = sut.yourIncomeCalculationHistoricYears(TaxYear().next, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR

      }
    }

  }


  "print Your income calculation" should {

    "show historic data" when {
      "historic data has been passed" in {
        val sut = createSUT
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(sampleEmployment))
        val result = sut.printYourIncomeCalculationHistoricYears(TaxYear().prev, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val content = contentAsString(result)
        val doc = Jsoup.parse(content)

        doc.select("#backLink").text() mustBe Messages("tai.back-link.upper")
      }
    }

    "throw bad request" when {
      "next year has been passed" in {
        val sut = createSUT

        val result = sut.printYourIncomeCalculationHistoricYears(TaxYear().next, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR

      }
    }

  }

  "Print Your Income Calculation" must {
    "return rti details page" when {
      "rti details are present" in {
        val sut = createSUT
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.printYourIncomeCalculationPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.yourIncome.heading"))

      }
    }

    "return internal server error" when {
      "employment details are not present" in {
        val sut = createSUT
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.printYourIncomeCalculationPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR

      }

      "tax code details are not present" in {
        val sut = createSUT
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Error")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.printYourIncomeCalculationPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }


  val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  val latestPayment = Payment(new LocalDate().minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)
  val annualAccount = AnnualAccount("KEY", uk.gov.hmrc.tai.model.TaxYear(), Available, Seq(latestPayment, secondPayment, thirdPayment, firstPayment), Nil)
  val employment = Employment("test employment", Some("EMPLOYER1"), LocalDate.now(),
    None, Seq(annualAccount), "", "", 2, None, false, false)

  val sampleEmployment = Seq(Employment("employer1", None, new LocalDate(2016, 6, 9), None, Nil, "taxNumber", "payeNumber", 1, None, false, false),
    Employment("employer2", None, new LocalDate(2016, 7, 9), None, Nil, "taxNumber", "payeNumber", 2, None, false, false))

  val taxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "pension", Week1Month1BasisOfOperation, Live))

  val nino = new Generator(new Random).nextNino

  def createSUT = new SUT

  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]

  class SUT extends YourIncomeCalculationController(
    taxAccountService,
    employmentService,
    FakeAuthAction,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {
  }


}
