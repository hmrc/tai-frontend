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
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}
import uk.gov.hmrc.tai.model.domain.{GiftAidPayments, GiftsSharesCharity}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, TaxAccountService}
import uk.gov.hmrc.tai.util.TaxYearRangeUtil

import scala.concurrent.Future
import scala.util.Random

class TaxFreeAmountControllerSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "taxFreeAmount" must {
    "show tax free amount page" in {
      val SUT = createSUT()
      val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
      val incomeCatergories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
      val totalTax : TotalTax = TotalTax(1000, Seq(incomeCatergories), None, None, None)

      when(codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(Future.successful(codingComponents))
      when(companyCarService.companyCarOnCodingComponents(any(), any())(any())).thenReturn(Future.successful(Nil))
      when(employmentService.employmentNames(any(), any())(any())).thenReturn(Future.successful(Map.empty[Int, String]))
      when(taxAccountService.totalTax(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))
      val result = SUT.taxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))

      val expectedTitle =
        s"${messagesApi("tai.taxFreeAmount.heading.pt1")} ${TaxYearRangeUtil.currentTaxYearRange}"

      doc.title() must include(expectedTitle)
    }

    "display error page" when {
      "display any error" in {
        val SUT = createSUT()
        when(codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(Future.failed(new InternalError("error occurred")))

        val result = SUT.taxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  private def createSUT() = new SUT()

  val codingComponents: Seq[CodingComponent] = Seq(CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
    CodingComponent(GiftsSharesCharity, None, 1000, "GiftsSharesCharity description"))

  val codingComponentService = mock[CodingComponentService]
  val companyCarService = mock[CompanyCarService]
  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]

  private class SUT() extends TaxFreeAmountController(
    codingComponentService,
    employmentService,
    taxAccountService,
    companyCarService,
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  )
}
