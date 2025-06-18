/*
 * Copyright 2025 HM Revenue & Customs
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
import cats.instances.future.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadRequestException, UpstreamErrorResponse}
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.service.{CodingComponentService, PersonService, TaxAccountService}
import utils.BaseSpec
import views.html.estimatedIncomeTax.DetailedIncomeTaxEstimateView

import scala.concurrent.Future

class DetailedIncomeTaxEstimateControllerSpec extends BaseSpec {

  val personService: PersonService                   = mock[PersonService]
  val codingComponentService: CodingComponentService = mock[CodingComponentService]
  val taxAccountService: TaxAccountService           = mock[TaxAccountService]

  def sut =
    new DetailedIncomeTaxEstimateController(
      taxAccountService,
      codingComponentService,
      mockAuthJourney,
      mcc,
      inject[DetailedIncomeTaxEstimateView],
      inject[ErrorPagesHandler]
    )

  when(taxAccountService.totalTax(any(), any())(any()))
    .thenReturn(Future.successful(TotalTax(0, Seq.empty, None, None, None)))
  when(taxAccountService.taxCodeIncomes(any(), any())(any()))
    .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))
  when(taxAccountService.taxAccountSummary(any(), any())(any()))
    .thenReturn(EitherT.rightT(TaxAccountSummary(0, 0, 0, 0, 0)))
  when(taxAccountService.nonTaxCodeIncomes(any(), any())(any()))
    .thenReturn(Future.successful(NonTaxCodeIncome(None, Seq.empty)))
  when(codingComponentService.taxFreeAmountComponents(any(), any())(any()))
    .thenReturn(Future.successful(Seq.empty))

  "Detailed Income Tax Estimate Controller" must {
    "return OK when responses are " when {
      "there are bands present" in {
        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
      }
    }

    "return Internal server error" when {
      "fetch total tax details fails" in {
        when(taxAccountService.totalTax(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("testFailure")))
        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "fetch tax code incomes fails" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("testFailure")))
        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "fetch tax account summary fails" in {
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR)))
        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "fetch of non-tax code incomes fails" in {
        when(taxAccountService.nonTaxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.failed(new BadRequestException("testFailure")))
        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "fetch of tax free amount components" in {
        when(codingComponentService.taxFreeAmountComponents(any(), any())(any()))
          .thenReturn(Future.failed(new Error))
        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
