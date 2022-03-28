/*
 * Copyright 2022 HM Revenue & Customs
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
import java.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadRequestException, HttpException, InternalServerException, NotFoundException}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, Available, Employment, Monthly, Payment, TemporarilyUnavailable}
import uk.gov.hmrc.tai.service.{EmploymentService, TaxCodeChangeService}
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import utils.BaseSpec
import views.html.paye.{HistoricPayAsYouEarnView, RtiDisabledHistoricPayAsYouEarnView}

import scala.concurrent.Future

class PayeControllerHistoricSpec
    extends BaseSpec with JsoupMatchers with ControllerViewTestHelper with BeforeAndAfterEach {

  override def beforeEach: Unit =
    Mockito.reset(employmentService)

  "Calling the payePage method with an authorised session" must {

    "redirect to the last year page successfully" when {
      "calling through static url" in {
        val testController = createTestController()

        val result = testController.lastYearPaye()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER

        redirectLocation(result).getOrElse("") mustBe controllers.routes.PayeControllerHistoric
          .payePage(TaxYear().prev)
          .url
      }
    }

    "display the last year paye page successfully " in {
      val testController = createTestController()

      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")
      when(employmentService.employments(any(), any())(any()))
        .thenReturn(Future.successful(sampleEmployment))

      val result = testController.payePage(TaxYear().prev)(request)

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc must haveParagraphWithText("£0.00")
    }

    "display the last year paye page successfully when RTI is down" in {

      val testController = createTestController()
      when(employmentService.employments(any(), any())(any()))
        .thenReturn(Future.successful(sampleEmploymentForRtiUnavailable))

      val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      contentAsString(result) must include(messages("tai.rti.down"))
    }

    "Redirect to the paye controller" when {

      "the supplied year relates to current tax year" in {

        val testController = createTestController()

        val result = testController.payePage(TaxYear())(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
      }

      "the supplied year is in advance of this tax year" in {

        val testController = createTestController()

        val result = testController.payePage(TaxYear().next)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
      }
    }

    "return ok and use last FPS(Full payment submission) for total taxable income when there are multiple same date submissions" in {

      val testController = createTestController()

      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")
      when(employmentService.employments(any(), any())(any()))
        .thenReturn(Future.successful(sampleEmploymentWithSameDatFpsSubmissions))

      val result = testController.payePage(TaxYear().prev)(request)

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc must haveParagraphWithText("£555,555.00")

    }

    "display an error page" when {
      "employment service call results in a NotFoundException from NPS" in {

        val testController = createTestController()
        when(employmentService.employments(any(), any())(any()))
          .thenReturn(Future.failed(new NotFoundException("appStatusMessage : not found")))

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
        when(employmentService.employments(any(), any())(any()))
          .thenReturn(Future.failed(new NotFoundException("not found")))

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
        when(employmentService.employments(any(), any())(any()))
          .thenReturn(Future.failed(new BadRequestException("Bad request")))

        val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Bad request - 400")
        doc must haveHeadingWithText(Messages("tai.errorMessage.heading"))
        doc must haveParagraphWithText(Messages("tai.errorMessage.frontend400.message1"))
      }

      "employment service call results in a internal server error" in {

        val testController = createTestController()
        when(employmentService.employments(any(), any())(any()))
          .thenReturn(Future.failed(new InternalServerException("Internal server error")))

        val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Sorry, there is a problem with the service")
        doc must haveHeadingWithText(Messages("tai.technical.error.heading"))
        doc must haveParagraphWithText(Messages("tai.technical.error.message"))
      }

      "employment service call results in an exception" in {

        val testController = createTestController()
        when(employmentService.employments(any(), any())(any()))
          .thenReturn(Future.failed(new HttpException("error", 502)))

        val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Sorry, there is a problem with the service")
        doc must haveHeadingWithText(Messages("tai.technical.error.heading"))
        doc must haveParagraphWithText(Messages("tai.technical.error.message"))
      }

      "payePage call when employee sequence is empty " in {

        val testController = createTestController()
        when(employmentService.employments(any(), any())(any()))
          .thenReturn(Future.successful(sampleEmptyEmployment))

        val result = testController.payePage(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }
  }

  def createTestController(employments: Seq[Employment] = Nil, showTaxCodeDescriptionLink: Boolean = false) =
    new PayeControllerHistoricTest(employments, showTaxCodeDescriptionLink)

  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val employmentService: EmploymentService = mock[EmploymentService]

  class PayeControllerHistoricTest(employments: Seq[Employment], showTaxCodeDescriptionLink: Boolean)
      extends PayeControllerHistoric(
        appConfig,
        taxCodeChangeService,
        employmentService,
        FakeAuthAction,
        FakeValidatePerson,
        mcc,
        inject[RtiDisabledHistoricPayAsYouEarnView],
        inject[HistoricPayAsYouEarnView],
        templateRenderer,
        inject[ErrorPagesHandler]
      ) {

    when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(employments))
    when(taxCodeChangeService.hasTaxCodeRecordsInYearPerEmployment(any(), any())(any()))
      .thenReturn(Future.successful(showTaxCodeDescriptionLink))
  }

  val sampleEmptyEmployment = Seq(
    )

  val sampleEmploymentForEmptyAnnualAccounts = Seq(
    Employment(
      "employer1",
      Live,
      None,
      LocalDate.of(2016, 6, 9),
      None,
      Seq(),
      "taxNumber",
      "payeNumber",
      1,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    ),
    Employment(
      "employer2",
      Live,
      None,
      LocalDate.of(2016, 7, 9),
      None,
      Seq(),
      "taxNumber",
      "payeNumber",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    )
  )

  val sampleEmploymentForRtiUnavailable = Seq(
    Employment(
      "employer1",
      Live,
      None,
      LocalDate.of(2016, 6, 9),
      None,
      Seq(AnnualAccount(TaxYear().prev, TemporarilyUnavailable, Nil, Nil)),
      "taxNumber",
      "payeNumber",
      1,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    ),
    Employment(
      "employer2",
      Live,
      None,
      LocalDate.of(2016, 7, 9),
      None,
      Seq(AnnualAccount(TaxYear().prev, TemporarilyUnavailable, Nil, Nil)),
      "taxNumber",
      "payeNumber",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    )
  )

  val sampleEmployment = Seq(
    Employment(
      "employer1",
      Live,
      None,
      LocalDate.of(2018, 6, 9),
      None,
      Seq(AnnualAccount(TaxYear().prev, Available, Nil, Nil)),
      "taxNumber",
      "payeNumber",
      1,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    ),
    Employment(
      "employer2",
      Live,
      None,
      LocalDate.of(2017, 7, 9),
      None,
      Seq(AnnualAccount(TaxYear().prev, Available, Nil, Nil)),
      "taxNumber",
      "payeNumber",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    )
  )

  val payment1 = Payment(LocalDate.parse("2019-04-05"), 333333, 111, 111, 111, 111, 111, Monthly)
  val payment2 = Payment(LocalDate.parse("2019-04-05"), 444444, 111, 111, 111, 111, 111, Monthly)
  val payment3 = Payment(LocalDate.parse("2019-04-05"), 555555, 111, 111, 111, 111, 111, Monthly)
  val payments = Seq(payment1, payment2, payment3)

  val sampleEmploymentWithSameDatFpsSubmissions = Seq(
    Employment(
      "employer1",
      Live,
      None,
      LocalDate.of(2018, 6, 9),
      None,
      Seq(AnnualAccount(TaxYear().prev, Available, payments, Nil)),
      "taxNumber",
      "payeNumber",
      1,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    ),
    Employment(
      "employer2",
      Live,
      None,
      LocalDate.of(2017, 7, 9),
      None,
      Seq(AnnualAccount(TaxYear().prev, Available, payments, Nil)),
      "taxNumber",
      "payeNumber",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    )
  )

}
