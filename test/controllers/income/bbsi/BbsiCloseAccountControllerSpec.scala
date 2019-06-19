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

package controllers.income.bbsi

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import controllers.{FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms.DateForm
import uk.gov.hmrc.tai.model.domain.BankAccount
import uk.gov.hmrc.tai.model.{CloseAccountRequest, TaxYear}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{BbsiService, PersonService}
import uk.gov.hmrc.tai.util.constants.{BankAccountClosingInterestConstants, FormValuesConstants, JourneyCacheConstants}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class BbsiCloseAccountControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport
  with JourneyCacheConstants
  with FormValuesConstants
  with BankAccountClosingInterestConstants
  with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    Mockito.reset(journeyCacheService)
  }

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "closeBankAccountDate" must {

    "show the close bank account date form" when {
      "the request contains a valid bank account id, account number, sortcode and account name" in {

        val sut = createSUT

        when(bbsiService.bankAccount(any(), any())(any()))
          .thenReturn(Future.successful(Some(bankAccount1)))
        when(journeyCacheService.currentValueAsDate(any())(any())).thenReturn(Future.successful(None))

        val result = sut.captureCloseDate(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.closeBankAccount.closeDateForm.title", "Test Bank account name"))
      }
    }

    "return error" when {
      "the bbsi service doesn't return an account with account number, sort code or account name" in {

        val sut = createSUT

        when(bbsiService.bankAccount(any(), any())(any()))
          .thenReturn(Future.successful(Some(bankAccount1.copy(bankName = None))))
        when(journeyCacheService.currentValueAsDate(any())(any())).thenReturn(Future.successful(None))

        val result = sut.captureCloseDate(2)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "the a bank account isn't found" in {

        val sut = createSUT

        when(bbsiService.bankAccount(any(), any())(any()))
          .thenReturn(Future.successful(None))
        when(journeyCacheService.currentValueAsDate(any())(any())).thenReturn(Future.successful(None))

        val result = sut.captureCloseDate(2)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit close date" must {
    "return redirect to checkYourAnswers page" when {
      "form is valid" in {

        val sut = createSUT

        val validFormData = Json.obj(
          sut.closeBankAccountDateForm.DateFormDay -> "01",
          sut.closeBankAccountDateForm.DateFormMonth -> "02",
          sut.closeBankAccountDateForm.DateFormYear -> "2017"
        )
        when(bbsiService.bankAccount(any(), any())(any()))
          .thenReturn(Future.successful(Some(bankAccount1)))

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map(CloseBankAccountDateKey -> "2017-02-01", CloseBankAccountNameKey -> "Test")))

        val result = sut.submitCloseDate(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(validFormData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.bbsi.routes.BbsiCloseAccountController.checkYourAnswers(bankAccountId).url
      }
    }

    "return redirect to checkYourAnswers page" when {
      "form is supplied with a date before the current tax year" in {

        val sut = createSUT

        val date = TaxYear().endPrev

        val validFormData = Json.obj(
          sut.closeBankAccountDateForm.DateFormDay -> date.getDayOfMonth,
          sut.closeBankAccountDateForm.DateFormMonth -> date.getMonthOfYear,
          sut.closeBankAccountDateForm.DateFormYear -> date.getYear
        )
        when(bbsiService.bankAccount(any(), any())(any()))
          .thenReturn(Future.successful(Some(bankAccount1)))

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map(CloseBankAccountDateKey -> date.toString("yyyy-MM-dd"), CloseBankAccountNameKey -> "Test")))

        val result = sut.submitCloseDate(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(validFormData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.bbsi.routes.BbsiCloseAccountController.checkYourAnswers(bankAccountId).url
      }
    }

    "return redirect to captureClosingInterest page" when {
      "form is supplied with a date within the current tax year" in {

        val sut = createSUT

        val date = TaxYear().start

        val validFormData = Json.obj(
          sut.closeBankAccountDateForm.DateFormDay -> date.getDayOfMonth,
          sut.closeBankAccountDateForm.DateFormMonth -> date.getMonthOfYear,
          sut.closeBankAccountDateForm.DateFormYear -> date.getYear
        )
        when(bbsiService.bankAccount(any(), any())(any()))
          .thenReturn(Future.successful(Some(bankAccount1)))

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map(CloseBankAccountDateKey -> date.toString("yyyy-MM-dd"), CloseBankAccountNameKey -> "Test")))

        val result = sut.submitCloseDate(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(validFormData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.bbsi.routes.BbsiCloseAccountController.captureClosingInterest(bankAccountId).url
      }
    }

    "return error" when {
      "form is supplied with a future date" in {

        val sut = createSUT

        val date = TaxYear().next.start

        val validFormData = Json.obj(
          sut.closeBankAccountDateForm.DateFormDay -> date.getDayOfMonth,
          sut.closeBankAccountDateForm.DateFormMonth -> date.getMonthOfYear,
          sut.closeBankAccountDateForm.DateFormYear -> date.getYear
        )
        when(bbsiService.bankAccount(any(), any())(any()))
          .thenReturn(Future.successful(Some(bankAccount1)))

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map(CloseBankAccountDateKey -> date.toString("yyyy-MM-dd"), CloseBankAccountNameKey -> "Test")))

        val result = sut.submitCloseDate(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(validFormData))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return error" when {
      "the bbsi service doesn't return an account with account number, sort code or account name" in {

        val sut = createSUT

        val validFormData = Json.obj(
          sut.closeBankAccountDateForm.DateFormDay -> "01",
          sut.closeBankAccountDateForm.DateFormMonth -> "02",
          sut.closeBankAccountDateForm.DateFormYear -> "2017"
        )
        when(journeyCacheService.currentValueAs[BigDecimal](any(), any())(any()))
          .thenReturn(Future.successful(None))
        when(bbsiService.bankAccount(any(), any())(any()))
          .thenReturn(Future.successful(Some(bankAccount1.copy(bankName = None))))

        val result = sut.captureCloseDate(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(validFormData))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "the bank account isn't found" in {

        val sut = createSUT

        val validFormData = Json.obj(
          sut.closeBankAccountDateForm.DateFormDay -> "01",
          sut.closeBankAccountDateForm.DateFormMonth -> "02",
          sut.closeBankAccountDateForm.DateFormYear -> "2017"
        )

        when(journeyCacheService.currentValueAs[BigDecimal](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(bbsiService.bankAccount(any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = sut.captureCloseDate(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(validFormData))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "capture closing interest" must {
    "show the close bank account interest form" when {
      "the request contains a valid bank account id" in {

        val sut = createSUT

        when(bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount1)))
        when(journeyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(None, None)))
        val result = sut.captureClosingInterest(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.closeBankAccount.closingInterest.heading", TaxYear().year.toString))
      }

      "the request contains a valid bank account id and we have some saved data in the form" in {

        val sut = createSUT

        val closingInterestData = Seq(Some("123"), Some("Yes"))


        when(bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount1)))
        when(journeyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(closingInterestData))

        val result = sut.captureClosingInterest(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.closeBankAccount.closingInterest.heading", TaxYear().year.toString))
      }
    }

    "return error" when {
      "the bbsi service doesn't return an account with account number, sort code or account name" in {

        val sut = createSUT

        when(journeyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq.empty[Option[String]]))
        when(bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount1.copy(bankName = None))))

        val result = sut.captureClosingInterest(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "the a bank account isn't found" in {

        val sut = createSUT

        when(journeyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq.empty[Option[String]]))
        when(bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.captureClosingInterest(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit closing interest" must {

    "cache closing interest" when {
      "the form is valid and user has entered a closing interest amount" in {

        val sut = createSUT
        val closingInterest = "123"

        val mapWithClosingInterest = Map(CloseBankAccountInterestKey -> closingInterest, CloseBankAccountInterestChoice -> YesValue)

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(mapWithClosingInterest))

        Await.result(sut.submitClosingInterest(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          ClosingInterestChoice -> YesValue, ClosingInterestEntry -> closingInterest)), 5.seconds)

        verify(journeyCacheService, times(1)).cache(Matchers.eq(mapWithClosingInterest))(any())
      }
    }

    "redirect to the 'check your answers' page" when {
      "the form is valid and user has entered a closing interest amount" in {

        val sut = createSUT
        val closingInterest = "123"

        val mapWithClosingInterest = Map(CloseBankAccountInterestKey -> closingInterest)

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(mapWithClosingInterest))

        val result = sut.submitClosingInterest(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          ClosingInterestChoice -> YesValue, ClosingInterestEntry -> closingInterest))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.bbsi.routes.BbsiCloseAccountController.checkYourAnswers(bankAccountId).url
      }
    }

    "closing interest amount is not cached" when {
      "the form is valid but user has selected that they don't have any interest" in {

        val sut = createSUT
        val closingInterest = ""

        val mapWithClosingInterest = Map(CloseBankAccountInterestKey -> closingInterest)

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(mapWithClosingInterest))

        Await.result(sut.submitClosingInterest(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          ClosingInterestChoice -> NoValue, ClosingInterestEntry -> "")), 5 seconds)

        verify(journeyCacheService, never()).cache(Matchers.eq(mapWithClosingInterest))(any())
      }
    }

    "redirect to the 'check your answers' page" when {
      "the form is valid but user has selected that they don't have any interest" in {

        val sut = createSUT
        val closingInterest = "£123.45"

        val mapWithClosingInterest = Map(CloseBankAccountInterestKey -> closingInterest)

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(mapWithClosingInterest))

        val result = sut.submitClosingInterest(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          ClosingInterestChoice -> NoValue, ClosingInterestEntry -> ""))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.bbsi.routes.BbsiCloseAccountController.checkYourAnswers(bankAccountId).url
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {

        val sut = createSUT

        val result = sut.submitClosingInterest(bankAccountId)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          ClosingInterestChoice -> YesValue, ClosingInterestEntry -> ""))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.closeBankAccount.closingInterest.heading", TaxYear().year.toString))
      }
    }
  }

  "save details in cache" when {
    "form is valid" in {

      val sut = createSUT

      val formData = Json.obj(
        sut.closeBankAccountDateForm.DateFormDay -> "01",
        sut.closeBankAccountDateForm.DateFormMonth -> "02",
        sut.closeBankAccountDateForm.DateFormYear -> "2017"
      )

      when(bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount1)))
      when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))


      val result = Await.result(sut.submitCloseDate(2)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)), 5.seconds)

      verify(journeyCacheService, times(1)).cache(Matchers.eq(Map(CloseBankAccountDateKey -> "2017-02-01", CloseBankAccountNameKey -> "Test Bank account name")))(any())
    }
  }

  "return bad request" when {
    "date is invalid" in {

      val sut = createSUT

      val year = LocalDate.now().getYear.toString

      val formData = Json.obj(
        sut.closeBankAccountDateForm.DateFormDay -> year,
        sut.closeBankAccountDateForm.DateFormMonth -> year,
        sut.closeBankAccountDateForm.DateFormYear -> year
      )
      when(bbsiService.bankAccount(any(), any())(any()))
        .thenReturn(Future.successful(Some(bankAccount1)))

      val result = sut.submitCloseDate(1)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

      status(result) mustBe BAD_REQUEST
    }
    "date is blank" in {

      val sut = createSUT

      val formData = Json.obj(
        sut.closeBankAccountDateForm.DateFormDay -> "",
        sut.closeBankAccountDateForm.DateFormMonth -> "",
        sut.closeBankAccountDateForm.DateFormYear -> ""
      )
      when(bbsiService.bankAccount(any(), any())(any()))
        .thenReturn(Future.successful(Some(bankAccount1)))

      val result = sut.submitCloseDate(1)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

      status(result) mustBe BAD_REQUEST
    }
  }
  "return internal server error" when {
    "no name provided" in {

      val sut = createSUT

      val year = LocalDate.now().getYear.toString

      val formData = Json.obj(
        sut.closeBankAccountDateForm.DateFormDay -> year,
        sut.closeBankAccountDateForm.DateFormMonth -> year,
        sut.closeBankAccountDateForm.DateFormYear -> year
      )
      when(bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount2)))

      val result = sut.submitCloseDate(1)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
    "no account provided" in {

      val sut = createSUT

      val year = LocalDate.now().getYear.toString

      val formData = Json.obj(
        sut.closeBankAccountDateForm.DateFormDay -> year,
        sut.closeBankAccountDateForm.DateFormMonth -> year,
        sut.closeBankAccountDateForm.DateFormYear -> year
      )
      when(bbsiService.bankAccount(any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = sut.submitCloseDate(1)(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }


  "Submit your answers" must {
    "send request to close the bank account" in {

      val sut = createSUT

      val closingDate = new LocalDate(2017, 10, 10)
      val closingInterest = BigDecimal(123.45)

      when(journeyCacheService.mandatoryValueAsDate(Matchers.eq(CloseBankAccountDateKey))(any())).thenReturn(Future.successful(closingDate))
      when(journeyCacheService.currentValueAs[BigDecimal](any(), any())(any())).thenReturn(Future.successful(Some(closingInterest)))

      when(bbsiService.closeBankAccount(any(), Matchers.eq(1), Matchers.eq(CloseAccountRequest(closingDate,
        Some(closingInterest))))(any())).thenReturn(Future.successful("123456"))

      when(journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = sut.submitYourAnswers(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.income.bbsi.routes.BbsiController.endConfirmation().url
      verify(journeyCacheService, times(1)).flush()(any())
    }
  }

  "Check your answers" must {
    "return ok" when {
      "bank name is present" in {

        val sut = createSUT

        when(journeyCacheService.collectedValues(any(), any())(any()))
          .thenReturn(Future.successful(Tuple2(Seq("2017-07-21"), Seq(Some("Bank account name"), Some("123.45")))))

        val result = sut.checkYourAnswers(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        verify(journeyCacheService, times(1)).collectedValues(Matchers.eq(Seq(CloseBankAccountDateKey)),
          Matchers.eq(Seq(CloseBankAccountNameKey, CloseBankAccountInterestKey)))(any())
      }

      "bank name is not present" in {
        val sut = createSUT
        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(Tuple2(Seq("2017-07-21"), Seq(None, Some("123.45")))))

        val result = sut.checkYourAnswers(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        verify(journeyCacheService, times(1)).collectedValues(Matchers.eq(Seq(CloseBankAccountDateKey)),
          Matchers.eq(Seq(CloseBankAccountNameKey, CloseBankAccountInterestKey)))(any())
      }
    }
  }

  private val nino = new Generator(new Random).nextNino
  private val bankAccountId = 1
  private val bankAccount1 = BankAccount(bankAccountId, Some("****5678"), Some("123456"), Some("Test Bank account name"), 100, None)
  private val bankAccount2 = BankAccount(bankAccountId, Some("****5678"), Some("123456"), None, 100, None)

  private def createSUT = new SUT

  private implicit val hc = HeaderCarrier()

  val bbsiService = mock[BbsiService]
  val journeyCacheService = mock[JourneyCacheService]

  private class SUT extends BbsiCloseAccountController(
    bbsiService,
    FakeAuthAction,
    FakeValidatePerson,
    journeyCacheService,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {
    val closeBankAccountDateForm = DateForm(Seq(futureDateValidation), "bankName")
  }

}
