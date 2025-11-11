/*
 * Copyright 2023 HM Revenue & Customs
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
import cats.data.EitherT
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.viewModels.incomeTaxHistory.IncomeTaxYear
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, Employment}
import uk.gov.hmrc.tai.service.{EmploymentService, RtiService, TaxAccountService}
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import utils.{AuthenticatedRequestFixture, BaseSpec, TaxAccountSummaryTestData}
import views.html.incomeTaxHistory.IncomeTaxHistoryView

import scala.concurrent.Future

class IncomeTaxHistoryControllerSpec extends BaseSpec with TaxAccountSummaryTestData with JsoupMatchers {

  val numberOfPreviousYearsToShowIncomeTaxHistory: Int = 5
  val totalInvocations: Int                            = numberOfPreviousYearsToShowIncomeTaxHistory + 1
  val employmentService: EmploymentService             = mock[EmploymentService]
  val taxAccountService: TaxAccountService             = mock[TaxAccountService]
  val rtiService: RtiService                           = mock[RtiService]
  override def beforeEach(): Unit                      = {
    super.beforeEach()
    reset(taxAccountService, employmentService, rtiService)
    when(rtiService.getAllPaymentsForYear(any(), any())(any()))
      .thenReturn(
        EitherT(Future.successful[Either[UpstreamErrorResponse, Seq[AnnualAccount]]](Right(Nil)))
      )
  }

  class TestController
      extends IncomeTaxHistoryController(
        appConfig,
        mockAuthJourney,
        inject[IncomeTaxHistoryView],
        mcc,
        taxAccountService,
        employmentService,
        rtiService
      )

  implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")
  implicit val authRequest: AuthenticatedRequest[AnyContent]    =
    AuthenticatedRequestFixture.buildUserRequest(request)

  val taxYears: List[TaxYear] =
    (TaxYear().year to (TaxYear().year - numberOfPreviousYearsToShowIncomeTaxHistory) by -1).map(TaxYear(_)).toList

  "onPageLoad" must {
    "display the income tax history page" when {
      "employment data is returned" in {

        for (_ <- taxYears) {

          when(taxAccountService.taxCodeIncomes(any(), any())(any())) thenReturn Future.successful(
            Right(Seq(taxCodeIncome))
          )

          when(employmentService.employmentsOnly(any(), any())(any())) thenReturn EitherT
            .rightT[Future, UpstreamErrorResponse](
              Seq(empEmployment1, empEmployment2)
            )
        }

        val controller = new TestController
        val result     = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomeTax.history.pageTitle"))

        verify(employmentService, times(totalInvocations)).employmentsOnly(any(), any())(any())
        verify(taxAccountService, times(totalInvocations)).taxCodeIncomes(any(), any())(any())

      }

      "pension data is returned" in {
        for (_ <- taxYears) {

          when(taxAccountService.taxCodeIncomes(any(), any())(any())) thenReturn Future.successful(
            Right(Seq(taxCodeIncome))
          )
          when(employmentService.employmentsOnly(any(), any())(any())) thenReturn EitherT
            .rightT[Future, UpstreamErrorResponse](
              Seq(pensionEmployment3, pensionEmployment4)
            )
        }

        val controller = new TestController
        val result     = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomeTax.history.pageTitle"))

        verify(taxAccountService, times(totalInvocations)).taxCodeIncomes(any(), any())(any())
        verify(employmentService, times(totalInvocations)).employmentsOnly(any(), any())(any())

      }

      "tax code is empty if the tax account can't be found" in {
        for (_ <- taxYears) {
          when(taxAccountService.taxCodeIncomes(any(), any())(any())) thenReturn Future.successful(Left("not found"))
          when(employmentService.employmentsOnly(any(), any())(any())) thenReturn EitherT
            .rightT[Future, UpstreamErrorResponse](
              Seq(pensionEmployment3, pensionEmployment4)
            )
        }

        val controller = new TestController
        val result     = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().text() must include(Messages("tai.incomeTax.history.unavailable"))
      }

      "tax code is empty if the tax account throws an exception" in {
        for (_ <- taxYears) {
          when(taxAccountService.taxCodeIncomes(any(), any())(any())) thenReturn Future.failed(
            new Exception("exception")
          )
          when(employmentService.employmentsOnly(any(), any())(any())) thenReturn EitherT
            .rightT[Future, UpstreamErrorResponse](
              Seq(pensionEmployment3, pensionEmployment4)
            )
        }

        val controller = new TestController
        val result     = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().text() must include(Messages("tai.incomeTax.history.unavailable"))
      }
    }
  }

  "getIncomeTaxYear" must {

    "return an empty tax code if there isn't one" in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any())) thenReturn Future.successful(Right(Seq()))
      when(employmentService.employmentsOnly(any(), any())(any())) thenReturn EitherT
        .rightT[Future, UpstreamErrorResponse](Seq(empEmployment1))

      val controller = new TestController
      val result     = controller.getIncomeTaxYear(nino, TaxYear())(authRequest)

      result
        .getOrElse(IncomeTaxYear(TaxYear(1), List.empty))
        .map(_.incomeTaxHistory.map(_.maybeTaxCode))
        .futureValue mustBe List(None)
    }

    "return an empty tax code if the taxAccountService fails to retrieve" in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any())) thenReturn Future.failed(new Exception("exception"))
      when(employmentService.employmentsOnly(any(), any())(any())) thenReturn EitherT
        .rightT[Future, UpstreamErrorResponse](Seq(empEmployment1))

      val controller = new TestController
      val result     = controller.getIncomeTaxYear(nino, TaxYear())(authRequest)

      result
        .getOrElse(IncomeTaxYear(TaxYear(1), List.empty))
        .map(_.incomeTaxHistory.map(_.maybeTaxCode))
        .futureValue mustBe List(None)
    }

    "display the income tax history page with no tax history message" when {
      "given taxYear returns no data" in {

        for (_ <- taxYears) {

          when(taxAccountService.taxCodeIncomes(any(), any())(any())) thenReturn Future.successful(Left(""))
          when(employmentService.employmentsOnly(any(), any())(any())) thenReturn EitherT
            .leftT[Future, Seq[Employment]](
              UpstreamErrorResponse("", 500, 500, Map.empty)
            )
        }

        val controller = new TestController
        val result     = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomeTax.history.pageTitle"))
        doc         must haveParagraphWithText(Messages("tai.incomeTax.history.noTaxHistory"))

        verify(taxAccountService, times(totalInvocations))
          .taxCodeIncomes(any(), any())(any())
        verify(employmentService, times(totalInvocations))
          .employmentsOnly(any(), any())(any())

      }
    }

    "return a tax code if it's returned by the taxAccountService" in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any())) thenReturn Future.successful(
        Right(Seq(taxCodeIncome))
      )
      when(employmentService.employmentsOnly(any(), any())(any())) thenReturn EitherT
        .rightT[Future, UpstreamErrorResponse](Seq(empEmployment1))

      val controller = new TestController
      val result     = controller.getIncomeTaxYear(nino, TaxYear())(authRequest)

      result
        .getOrElse(IncomeTaxYear(TaxYear(1), List.empty))
        .map(_.incomeTaxHistory.map(_.maybeTaxCode))
        .futureValue mustBe List(Some(taxCodeIncome.taxCode))
    }

    "return an error when " in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any())) thenReturn Future.successful(
        Right(Seq(taxCodeIncome))
      )
      when(employmentService.employmentsOnly(any(), any())(any())) thenReturn EitherT
        .rightT[Future, UpstreamErrorResponse](Seq(empEmployment1))

      val controller = new TestController
      val result     = controller.getIncomeTaxYear(nino, TaxYear())(authRequest)

      result
        .getOrElse(IncomeTaxYear(TaxYear(1), List.empty))
        .map(_.incomeTaxHistory.map(_.maybeTaxCode))
        .futureValue mustBe List(Some(taxCodeIncome.taxCode))
    }
  }
}
