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
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, TotalTax}
import uk.gov.hmrc.tai.service.{PersonService, TaxAccountService}

import scala.concurrent.Future
import scala.util.Random

class DetailedIncomeTaxEstimateControllerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "Tax Explanation Controller" must {
    "return OK" when {
      "there are bands present" in {
        val sut = createSUT
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            TotalTax(0 , Seq.empty[IncomeCategory], None, None, None)
          )))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            Seq.empty[TaxCodeIncome]
          )))

        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return error" when {
      "failed to fetch tax bands" in {
        val sut = createSUT
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("FAILED")))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            Seq.empty[TaxCodeIncome]
          )))

        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }


  val nino: Nino = new Generator(new Random).nextNino
  private def createSUT = new SUT

  class SUT extends TaxExplanationController {
    override val personService: PersonService = mock[PersonService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = mock[FormPartialRetriever]

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData)
  }
}

