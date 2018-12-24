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
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.model.domain.{BankAccount, UntaxedInterest}
import uk.gov.hmrc.tai.service.{BbsiService, JourneyCacheService, PersonService}

import scala.concurrent.Future
import scala.util.Random

class BbsiUpdateAccountControllerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Bbsi Update controller" must {
    "show capture interest form" when {
      "valid details has been passed" in {
        val sut = createSut

        when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))

        val result = sut.captureInterest(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bbsi.update.captureInterest.title", "TEST"))

        doc.select("input[id=untaxedInterest]").get(0).attributes().get("value") mustBe ""
      }

      "valid details has been passed and values have been previously cached" in {
        val sut = createSut

        when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(Some("10234")))

        val result = sut.captureInterest(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bbsi.update.captureInterest.title", "TEST"))

        doc.select("input[id=untaxedInterest]").get(0).attributes().get("value") mustBe "10234"
      }
    }

    "throws internal server error" when {
      "details are invalid" in {
        val sut = createSut

        when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))

        val result = sut.captureInterest(2)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "Submit interest" must {
    "redirect to check your answers page" when {
      "form is valid" in {
        val sut = createSut
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitInterest(1)(RequestBuilder.buildFakeRequestWithAuth("POST").
          withFormUrlEncodedBody(("untaxedInterest", "100")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.bbsi.routes.BbsiUpdateAccountController.checkYourAnswers(1).url
      }
    }

    "return bad request" when {
      "form is invalid" in {
        val sut = createSut

        val result = sut.submitInterest(1)(RequestBuilder.buildFakeRequestWithAuth("POST").
          withFormUrlEncodedBody(("untaxedInterest", "")))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return Internal Server Error" when {
      "account doesn't exist" in {
        val sut = createSut
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitInterest(2)(RequestBuilder.buildFakeRequestWithAuth("POST").
          withFormUrlEncodedBody(("untaxedInterest", "100")))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "check you answers" must {
    "return OK" when {
      "details are valid" in {
        val sut = createSut
        when(sut.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq("1,000", "TEST")))

        val result = sut.checkYourAnswers(1)(RequestBuilder.buildFakeInvalidRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }
  }

  "submit your answers" must {
    "redirect to confirmation page" in {
      val sut = createSut
      when(sut.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq("1,000", "TEST")))
      when(bbsiService.updateBankAccountInterest(any(), any(), any())(any())).thenReturn(Future.successful("123-456-789"))
      when(sut.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = sut.submitYourAnswers(1)(RequestBuilder.buildFakeInvalidRequestWithAuth("POST"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.income.bbsi.routes.BbsiController.updateConfirmation().url

      verify(sut.journeyCacheService, times(1)).flush()(any())
    }
  }

  def createSut = new SUT

  private val nino = new Generator(new Random).nextNino
  private implicit val hc = HeaderCarrier()
  val personService: PersonService = mock[PersonService]
  val bbsiService = mock[BbsiService]

  class SUT extends BbsiUpdateAccountController(
    bbsiService,
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
    when(bbsiService.untaxedInterest(any())(any())).thenReturn(Future.successful(UntaxedInterest(1000,
      Seq(BankAccount(1, Some("1231231"), Some("123456"), Some("TEST"), 1000, Some("customer"))))))
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
  }

}
