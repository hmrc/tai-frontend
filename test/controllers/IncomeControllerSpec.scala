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

import builders.{RequestBuilder, UserBuilder}
import controllers.actions.FakeValidatePerson
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.forms.employments.EmploymentAddDateForm
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.{JourneyCacheConstants, TaiConstants}

import scala.concurrent.Future

class IncomeControllerSpec extends PlaySpec
  with MockitoSugar
  with JourneyCacheConstants
  with FakeTaiPlayApplication
  with I18nSupport
  with BeforeAndAfterEach {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override def beforeEach: Unit = {
    Mockito.reset(incomeService)
  }

  "cancel" must {
    "flush the journey cache and redirect to the employer id's income details page" in {
      val testController = createTestIncomeController()
      val employerId = 1

      when(journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = testController.cancel(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url

      verify(journeyCacheService, times(1)).flush()(any())
    }
  }

  "regularIncome" must {
    "return OK with regular income view" when {
      "valid inputs are passed" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))
        when(incomeService.cachePaymentForRegularIncome(any())(any())).thenReturn(Map.empty[String, String])
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.edit.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }

     "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

       "tax code incomes return failure" in {
         val testController = createTestIncomeController()
         val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
         val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
         val employment = employmentWithAccounts(List(annualAccount))
         when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
           thenReturn(Future.successful(1))
         when(taxAccountService.taxCodeIncomes(any(), any())(any())).
           thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
         when(employmentService.employment(any(), any())(any())).
           thenReturn(Future.successful(Some(employment)))
         when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

         val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

         status(result) mustBe INTERNAL_SERVER_ERROR
       }

       "employment return None" in {
         val testController = createTestIncomeController()
         when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
           thenReturn(Future.successful(1))
         when(taxAccountService.taxCodeIncomes(any(), any())(any())).
           thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
         when(employmentService.employment(any(), any())(any())).
           thenReturn(Future.successful(None))
         when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

         val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

         status(result) mustBe INTERNAL_SERVER_ERROR
       }
    }
  }

  "editRegularIncome" must {
    "redirect to confirm regular income page" when {
      "valid input is passed" in {
        val testController = createTestIncomeController()
        when(journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(new LocalDate(2017, 2, 1).toString))))

        when(journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("200"))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController.confirmRegularIncome().url
      }
    }

    "redirect to the same estimated pay page" when {
      "new input is the same as the cached input" in {
        val testController = createTestIncomeController()

        val sameAmount = "200"

        when(journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(new LocalDate(2017, 2, 1).toString))))

        when(journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(UpdateIncome_ConfirmedNewAmountKey -> sameAmount)))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(sameAmount))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameEstimatedPayInCache().url)
      }

      "new amount is the same as the current amount" in {
        val testController = createTestIncomeController()

        when(journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(
            Seq("1", "2", "employer name"),
            Seq(None)
          ))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = 212)
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
      }
    }

    "return Bad request" when {
      "an input error occurs" in {
        val invalidNewAmount = ""
        val testController = createTestIncomeController()
        when(journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "EmployerName"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(invalidNewAmount))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().toString must include(Messages("error.tai.updateDataEmployment.blankValue"))

      }

    }
  }

  "confirmRegularIncome" must {
    "return OK" when {
      "valid values are present in cache" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(journeyCacheService.mandatoryValues(any())(any())).
        thenReturn(Future.successful(Seq("1", "200")))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }

      "pay to date cannot be determined, due to no annual account records on the income source" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(Nil)
        when(journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("1", "200")))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController()
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(None))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "updateEstimatedIncome" must {
    "return OK" when {
      "income from employment is successfully updated" in {
        val testController = createTestIncomeController

        val employerName = "Employer"
        val employerId = 1
        val employerType = TaiConstants.IncomeTypeEmployment

        val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

        val expected = testController.renderSuccess(employerName, employerId)(fakeRequest)

        when(journeyCacheService.mandatoryValues(any())(any()))
          .thenReturn(Future.successful(Seq(employerName, "100,000", employerId.toString, employerType)))

        when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponse))

        when(estimatedPayJourneyCompletionService.journeyCompleted(Matchers.eq(employerId.toString))(any())).
          thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.updateEstimatedIncome()(fakeRequest)

        status(result) mustBe OK

        contentAsString(result) must equal(expected.toString)
      }

      "income from pension is successfully updated" in {
        val testController = createTestIncomeController

        val employerName = "Pension"
        val employerId = 1
        val employerType = TaiConstants.IncomeTypePension

        val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

        val expected = testController.renderPensionSuccess(employerName, employerId)(fakeRequest)

        when(journeyCacheService.mandatoryValues(any())(any()))
          .thenReturn(Future.successful(Seq(employerName, "100,000", employerId.toString, employerType)))

        when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponse))

        when(estimatedPayJourneyCompletionService.journeyCompleted(Matchers.eq(employerId.toString))(any())).
          thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.updateEstimatedIncome()(fakeRequest)

        status(result) mustBe OK

        contentAsString(result) must equal(expected.toString)
      }
  }

    "return Internal Server Error" when {
      "update is failed" in {
        val testController = createTestIncomeController()
        when(journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("200", "1", "TEST")))
        when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))

        val result = testController.updateEstimatedIncome()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  "pension" must {
    "return OK with regular income view" when {
      "valid inputs are passed" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))
        when(incomeService.cachePaymentForRegularIncome(any())(any())).thenReturn(Map.empty[String, String])
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.edit.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController()
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(None))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "editPensionIncome" must {
    "redirect to confirm regular income page" when {
      "valid input is passed" in {
        val testController = createTestIncomeController()
        when(journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("201"))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController.confirmPensionIncome().url
      }
    }

    "return Bad request" when {
      "new amount is blank" in {
        val testController = createTestIncomeController()
        when(journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1", "Employer Name"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(""))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to the same estimated pay page" when {
      "new input is the same as the cached input" in {
        val testController = createTestIncomeController()

        val sameAmount = "987"

        when(journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(UpdateIncome_ConfirmedNewAmountKey -> sameAmount)))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(sameAmount))
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController.sameEstimatedPayInCache().url
      }

      "new amount is the same as the current amount" in {
        val testController = createTestIncomeController()

        when(journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(
            Seq("1", "2", "employer name"),
            Seq(None)
          ))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = 212)
        val formData = Json.toJson(editIncomeForm)

        val result = testController.editPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
      }
    }
  }

  "confirmPensionIncome" must {
    "return OK" when {
      "valid values are present in cache" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("1", "200")))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController()
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(None))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "viewIncomeForEdit" must {
    "redirect user" when {
      "employment is live and is not occupational pension" in {
        val testController = createTestIncomeController()

        val employmentAmount = EmploymentAmount("employment","(Current employer)",1,11,11,None,None,None,None,true,false)
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))

        val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeController.regularIncome().url
      }

      "employment is not live and is not occupational pension" in {
        val testController = createTestIncomeController()

        val employmentAmount = EmploymentAmount("employment","(Current employer)",1,11,11,None,None,None,None,false,false)
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))

        val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.TaxAccountSummaryController.onPageLoad().url
      }

      "employment is not live and is occupational pension" in {
        val testController = createTestIncomeController()

        val employmentAmount = EmploymentAmount("employment","(Current employer)",1,11,11,None,None,None,None,false, true)
        when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))

        val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeController.pensionIncome().url
      }
    }

    "sameEstimatedPay page" should {
      "contain the employer name and current pay " in {
        val testController = createTestIncomeController()

        val currentCache: Map[String, String] = Map(UpdateIncome_ConfirmedNewAmountKey -> "12345", UpdateIncome_NameKey -> "Employer Name")

        when(journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq("Employer Name", "1", "987")))

        val result = testController.sameEstimatedPayInCache()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        val body = doc.body().toString
        body must include("Employer Name")
        body must include("987")
      }

      "fail if there are no mandatory values " in {
        val testController = createTestIncomeController()

        when(journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq.empty))

        val result = testController.sameEstimatedPayInCache()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "sameAnnualEstimatedPay" must {
      "show the same annual estimated pay page" in {
        val testController = createTestIncomeController()

        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))

        when(journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq("1")))
        when(journeyCacheService.mandatoryValueAsInt(any())(any())).thenReturn(Future.successful(1))

        val result = testController.sameAnnualEstimatedPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messagesApi("tai.updateEmployment.incomeSame.title", ""))
      }
    }
  }

  val nino = FakeAuthAction.nino

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

  val incomeService = mock[IncomeService]
  val employmentService = mock[EmploymentService]
  val personService = mock[PersonService]
  val taxAccountService = mock[TaxAccountService]
  val journeyCacheService = mock[JourneyCacheService]
  val estimatedPayJourneyCompletionService = mock[EstimatedPayJourneyCompletionService]

  private def createTestIncomeController() = new TestIncomeController()
  private class TestIncomeController() extends IncomeController(
    journeyCacheService,
    taxAccountService,
    employmentService,
    incomeService,
    estimatedPayJourneyCompletionService,
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer){

    implicit val user = UserBuilder.apply()

    def renderSuccess(employerName: String, employerId: Int) = {
      implicit request: FakeRequest[_] => {
        implicit val user = FakeAuthAction.user
        views.html.incomes.editSuccess(employerName, employerId)
      }
    }

    def renderPensionSuccess(employerName: String, employerId: Int) = {
      implicit request: FakeRequest[_] => {
        implicit val user = FakeAuthAction.user
        views.html.incomes.editPensionSuccess(employerName, employerId)
      }
    }

    val editIncomeForm = EditIncomeForm("Test", "Test", 1, None, 10, None, None, None, None)

    val employmentStartDateForm = EmploymentAddDateForm("employer")
  }

}


