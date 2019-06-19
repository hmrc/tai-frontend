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

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{BadRequestException, HttpException, InternalServerException, NotFoundException}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.service.{EmploymentService, TaxCodeChangeService}
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import uk.gov.hmrc.tai.viewModels.HistoricPayAsYouEarnViewModel
import views.html.paye.historicPayAsYouEarn

import scala.concurrent.Future
import scala.util.Random

class PayeControllerHistoricSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport
  with JsoupMatchers
  with ControllerViewTestHelper
  with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    Mockito.reset(employmentService)
  }

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  private val currentYear: Int = TaxYear().year
  private val cyMinusOneTaxYear: TaxYear = TaxYear(currentYear - 1)

  "Calling the payePage method with an authorised session" must {

    "redirect to the last year page successfully" when {
      "calling through static url" in {
        val testController = createTestController()

        val result = testController.lastYearPaye()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER

        redirectLocation(result).getOrElse("") mustBe controllers.routes.PayeControllerHistoric.payePage(TaxYear().prev).url
      }
    }

    "display the last year paye page successfully " in {
      val testController = createTestController()

      implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

      val result = testController.payePage(TaxYear().prev)(request)

      status(result) mustBe OK

      val viewModel = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Seq.empty[Employment], true)

      result rendersTheSameViewAs historicPayAsYouEarn(viewModel, 3)
    }

    "Redirect to the paye controller" when {

      "the supplied year relates to current tax year" in {

        val testController = createTestController()

        val result = testController.payePage(TaxYear())(RequestBuilder.buildFakeRequestWithAuth("GET"))

        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
      }

      "the supplied year is in advance of this tax year" in {

        val testController = createTestController()

        val result = testController.payePage(TaxYear().next)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
      }
    }

    "display an error page" when {
      "employment service call results in a NotFoundException from NPS" in {

        val testController = createTestController()
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.failed(new NotFoundException("appStatusMessage : not found")))

        val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe NOT_FOUND
        verify(employmentService, times(1)).employments(any(), any())(any())
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Page not found - 404")
        doc must haveHeadingWithText(Messages("tai.errorMessage.heading.nps"))
        doc must haveParagraphWithText(Messages("tai.errorMessage.frontend400.message1.nps"))
        doc must haveParagraphWithText(Messages("tai.errorMessage.frontend400.message2.nps"))
      }

      "employment service call results in a NotFoundException from RTI" in {

        val testController = createTestController()
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.failed(new NotFoundException("not found")))

        val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe NOT_FOUND
        verify(employmentService, times(1)).employments(any(), any())(any())
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Page not found - 404")
        doc must haveHeadingWithText(Messages("tai.errorMessage.heading"))
        doc must haveParagraphWithText(Messages("tai.errorMessage.frontend400.message1"))
      }

      "employment service call results in a bad request" in {

        val testController = createTestController()
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.failed(new BadRequestException("Bad request")))

        val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Bad request - 400")
        doc must haveHeadingWithText(Messages("tai.errorMessage.heading"))
        doc must haveParagraphWithText(Messages("tai.errorMessage.frontend400.message1"))
      }

      "employment service call results in a internal server error" in {

        val testController = createTestController()
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.failed(new InternalServerException("Internal server error")))

        val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Sorry, we are experiencing technical difficulties - 500")
        doc must haveHeadingWithText(Messages("tai.technical.error.heading"))
        doc must haveParagraphWithText(Messages("tai.technical.error.message"))
      }

      "employment service call results in an exception" in {

        val testController = createTestController()
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.failed(new HttpException("error", 502)))

        val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Sorry, we are experiencing technical difficulties - 500")
        doc must haveHeadingWithText(Messages("tai.technical.error.heading"))
        doc must haveParagraphWithText(Messages("tai.technical.error.message"))
      }
    }
  }

  val fakeNino = new Generator(new Random).nextNino

  def createTestController(employments: Seq[Employment] = Nil, previousYears: Int = 3, showTaxCodeDescriptionLink: Boolean = false) = {
    new PayeControllerHistoricTest(employments, previousYears, showTaxCodeDescriptionLink)
  }

  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val employmentService = mock[EmploymentService]

  class PayeControllerHistoricTest(employments: Seq[Employment],
                                   previousYears: Int,
                                   showTaxCodeDescriptionLink: Boolean) extends PayeControllerHistoric(
    mock[ApplicationConfig],
    taxCodeChangeService,
    employmentService,
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {
    override val numberOfPreviousYearsToShow: Int = previousYears

    when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(employments))
    when(taxCodeChangeService.hasTaxCodeRecordsInYearPerEmployment(any(), any())(any())).thenReturn(Future.successful(showTaxCodeDescriptionLink))
  }

}
