/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.auth.AuthenticatedRequest
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import play.api.mvc.Results.{BadRequest, Redirect}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.tai.connectors.responses.TaiTaxAccountFailureResponse
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.util.constants.TaiConstants._
import utils.BaseSpec
import views.html.ErrorNoPrimary

import scala.concurrent.Future

class ErrorPagesHandlerSpec extends BaseSpec {

  private lazy val error_no_primary: ErrorNoPrimary = inject[ErrorNoPrimary]
  private val ninoValue = nino.value
  private val createSut = inject[ErrorPagesHandler]

  "ErrorPagesHandler" must {
    "handle an internal server error" in {
      val controller = createSut
      implicit val request = FakeRequest("GET", "/")
      val result = Future.successful(controller.internalServerError("bad"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return the correct service unavailable page for 400" in {

      val sut = createSut.handleErrorResponse("testMethod", nino)(FakeRequest("POST", "/"), messages)

      val e = new BadRequestException(message = "appStatusMessage=Version number incorrect")
      val result = sut(e)

      status(result) mustBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val title = doc.select("title").text()
      title must include("Bad request - 400")

      val heading = doc.select(".h1-heading").text()
      heading mustBe "There is a problem"

    }

    "return the correct service unavailable page for  primary data" in {
      val sut = createSut.handleErrorResponse("testMethod", nino)(FakeRequest("GET", "/"), messages)
      val e = new BadRequestException(message = "appStatusMessage=Primary") { override val responseCode = 400 }
      val result = sut(e)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url
    }

    "return the correct service unavailable page for no data" in {
      val sut = createSut.handleErrorResponse("testMethod", nino)(FakeRequest("GET", "/"), messages)
      val e = new BadRequestException(message = "appStatusMessage=")
      val result = sut(e)

      status(result) mustBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val title = doc.select("title").text()
      title must include("Bad request - 400")

      val heading = doc.select(".h1-heading").text()
      heading mustBe Messages("tai.errorMessage.heading.nps")
    }

    "return the correct service unavailable page for 404" in {
      val sut = createSut.handleErrorResponse("testMethod", nino)(FakeRequest("GET", "/"), messages)

      val e = new Upstream4xxResponse("NotFoundException", 404, 404)

      val result = sut(e)

      status(result) mustBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val title = doc.select("title").text()
      title must include("Bad request - 400")

      val heading = doc.select(".h1-heading").text()
      heading mustBe "There is a problem"

    }

    "return the correct service unavailable page for 500" in {
      val sut = createSut.handleErrorResponse("testMethod", nino)(FakeRequest("GET", "/"), messages)

      val e = new Upstream5xxResponse("Upstream5xxResponse", 500, 500)

      val result = sut(e)

      status(result) mustBe 500

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val heading = doc.select(".h1-heading").text()
      heading mustBe "Sorry, there is a problem with the service"

      val title = doc.select("title").text()
      title must include("Sorry, there is a problem with the service")

    }

    "return the correct page where the user has no primary employments" in {
      val sut = createSut.handleErrorResponse("testMethod", nino)(FakeRequest("GET", "/"), messages)
      val e = new BadRequestException(
        message = "appStatusMessage=Cannot complete a Coding Calculation without a Primary Employment") {
        override val responseCode = 400
      }
      val result = sut(e)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url
    }

    "return the correct service unavailable page for not found with npm" in {
      val sut = createSut.handleErrorResponse("testMethod", nino)(FakeRequest("GET", "/"), messages)

      val e = new NotFoundException("appStatusMessage")

      val result = sut(e)

      status(result) mustBe 404

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val heading = doc.select(".h1-heading").text()
      heading mustBe Messages("tai.errorMessage.heading.nps")

      val title = doc.select("title").text()
      title must include("Page not found - 404")

    }

    "return the correct service unavailable page for not found without npm" in {
      val sut = createSut.handleErrorResponse("testMethod", nino)(FakeRequest("GET", "/"), messages)

      val result = sut(new NotFoundException(""))
      status(result) mustBe 404

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val heading = doc.select(".h1-heading").text()
      heading mustBe "There is a problem"

      val title = doc.select("title").text()
      title must include("Page not found - 404")

    }

    "return the correct service unavailable page for internal server error" in {
      val sut = createSut.handleErrorResponse("testMethod", nino)(FakeRequest("GET", "/"), messages)

      val e = new InternalServerException("Internal Server Error")

      val result = sut(e)

      status(result) mustBe 500

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val heading = doc.select(".h1-heading").text()
      heading mustBe "Sorry, there is a problem with the service"

      val title = doc.select("title").text()
      title must include("Sorry, there is a problem with the service")

    }
  }

  "The fine grained partial functions provided by ErrorPageHandler" should {

    implicit val request = FakeRequest("GET", "/")
    implicit val user = UserBuilder()
    implicit val recoveryLocation = classOf[ErrorPagesHandler]

    "Identify nps tax account failures, and generate an appropriate redirect" when {

      "nps tax account responds with a deceased nessage" in {
        val handler = createSut
        val partialErrorFunction = handler.npsTaxAccountDeceasedResult(ninoValue)
        val result = partialErrorFunction(TaiTaxAccountFailureResponse(NpsTaxAccountDeceasedMsg))
        result mustBe Some(Redirect(routes.DeceasedController.deceased()))
      }

      "nps tax account responds with a 'no employments recorded for this individual' message" in {
        val handler = createSut
        val partialErrorFunction = handler.npsNoEmploymentResult(ninoValue)
        val result = partialErrorFunction(TaiTaxAccountFailureResponse(NpsNoEmploymentsRecorded))
        result mustBe Some(BadRequest(error_no_primary()))
      }

      "nps tax account responds with a 'no primary employment' message (data is absent), but employment data is available for previous year" in {
        val handler = createSut
        val employmentDetails = Seq(
          Employment(
            "company name",
            Live,
            Some("123"),
            new LocalDate("2016-05-26"),
            Some(new LocalDate("2016-05-26")),
            Nil,
            "",
            "",
            2,
            None,
            false,
            false))
        val partialErrorFunction = handler.npsTaxAccountAbsentResult_withEmployCheck(employmentDetails, ninoValue)
        val result = partialErrorFunction(TaiTaxAccountFailureResponse(NpsTaxAccountDataAbsentMsg))
        result mustBe None
      }

      "nps tax account responds with a 'no primary employment' message (data is absent), and no employment data is available for previous year" in {
        val handler = createSut
        val partialErrorFunction = handler.npsTaxAccountAbsentResult_withEmployCheck(Nil, ninoValue)
        val result = partialErrorFunction(TaiTaxAccountFailureResponse(NpsTaxAccountDataAbsentMsg))
        result mustBe Some(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
      }

      "nps tax account responds with a 'no tax account information' message (data is absent), but employment data is available for previous year" in {
        val handler = createSut
        val employmentDetails = Seq(
          Employment(
            "company name",
            Live,
            Some("123"),
            new LocalDate("2016-05-26"),
            Some(new LocalDate("2016-05-26")),
            Nil,
            "",
            "",
            2,
            None,
            false,
            false))
        val partialErrorFunction = handler.npsTaxAccountCYAbsentResult_withEmployCheck(employmentDetails, ninoValue)
        val result = partialErrorFunction(TaiTaxAccountFailureResponse(NpsTaxAccountCYDataAbsentMsg))
        result mustBe None
      }

      "nps tax account responds with a 'no tax account information' message (data is absent), and no employment data is available for previous year" in {
        val handler = createSut
        val partialErrorFunction = handler.npsTaxAccountCYAbsentResult_withEmployCheck(Nil, ninoValue)
        val result = partialErrorFunction(TaiTaxAccountFailureResponse(NpsTaxAccountCYDataAbsentMsg))
        result mustBe Some(BadRequest(error_no_primary()))
      }

      "nps tax account responds with a 'no employments recorded for current tax year' message, but employment data is available for previous year" in {
        val exceptionController = createSut
        val employmentDetails = Seq(
          Employment(
            "company name",
            Live,
            Some("123"),
            new LocalDate("2016-05-26"),
            Some(new LocalDate("2016-05-26")),
            Nil,
            "",
            "",
            2,
            None,
            false,
            false))
        val partialErrorFunction =
          exceptionController.npsNoEmploymentForCYResult_withEmployCheck(employmentDetails, ninoValue)
        val result = partialErrorFunction(TaiTaxAccountFailureResponse(NpsNoEmploymentForCurrentTaxYear))
        result mustBe None
      }

      "nps tax account responds with a 'no employments recorded for current tax year' message, and no employment data is available for previous year" in {
        val exceptionController = createSut
        val partialErrorFunction = exceptionController.npsNoEmploymentForCYResult_withEmployCheck(Nil, ninoValue)
        val result = partialErrorFunction(TaiTaxAccountFailureResponse(NpsNoEmploymentForCurrentTaxYear))
        result mustBe Some(BadRequest(error_no_primary()))
      }
    }

    "Identify general exceptions, route to appropriate error page" when {

      "there is hod internal server error" in {
        val exceptionController = createSut

        implicit val request = AuthenticatedRequest[AnyContent](fakeRequest, authedUser, "name")

        val partialErrorFunction = exceptionController.hodInternalErrorResult(ninoValue)
        val result = partialErrorFunction(new InternalServerException("Internal server error"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "there is hod bad request exception" in {
        val exceptionController = createSut
        implicit val request = AuthenticatedRequest[AnyContent](fakeRequest, authedUser, "name")

        val partialErrorFunction = exceptionController.hodBadRequestResult(ninoValue)
        val result = partialErrorFunction(new BadRequestException("Bad Request Exception"))
        status(result) mustBe BAD_REQUEST
      }

      "there is any kind of exception" in {
        val exceptionController = createSut
        implicit val request = AuthenticatedRequest[AnyContent](FakeRequest("GET", "/"), authedUser, "name")

        val partialErrorFunction = exceptionController.hodAnyErrorResult(ninoValue)
        val result = partialErrorFunction(new ForbiddenException("Exception"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
