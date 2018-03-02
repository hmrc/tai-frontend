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

import builders.UserBuilder
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http._
import uk.gov.hmrc.tai.model.TaiRoot
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.util.TaiConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ErrorPagesHandlerSpec extends PlaySpec
    with FakeTaiPlayApplication
    with I18nSupport
    with MockitoSugar {

  "ErrorPagesHandler" must {
    "return the correct service unavailable page for 400" in  {

      val sut = createSut.handleErrorResponse("testMethod", nino) (FakeRequest("POST", "/"), UserBuilder())

      val e = new BadRequestException(message = "appStatusMessage=Version number incorrect")
      val result = sut(e)

      status(result) mustBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val title = doc.select("title").text()
      title mustBe "Bad request - 400"

      val heading = doc.select(".h1-heading").text()
      heading mustBe "There is a problem"

    }

    "return the correct service unavailable page for  primary data" in  {
      val sut = createSut.handleErrorResponse("testMethod", nino) (FakeRequest("GET", "/"), UserBuilder())
      val e = new BadRequestException(message = "appStatusMessage=Primary") {override val responseCode = 400}
      val result = sut(e)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url
    }

    "return the correct service unavailable page for no data" in  {
      val sut = createSut.handleErrorResponse("testMethod", nino) (FakeRequest("GET", "/"), UserBuilder())
      val e = new BadRequestException(message = "appStatusMessage=")
      val result = sut(e)

      status(result) mustBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val title = doc.select("title").text()
      title mustBe "Bad request - 400"

      val heading = doc.select(".h1-heading").text()
      heading mustBe Messages("tai.errorMessage.heading.nps")
    }

    "return the correct service unavailable page for 404" in  {
      val sut = createSut.handleErrorResponse("testMethod", nino) (FakeRequest("GET", "/"), UserBuilder())

      val e = new Upstream4xxResponse("NotFoundException", 404, 404)

      val result = sut(e)

      status(result) mustBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val title = doc.select("title").text()
      title mustBe "Bad request - 400"

      val heading = doc.select(".h1-heading").text()
      heading mustBe "There is a problem"

    }


    "return the correct service unavailable page for 500" in  {
      val sut = createSut.handleErrorResponse("testMethod", nino) (FakeRequest("GET", "/"), UserBuilder())

      val e = new Upstream5xxResponse("Upstream5xxResponse", 500, 500)

      val result = sut(e)

      status(result) mustBe 500

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val heading = doc.select(".h1-heading").text()
      heading mustBe "Sorry we’re experiencing technical difficulties"

      val title = doc.select("title").text()
      title mustBe "Sorry, we are experiencing technical difficulties - 500"

    }

    "return the correct page where the user has no primary employments" in  {
      val sut = createSut.handleErrorResponse("testMethod", nino) (FakeRequest("GET", "/"), UserBuilder())
      val e = new BadRequestException(message="appStatusMessage=Cannot complete a Coding Calculation without a Primary Employment") {override val responseCode = 400}
      val result = sut(e)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url
    }

    "return the correct service unavailable page for not found with npm" in  {
      val sut = createSut.handleErrorResponse("testMethod", nino) (FakeRequest("GET", "/"), UserBuilder())

      val e = new NotFoundException("appStatusMessage")

      val result = sut(e)

      status(result) mustBe 404

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val heading = doc.select(".h1-heading").text()
      heading mustBe Messages("tai.errorMessage.heading.nps")

      val title = doc.select("title").text()
      title mustBe "Page not found - 404"

    }

    "return the correct service unavailable page for not found without npm" in  {
      val sut = createSut.handleErrorResponse("testMethod", nino) (FakeRequest("GET", "/"), UserBuilder())

      val result = sut(new NotFoundException(""))
      status(result) mustBe 404

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val heading = doc.select(".h1-heading").text()
      heading mustBe "There is a problem"

      val title = doc.select("title").text()
      title mustBe "Page not found - 404"

    }

    "return the correct service unavailable page for internal server error" in  {
      val sut = createSut.handleErrorResponse("testMethod", nino) (FakeRequest("GET", "/"), UserBuilder())

      val e = new InternalServerException("Internal Server Error")

      val result = sut(e)

      status(result) mustBe 500

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val heading = doc.select(".h1-heading").text()
      heading mustBe "Sorry we’re experiencing technical difficulties"

      val title = doc.select("title").text()
      title mustBe "Sorry, we are experiencing technical difficulties - 500"

    }
  }

  "Error page handler" should {
    "call fine grained error handlers" when {
      "nps Tax Account have a deceased result" in {
        val exceptionController = createSut
        val partialErrorFunction = exceptionController.npsTaxAccountDeceasedResult(FakeRequest("GET", "/"), UserBuilder(), exceptionController.recoveryLocation)
        val result = partialErrorFunction(new InternalServerException(NpsTaxAccountDeceasedMsg))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe "/check-income-tax/deceased"
      }

      "rti data is absent" in {
        val exceptionController = createSut
        val partialErrorFunction = exceptionController.rtiDataAbsentResult(FakeRequest("GET", "/"), UserBuilder(), exceptionController.recoveryLocation)
        val result = partialErrorFunction(new NotFoundException(RtiPaymentDataAbsentMsg))
        status(result) mustBe NOT_FOUND
      }

      "there are no nps result" in {
        val exceptionController = createSut
        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.npsNoEmploymentResult
        val result = partialErrorFunction(new BadRequestException(NpsNoEmploymentsRecorded))
        status(result) mustBe BAD_REQUEST
      }

      "there is hod internal server error" in {
        val exceptionController = createSut
        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.hodInternalErrorResult
        val result = partialErrorFunction(new InternalServerException("Internal server error"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "there are no tax account but employment is available for previous year" in {
        val exceptionController = createSut
        val employmentDetails = Seq(Employment("company name", Some("123"), new LocalDate("2016-05-26"), Some(new LocalDate("2016-05-26")), Nil, "", "", 2))
        val employment = (_: Nino) => Future.successful(employmentDetails)
        val proceed = (_:TaiRoot) => Future.successful(Ok)

        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.npsTaxAccountAbsentResult_withEmployCheck(employment, proceed)

        val result = partialErrorFunction(new BadRequestException(NpsTaxAccountDataAbsentMsg))
        status(result) mustBe OK
      }

      "there are no tax account and no employment is available for previous year" in {
        val exceptionController = createSut
        val employment = (_: Nino) => Future.successful(Nil)
        val proceed = (_:TaiRoot) => Future.successful(Ok)

        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.npsTaxAccountAbsentResult_withEmployCheck(employment, proceed)

        val result = partialErrorFunction(new BadRequestException(NpsTaxAccountDataAbsentMsg))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url
      }

      "there are no tax account for current year but employment is available for previous year" in {
        val exceptionController = createSut
        val employmentDetails = Seq(Employment("company name", Some("123"), new LocalDate("2016-05-26"), Some(new LocalDate("2016-05-26")), Nil, "", "", 2))
        val employment = (_: Nino) => Future.successful(employmentDetails)
        val proceed = (_:TaiRoot) => Future.successful(Ok)

        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.npsTaxAccountCYAbsentResult_withEmployCheck(employment, proceed)

        val result = partialErrorFunction(new NotFoundException(NpsTaxAccountCYDataAbsentMsg))
        status(result) mustBe OK
      }

      "there are no tax account for current year and no employment is available for previous year" in {
        val exceptionController = createSut
        val employment = (_: Nino) => Future.successful(Nil)
        val proceed = (_:TaiRoot) => Future.successful(Ok)

        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.npsTaxAccountCYAbsentResult_withEmployCheck(employment, proceed)

        val result = partialErrorFunction(new NotFoundException(NpsTaxAccountCYDataAbsentMsg))
        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe "Sorry, there is a problem so you can’t use this service"
      }

      "there are no employment for current year but employment is available for previous year" in {
        val exceptionController = createSut
        val employmentDetails = Seq(Employment("company name", Some("123"), new LocalDate("2016-05-26"), Some(new LocalDate("2016-05-26")), Nil, "", "", 2))
        val employment = (_: Nino) => Future.successful(employmentDetails)
        val proceed = (_:TaiRoot) => Future.successful(Ok)

        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.npsNoEmploymentForCYResult(employment, proceed)

        val result = partialErrorFunction(new BadRequestException(NpsNoEmploymentForCurrentTaxYear))
        status(result) mustBe OK
      }

      "there are no employment for current year and no employment is available for previous year" in {
        val exceptionController = createSut
        val employment = (_: Nino) => Future.successful(Nil)
        val proceed = (_:TaiRoot) => Future.successful(Ok)

        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.npsNoEmploymentForCYResult(employment, proceed)

        val result = partialErrorFunction(new BadRequestException(NpsNoEmploymentForCurrentTaxYear))
        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe "Sorry, there is a problem so you can’t use this service"
      }

      "there is hod bad request exception" in {
        val exceptionController = createSut
        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.hodBadRequestResult
        val result = partialErrorFunction(new BadRequestException("Bad Request Exception"))
        status(result) mustBe BAD_REQUEST
      }

      "there is any kind of exception" in {
        val exceptionController = createSut
        implicit val request = FakeRequest("GET", "/")
        implicit val user = UserBuilder()
        implicit val rl = exceptionController.recoveryLocation

        val partialErrorFunction = exceptionController.hodAnyErrorResult
        val result = partialErrorFunction(new ForbiddenException("Exception"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val nino = new Generator().nextNino

  val createSut = new SUT

  class SUT extends ErrorPagesHandler {
    override implicit def templateRenderer = MockTemplateRenderer
    override implicit def partialRetriever = MockPartialRetriever

    val recoveryLocation:RecoveryLocation = classOf[SUT]
  }

}
