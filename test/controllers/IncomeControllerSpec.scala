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

package controllers

import builders.{AuthBuilder, RequestBuilder}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.forms.employments.EmploymentAddDateForm
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.JourneyCacheConstants
import uk.gov.hmrc.tai.util.ViewModelHelper.currentTaxYearRangeHtmlNonBreak

import scala.concurrent.Future
import scala.util.Random

class IncomeControllerSpec extends PlaySpec
  with MockitoSugar
  with JourneyCacheConstants
  with FakeTaiPlayApplication
  with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "regularIncome" must {
    "return OK with regular income view" when {
      "valid inputs are passed" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(testController.incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))
        when(testController.incomeService.cachePaymentForRegularIncome(any())(any())).thenReturn(Map.empty[String, String])
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.edit.heading", currentTaxYearRangeHtmlNonBreak))
      }
    }

     "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

       "tax code incomes return failure" in {
         val testController = createTestIncomeController
         val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
         val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
         val employment = employmentWithAccounts(List(annualAccount))
         when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
           thenReturn(Future.successful(1))
         when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
           thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
         when(testController.employmentService.employment(any(), any())(any())).
           thenReturn(Future.successful(Some(employment)))
         when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

         val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

         status(result) mustBe INTERNAL_SERVER_ERROR
       }

       "employment return None" in {
         val testController = createTestIncomeController
         when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
           thenReturn(Future.successful(1))
         when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
           thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
         when(testController.employmentService.employment(any(), any())(any())).
           thenReturn(Future.successful(None))
         when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

         val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

         status(result) mustBe INTERNAL_SERVER_ERROR
       }
    }
  }

  "editRegularIncome" must {
    "redirect to confirm regular income page" when {
      "valid input is passed" in {
        val testController = createTestIncomeController
        when(testController.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(testController.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("200"))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController.confirmRegularIncome().url
      }
    }

    "return Bad request" when {
      "new amount is blank" in {
        val invalidNewAmount = ""
        val testController = createTestIncomeController
        when(testController.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "EmployerName"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(testController.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(invalidNewAmount))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().toString must include(Messages("error.tai.updateDataEmployment.blankValue"))

      }

      "new amount is not a number" in {
        val invalidNewAmount = "Not a number"
        val testController = createTestIncomeController
        when(testController.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(testController.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(invalidNewAmount))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().toString must include(Messages("error.tai.updateDataEmployment.enterRealNumber"))

      }

      "new amount is too large" in {
        val invalidNewAmount = "1000000000"
        val testController = createTestIncomeController
        when(testController.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(testController.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(invalidNewAmount))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().toString must include(Messages("error.tai.updateDataEmployment.maxLength"))

      }

      "new amount is less than old amount" in {
        val invalidNewAmount = "50"
        val date = new LocalDate(2017, 2, 1)
        val dateForMessage = "February 2017"
        val EmployerName = "Employer Name"
        val testController = createTestIncomeController
        when(testController.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(date.toString))))
        when(testController.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(invalidNewAmount))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().toString must include(Messages("error.tai.updateDataEmployment.enterLargerValue", "100", dateForMessage, EmployerName))
      }
    }
  }

  "confirmRegularIncome" must {
    "return OK" when {
      "valid values are present in cache" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(testController.journeyCacheService.mandatoryValues(any())(any())).
        thenReturn(Future.successful(Seq("1", "200")))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }

      "pay to date cannot be determined, due to no annual account records on the income source" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(Nil)
        when(testController.journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("1", "200")))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(None))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "updateEstimatedIncome" must {
    "return OK" when {
      "income is successfully updated" in {
        val testController = createTestIncomeController

        when(testController.journeyCacheService.mandatoryValues(any())(any()))
          .thenReturn(Future.successful(Seq("Employer", "100,000", "1")))

        when(testController.taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponse))

        val result = testController.updateEstimatedIncome()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.updated.check.title", "Employer"))
      }
    }

    "return OK" when {
      "income is successfully updated with comma separated values input" in {
        val testController = createTestIncomeController

        when(testController.journeyCacheService.mandatoryValues(any())(any()))
          .thenReturn(Future.successful(Seq("Employer", "100,000", "1")))

        when(testController.taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponse))

        val result = testController.updateEstimatedIncome()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.updated.check.title", "Employer"))
      }
    }

    "return Internal Server Error" when {
      "update is failed" in {
        val testController = createTestIncomeController
        when(testController.journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("200", "1", "TEST")))
        when(testController.taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))

        val result = testController.updateEstimatedIncome()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "pension" must {
    "return OK with regular income view" when {
      "valid inputs are passed" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(testController.incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))
        when(testController.incomeService.cachePaymentForRegularIncome(any())(any())).thenReturn(Map.empty[String, String])
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.edit.heading", currentTaxYearRangeHtmlNonBreak))
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(None))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "editPensionIncome" must {
    "redirect to confirm regular income page" when {
      "valid input is passed" in {
        val testController = createTestIncomeController
        when(testController.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(testController.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("200"))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController.confirmPensionIncome().url
      }
    }

    "return Bad request" when {
      "new amount is blank" in {
        val testController = createTestIncomeController
        when(testController.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(testController.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(""))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "confirmPensionIncome" must {
    "return OK" when {
      "valid values are present in cache" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(testController.journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("1", "200")))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(testController.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(None))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "viewIncomeForEdit" must {
    "redirect user" when {
      "employment is live and is not occupational pension" in {
        val testController = createTestIncomeController

        val employmentAmount = EmploymentAmount("employment","(Current employer)",1,11,11,None,None,None,None,true,false)
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))

        val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeController.regularIncome().url
      }

      "employment is not live and is not occupational pension" in {
        val testController = createTestIncomeController

        val employmentAmount = EmploymentAmount("employment","(Current employer)",1,11,11,None,None,None,None,false,false)
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))

        val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.TaxAccountSummaryController.onPageLoad().url
      }

      "employment is not live and is occupational pension" in {
        val testController = createTestIncomeController

        val employmentAmount = EmploymentAmount("employment","(Current employer)",1,11,11,None,None,None,None,false, true)
        when(testController.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))

        val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeController.pensionIncome().url
      }
    }
  }

  val nino = new Generator(new Random).nextNino

  def employmentWithAccounts(accounts:List[AnnualAccount]) = Employment("ABCD", Some("ABC123"), new LocalDate(2000, 5, 20),
    None, accounts, "", "", 8, None, false, false)

  def paymentOnDate(date: LocalDate) = Payment(
    date = date,
    amountYearToDate = 2000,
    taxAmountYearToDate = 200,
    nationalInsuranceAmountYearToDate = 100,
    amount = 1000,
    taxAmount = 100,
    nationalInsuranceAmount = 50,
    payFrequency = Monthly)

  val taxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )


  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val employmentAmount = EmploymentAmount("employment","(Current employer)",1,1111,1111,
    None,None,Some(new LocalDate(2000, 5, 20)), None, true, false)

  private def createTestIncomeController = new TestIncomeController
  private class TestIncomeController extends IncomeController {
    override implicit def templateRenderer: MockTemplateRenderer.type = MockTemplateRenderer

    override val personService: PersonService = mock[PersonService]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override implicit val partialRetriever: FormPartialRetriever = mock[FormPartialRetriever]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val incomeService: IncomeService = mock[IncomeService]

    val editIncomeForm = EditIncomeForm("Test", "Test", 1, None, 10, None, None, None, None)

    val employmentStartDateForm = EmploymentAddDateForm("employer")
    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(nino.nino)))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

  }

}


