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
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, TaxCodeRecord}
import uk.gov.hmrc.tai.service.{TaxAccountService, TaxCodeChangeService}

import scala.concurrent.Future
import scala.util.Random

class YourTaxCodeControllerSpec
    extends PlaySpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar with BeforeAndAfterEach {

  override def beforeEach: Unit =
    Mockito.reset(taxAccountService)

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val hc = HeaderCarrier()

  "viewTaxCode" must {
    "display tax code page" in {
      val testController = createTestController
      val startOfTaxYear: String = TaxYear().start.toString("d MMMM yyyy").replaceAll(" ", "\u00A0")
      val endOfTaxYear: String = TaxYear().end.toString("d MMMM yyyy").replaceAll(" ", "\u00A0")
      val taxCodeIncomes = Seq(
        TaxCodeIncome(
          EmploymentIncome,
          Some(1),
          1111,
          "employment",
          "1150L",
          "employment",
          OtherBasisOfOperation,
          Live))

      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))
      when(taxAccountService.scottishBandRates(any(), any(), any())(any()))
        .thenReturn(Future.successful(Map.empty[String, BigDecimal]))

      val result = testController.taxCodes()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title must include(Messages("tai.taxCode.single.code.title", startOfTaxYear, endOfTaxYear))
    }

    "display error when there is TaiFailure in service" in {
      val testController = createTestController
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiTaxAccountFailureResponse("error occurred")))
      val result = testController.taxCodes()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "display any error" in {
      val testController = createTestController
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.failed(new InternalError("error occurred")))
      val result = testController.taxCodes()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "prevTaxCodes" must {
    "display tax code page" in {
      val testController = createTestController
      val startOfTaxYear: String = TaxYear().prev.start.toString("d MMMM yyyy")
      val endOfTaxYear: String = TaxYear().prev.end.toString("d MMMM yyyy")

      when(taxAccountService.scottishBandRates(any(), any(), any())(any()))
        .thenReturn(Future.successful(Map.empty[String, BigDecimal]))

      val startDate = TaxYear().start
      val previousTaxCodeRecord1 = TaxCodeRecord(
        "1185L",
        startDate,
        startDate.plusMonths(1),
        OtherBasisOfOperation,
        "A Employer 1",
        false,
        Some("1234"),
        false)

      val taxCodeRecords = Seq(previousTaxCodeRecord1)

      when(taxCodeChangeService.lastTaxCodeRecordsInYearPerEmployment(any(), any())(any()))
        .thenReturn(Future.successful(taxCodeRecords))

      val result = testController.prevTaxCodes(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      val startOfYearNonBreak = startOfTaxYear.replaceAll(" ", "\u00A0")
      val endOfYearNonBreak = endOfTaxYear.replaceAll(" ", "\u00A0")

      doc.title must include(Messages("tai.taxCode.prev.single.code.title", startOfYearNonBreak, endOfYearNonBreak))
    }

    "display error when there is TaiFailure in service" in {
      val testController = createTestController
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiTaxAccountFailureResponse("error occurred")))
      val result = testController.prevTaxCodes(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "display any error" in {
      val testController = createTestController
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.failed(new InternalError("error occurred")))
      val result = testController.prevTaxCodes(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  val nino = new Generator(new Random).nextNino

  private def createTestController = new TestController

  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val taxAccountService = mock[TaxAccountService]

  private class TestController
      extends YourTaxCodeController(
        taxAccountService,
        taxCodeChangeService,
        FakeAuthAction,
        FakeValidatePerson,
        mock[FormPartialRetriever],
        MockTemplateRenderer
      )
}
