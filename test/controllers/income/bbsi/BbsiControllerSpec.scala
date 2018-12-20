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

package controllers.income.bbsi

import builders.{AuthBuilder, RequestBuilder}
import controllers.FakeTaiPlayApplication
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.model.domain.{BankAccount, UntaxedInterest}
import uk.gov.hmrc.tai.service.{BbsiService, JourneyCacheService, PersonService}
import uk.gov.hmrc.tai.util.constants.BankAccountDecisionConstants

import scala.concurrent.Future
import scala.util.Random

class BbsiControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with BankAccountDecisionConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Bbsi Controller" must {

    "show bbsi details page" in {
      val sut = createSUT
      Mockito.when(sut.bbsiService.untaxedInterest(any())(any())).thenReturn(Future.successful(UntaxedInterest(0, Nil)))

      val result = sut.accounts()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.bbsi.accountDetails.heading"))
    }

    "show endConfirmation page" in {
      val sut = createSUT

      val result = sut.endConfirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.bbsi.confirmation.heading"))
    }

    "show removeConfirmation page" in {
      val sut = createSUT

      val result = sut.removeConfirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.bbsi.confirmation.heading"))
    }

    "show updateConfirmation page" in {
      val sut = createSUT

      val result = sut.updateConfirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.bbsi.confirmation.heading"))
    }

    "show overview page" in {
      val sut = createSUT
      when(sut.bbsiService.untaxedInterest(any())(any())).thenReturn(Future.successful(UntaxedInterest(2000, Nil)))

      val result = sut.untaxedInterestDetails()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.bbsi.overview.heading"))
    }

    "show internal server page" when {
      "bbsi service throws error" in {
        val sut = createSUT
        when(sut.bbsiService.untaxedInterest(any())(any())).thenReturn(Future.failed(new RuntimeException("")))

        val result = sut.untaxedInterestDetails()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("global.error.InternalServerError500.title"))
      }
    }
  }

  "Bbsi Controller - decision" must {

    "show decision page" in {
      val sut = createSUT
      when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount)))

      when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))

      val result = sut.decision(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.bbsi.decision.title", bankAccount.bankName.getOrElse("")))
    }

    "show decision page with a cached user choice pre-selected" in {
      val sut = createSUT
      when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount)))

      val cache = Some("updateInterest")

      when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(cache))

      val result = sut.decision(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.bbsi.decision.title", bankAccount.bankName.getOrElse("")))
      doc.select("input[id=bankAccountDecision-updateinterest][checked=checked]").size() mustBe 1
      doc.select("input[id=bankAccountDecision-closeaccount][checked=checked]").size() mustBe 0
      doc.select("input[id=bankAccountDecision-removeaccount][checked=checked]").size() mustBe 0
    }

    "return error" when {
      "decision page is requested but the bbsi service didn't return an account with account number, sort code or account name" in {
        val sut = createSUT
        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(emptyBankAccount)))

        when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))

        val result = sut.decision(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "decision page is requested but the bank account isn't found" in {
        val sut = createSUT
        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(None))

        when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))

        val result = sut.decision(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe NOT_FOUND
      }
    }
  }

  "Bbsi Controller - handleDecisionPage" must {

    "redirect to the start of the update interest journey" when {
      "posting the 'updateInterest' decision to handleDecisionPage" in {
        val sut = createSUT

        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount)))

        when(sut.journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.handleDecisionPage(0)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          BankAccountDecision -> UpdateInterest))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.BbsiUpdateAccountController.captureInterest(0).url
      }
    }

    "redirect to the start of the close account journey" when {
      "posting the 'closeAccount' decision to handleDecisionPage" in {
        val sut = createSUT

        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount)))

        when(sut.journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.handleDecisionPage(0)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          BankAccountDecision -> CloseAccount))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.BbsiCloseAccountController.captureCloseDate(0).url
      }
    }

    "redirect to the start of the remove account journey" when {
      "posting the 'removeAccount' decision to handleDecisionPage" in {
        val sut = createSUT

        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount)))

        when(sut.journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.handleDecisionPage(0)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          BankAccountDecision -> RemoveAccount))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.BbsiRemoveAccountController.checkYourAnswers(0).url
      }
    }

    "return something else" when {
      "there is a form validation error" in {
        val sut = createSUT

        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount)))

        when(sut.journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.handleDecisionPage(0)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          BankAccountDecision -> "somethingElseNotHandled"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.BbsiController.accounts().url
      }
    }


    "return BadRequest" when {
      "no form data is posted" in {
        val sut = createSUT

        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount)))

        val result = sut.handleDecisionPage(0)(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bbsi.decision.title", bankAccount.bankName.getOrElse("")))
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT
        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(bankAccount)))

        val result = sut.handleDecisionPage(0)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          BankAccountDecision -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bbsi.decision.title", bankAccount.bankName.getOrElse("")))
      }
    }

    "return RuntimeException" when {
      "A bank account which doesn't meet the bank account match criteria is returned from the bbsi service bank account call" in {
        val sut = createSUT
        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(Some(emptyBankAccount)))

        val result = sut.handleDecisionPage(0)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          BankAccountDecision -> ""))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return RuntimeException" when {
      "None is returned from the bbsi service bank account call" in {
        val sut = createSUT

        when(sut.bbsiService.bankAccount(any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.handleDecisionPage(0)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          BankAccountDecision -> ""))
        status(result) mustBe NOT_FOUND
      }
    }
  }

  def createSUT = new SUT

  val nino = new Generator(new Random).nextNino

  val emptyBankAccount: BankAccount = BankAccount(1, None, None, None, 123.4, None)
  val bankAccount: BankAccount = BankAccount(0, Some("0"), Some("0"), Some("TestBank"), 0, None)

  val personService: PersonService = mock[PersonService]

  class SUT extends BbsiController(
    mock[BbsiService],
    personService,
    mock[AuditConnector],
    mock[DelegationConnector],
    mock[AuthConnector],
    mock[JourneyCacheService],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    val ad: Future[Some[Authority]] = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

  }

}
