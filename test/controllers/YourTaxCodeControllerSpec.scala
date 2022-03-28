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
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, TaxCodeRecord}
import uk.gov.hmrc.tai.service.{TaxAccountService, TaxCodeChangeService}
import utils.BaseSpec
import views.html.{TaxCodeDetailsPreviousYearsView, TaxCodeDetailsView}

import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class YourTaxCodeControllerSpec extends BaseSpec with BeforeAndAfterEach {

  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]

  def sut = new YourTaxCodeController(
    taxAccountService,
    taxCodeChangeService,
    FakeAuthAction,
    FakeValidatePerson,
    mcc,
    appConfig,
    inject[TaxCodeDetailsView],
    inject[TaxCodeDetailsPreviousYearsView],
    templateRenderer,
    inject[ErrorPagesHandler]
  )

  override def beforeEach: Unit =
    Mockito.reset(taxAccountService)

  "renderTaxCodes" must {
    "display error when there is TaiFailure in service" in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiTaxAccountFailureResponse("error occurred")))
      val result = sut.renderTaxCodes(None)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "display any error" in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.failed(new InternalError("error occurred")))
      val result = sut.renderTaxCodes(None)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "tax code pages" must {
    val empId = 1
    val taxCodeIncomes = Seq(
      TaxCodeIncome(
        EmploymentIncome,
        Some(empId),
        1111,
        "employment",
        "1150L",
        "employment",
        OtherBasisOfOperation,
        Live))

    "display tax code page containing all tax codes" in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))
      when(taxAccountService.scottishBandRates(any(), any(), any())(any()))
        .thenReturn(Future.successful(Map.empty[String, BigDecimal]))

      val startOfTaxYear: String =
        TaxYear().start.format(DateTimeFormatter.ofPattern("d MMMM yyyy")).replaceAll(" ", "\u00A0")
      val endOfTaxYear: String =
        TaxYear().end.format(DateTimeFormatter.ofPattern("d MMMM yyyy")).replaceAll(" ", "\u00A0")

      val result = sut.taxCodes(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title must include(Messages("tai.taxCode.single.code.title", startOfTaxYear, endOfTaxYear))
    }

    "display tax code page containing the relevant tax codes" in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))
      when(taxAccountService.scottishBandRates(any(), any(), any())(any()))
        .thenReturn(Future.successful(Map.empty[String, BigDecimal]))

      val result = sut.taxCode(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.body().toString must include(Messages("tai.taxCode.wrong"))
      doc.body().toString must include(
        Messages("tai.taxCode.subheading", taxCodeIncomes.head.name, taxCodeIncomes.head.taxCode))
    }
  }

  "prevTaxCodes" must {
    "display tax code page" in {
      val startOfTaxYear: String = TaxYear().prev.start.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
      val endOfTaxYear: String = TaxYear().prev.end.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

      when(taxAccountService.scottishBandRates(any(), any(), any())(any()))
        .thenReturn(Future.successful(Map.empty[String, BigDecimal]))

      val startDate = TaxYear().start
      val previousTaxCodeRecord1 = TaxCodeRecord(
        "1185L",
        startDate,
        startDate.plusMonths(1),
        OtherBasisOfOperation,
        "A Employer 1",
        pensionIndicator = false,
        Some("1234"),
        primary = false)

      val taxCodeRecords = List(previousTaxCodeRecord1)

      when(taxCodeChangeService.lastTaxCodeRecordsInYearPerEmployment(any(), any())(any()))
        .thenReturn(Future.successful(taxCodeRecords))

      val result = sut.prevTaxCodes(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      val startOfYearNonBreak = startOfTaxYear.replaceAll(" ", "\u00A0")
      val endOfYearNonBreak = endOfTaxYear.replaceAll(" ", "\u00A0")

      doc.title must include(Messages("tai.taxCode.prev.single.code.title", startOfYearNonBreak, endOfYearNonBreak))
    }

    "display error when there is TaiFailure in service" in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiTaxAccountFailureResponse("error occurred")))
      val result = sut.prevTaxCodes(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "display any error" in {
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.failed(new InternalError("error occurred")))
      val result = sut.prevTaxCodes(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
