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
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.forms.employments.EmploymentAddDateForm
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaiRoot}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome, Week1Month1BasisOperation}
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.JourneyCacheConstants

import scala.concurrent.Future
import scala.util.Random

class IncomeControllerNewSpec extends PlaySpec
  with MockitoSugar
  with JourneyCacheConstants
  with FakeTaiPlayApplication
  with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "regularIncome" must {
    "return OK with regular income view" when {
      "valid inputs are passed" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(sut.incomeService.employmentAmount(any(), any())(any())).thenReturn(Future.successful(employmentAmount))
        when(sut.incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))
        when(sut.incomeService.cachePaymentForRegularIncome(any())(any())).thenReturn(Map.empty[String, String])
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.edit.heading"))
      }
    }

     "return Internal Server Error" when {
      "employment doesn't present" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

       "tax code incomes return failure" in {
         val sut = createSUT
         val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
         val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
         val employment = employmentWithAccounts(List(annualAccount))
         when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
           thenReturn(Future.successful(1))
         when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
           thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
         when(sut.employmentService.employment(any(), any())(any())).
           thenReturn(Future.successful(Some(employment)))
         when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

         val result = sut.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

         status(result) mustBe INTERNAL_SERVER_ERROR
       }

       "employment return None" in {
         val sut = createSUT
         when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
           thenReturn(Future.successful(1))
         when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
           thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
         when(sut.employmentService.employment(any(), any())(any())).
           thenReturn(Future.successful(None))
         when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

         val result = sut.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

         status(result) mustBe INTERNAL_SERVER_ERROR
       }
    }
  }

  "editRegularIncome" must {
    "redirect to confirm regular income page" when {
      "valid input is passed" in {
        val sut = createSUT
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(sut.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = sut.editIncomeForm.copy(newAmount = Some("200"))
        val formData = Json.toJson(editIncomeForm)

        val result = sut.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeControllerNew.confirmRegularIncome().url
      }
    }

    "return Bad request" when {
      "new amount is blank" in {
        val sut = createSUT
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(sut.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = sut.editIncomeForm.copy(newAmount = Some(""))
        val formData = Json.toJson(editIncomeForm)

        val result = sut.editRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "confirmRegularIncome" must {
    "return OK" when {
      "valid values are present in cache" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(sut.journeyCacheService.mandatoryValues(any())(any())).
        thenReturn(Future.successful(Seq("1", "200")))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = sut.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = sut.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = sut.confirmRegularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(None))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.regularIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "updateEstimatedIncome" must {
    "return OK" when {
      "income is successfully updated" in {
        val sut = createSUT
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("200", "1"), Seq(Some("TEST"))))
        when(sut.taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponse))

        val result = sut.updateEstimatedIncome()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.updated.check.title"))
      }
    }

    "return OK" when {
      "income is successfully updated with comma separated values input" in {
        val sut = createSUT
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("200,000", "1"), Seq(Some("TEST"))))
        when(sut.taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponse))

        val result = sut.updateEstimatedIncome()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.updated.check.title"))
      }
    }

    "return Internal Server Error" when {
      "update is failed" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("200", "1", "TEST")))
        when(sut.taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))

        val result = sut.updateEstimatedIncome()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "pension" must {
    "return OK with regular income view" when {
      "valid inputs are passed" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(sut.incomeService.employmentAmount(any(), any())(any())).thenReturn(Future.successful(employmentAmount))
        when(sut.incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))
        when(sut.incomeService.cachePaymentForRegularIncome(any())(any())).thenReturn(Map.empty[String, String])
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.pension.heading"))
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(None))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.pensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "editPensionIncome" must {
    "redirect to confirm regular income page" when {
      "valid input is passed" in {
        val sut = createSUT
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(sut.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = sut.editIncomeForm.copy(newAmount = Some("200"))
        val formData = Json.toJson(editIncomeForm)

        val result = sut.editPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeControllerNew.confirmPensionIncome().url
      }
    }

    "return Bad request" when {
      "new amount is blank" in {
        val sut = createSUT
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(Seq("100", "1"), Seq(Some(new LocalDate(2017, 2, 1).toString))))
        when(sut.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))
        val editIncomeForm = sut.editIncomeForm.copy(newAmount = Some(""))
        val formData = Json.toJson(editIncomeForm)

        val result = sut.editPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "confirmPensionIncome" must {
    "return OK" when {
      "valid values are present in cache" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(sut.journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("1", "200")))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = sut.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = sut.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val sut = createSUT
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(Some(employment)))

        val result = sut.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).
          thenReturn(Future.successful(1))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.employmentService.employment(any(), any())(any())).
          thenReturn(Future.successful(None))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.confirmPensionIncome()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "viewIncomeForEdit" must {
    "redirect user" when {
      "employment is live and is not occupational pension" in {
        val sut = createSUT

        val employmentAmount = EmploymentAmount("employment","(Current employer)",1,11,11,None,None,None,None,true,false)
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(sut.incomeService.employmentAmount(any(), any())(any())).thenReturn(Future.successful(employmentAmount))

        val result = sut.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeControllerNew.regularIncome().url
      }

      "employment is not live and is not occupational pension" in {
        val sut = createSUT

        val employmentAmount = EmploymentAmount("employment","(Current employer)",1,11,11,None,None,None,None,false,false)
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(sut.incomeService.employmentAmount(any(), any())(any())).thenReturn(Future.successful(employmentAmount))

        val result = sut.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.TaxAccountSummaryController.onPageLoad().url
      }

      "employment is not live and is occupational pension" in {
        val sut = createSUT

        val employmentAmount = EmploymentAmount("employment","(Current employer)",1,11,11,None,None,None,None,false, true)
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(1))
        when(sut.incomeService.employmentAmount(any(), any())(any())).thenReturn(Future.successful(employmentAmount))

        val result = sut.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeControllerNew.pensionIncome().url
      }
    }
  }

  val nino = new Generator(new Random).nextNino

  def employmentWithAccounts(accounts:List[AnnualAccount]) = Employment("ABCD", Some("ABC123"), new LocalDate(2000, 5, 20),
    None, accounts, "", "", 8)

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
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOperation, Live)
  )


  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val employmentAmount = EmploymentAmount("employment","(Current employer)",1,1111,1111,
    None,None,Some(new LocalDate(2000, 5, 20)),None,true,false)

  private def createSUT = new SUT
  private class SUT extends IncomeControllerNew {
    override implicit def templateRenderer: MockTemplateRenderer.type = MockTemplateRenderer

    override val taiService: TaiService = mock[TaiService]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override implicit val partialRetriever: PartialRetriever = mock[PartialRetriever]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val incomeService: IncomeService = mock[IncomeService]

    val editIncomeForm = EditIncomeForm("Test", "Test", 1, None, 10, None, None, None, None)

    val employmentStartDateForm = EmploymentAddDateForm("employer")
    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(nino.nino)))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(TaiRoot("", 1, "", "", None, "", "", false, None)))

  }

}


