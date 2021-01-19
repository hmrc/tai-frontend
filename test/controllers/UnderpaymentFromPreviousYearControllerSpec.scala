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

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import utils.BaseSpec

import scala.concurrent.Future

class UnderpaymentFromPreviousYearControllerSpec extends BaseSpec {

  val referralMap = Map("Referer" -> "http://somelocation/somePageResource")

  val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
  val incomeCatergories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
  val totalTax: TotalTax = TotalTax(1000, Seq(incomeCatergories), None, None, None)

  "UnderPaymentFromPreviousYearController" should {
    "respond with OK" when {
      "underpaymentExplanation is called" in {
        val controller = new SUT
        val result = controller.underpaymentExplanation()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
        status(result) mustBe OK
        contentAsString(result) must include(messagesApi("tai.previous.year.underpayment.title"))
      }
    }
  }

  val codingComponentService: CodingComponentService = mock[CodingComponentService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]

  private class SUT()
      extends UnderpaymentFromPreviousYearController(
        codingComponentService,
        employmentService,
        mock[CompanyCarService],
        taxAccountService,
        FakeAuthAction,
        FakeValidatePerson,
        mcc,
        partialRetriever,
        templateRenderer
      ) {
    when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq.empty))
    when(taxAccountService.totalTax(any(), any())(any())).thenReturn(Future(TaiSuccessResponseWithPayload(totalTax)))
    when(codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(Future.successful(Seq.empty))

  }

}
