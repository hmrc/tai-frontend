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
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.{FormPartialRetriever, HtmlPartial}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, OtherNonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, TotalTax}
import uk.gov.hmrc.tai.service.{CodingComponentService, HasFormPartialService, PersonService, TaxAccountService}

import scala.concurrent.Future
import scala.util.Random

class EstimatedIncomeTaxControllerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "EstimatedIncomeTaxController" must {
    "return Ok" when {
      "loading the estimated income tax page" in {
        val sut = createSUT
        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            TaxAccountSummary(0, 0, 0, 0, 0)
          )))
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            TotalTax(0 , Seq.empty[IncomeCategory], None, None, None)
          )))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).
          thenReturn(Future.successful(Seq.empty[CodingComponent]))
        when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          )))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            Seq.empty[TaxCodeIncome]
          )))
        when(sut.partialService.getIncomeTaxPartial(any())) .thenReturn(Future.successful[HtmlPartial]
          (HtmlPartial.Success(Some("title"), Html("<title/>"))))

        val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return error" when {
      "failed to fetch details" in {
        val sut = createSUT
        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            TotalTax(0 , Seq.empty[IncomeCategory], None, None, None)
          )))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).
          thenReturn(Future.successful(Seq.empty[CodingComponent]))
        when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          )))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            Seq.empty[TaxCodeIncome]
          )))

        val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }


  "Tax Relief" must {
    "return Ok" when {
      "loading the tax relief page" in {
        val sut = createSUT
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            TotalTax(0 , Seq.empty[IncomeCategory], None, None, None)
          )))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).
          thenReturn(Future.successful(Seq.empty[CodingComponent]))

        val result = sut.taxRelief()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return error" when {
      "failed to fetch details" in {
        val sut = createSUT
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).
          thenReturn(Future.successful(Seq.empty[CodingComponent]))

        val result = sut.taxRelief()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  val nino: Nino = new Generator(new Random).nextNino
  private def createSUT = new SUT

  class SUT extends EstimatedIncomeTaxController {
    override val personService: PersonService = mock[PersonService]
    override val partialService: HasFormPartialService = mock[HasFormPartialService]
    override val codingComponentService: CodingComponentService = mock[CodingComponentService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = mock[FormPartialRetriever]

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData)
  }

}
