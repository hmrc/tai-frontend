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
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.service.{PersonService, TaxAccountService, TaxCodeChangeService}
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future
import scala.util.Random

class YourTaxCodeControllerSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val hc = HeaderCarrier()

  "viewTaxCode" must {
    "display tax code page" in {
      val SUT = createSUT
      val startOfTaxYear: String = TaxYear().start.toString("d MMMM yyyy")
      val endOfTaxYear: String = TaxYear().end.toString("d MMMM yyyy")
      val taxCodeIncomes = Seq(TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L",
        "employment", OtherBasisOfOperation, Live))

      when(SUT.taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))
      when(SUT.taxAccountService.scottishBandRates(any(), any(), any())(any()))
        .thenReturn(Future.successful(Map.empty[String, BigDecimal]))

      val startDate = TaxYearResolver.startOfCurrentTaxYear
      val previousTaxCodeRecord1 = TaxCodeRecord("1185L", startDate, startDate.plusMonths(1), OtherBasisOfOperation,"A Employer 1", false, Some("1234"), false)
      val currentTaxCodeRecord1 = previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)

      val taxCodeChange = TaxCodeChange(Seq(previousTaxCodeRecord1), Seq(currentTaxCodeRecord1))

      when(SUT.taxCodeChangeService.taxCodeChange(any(), any())(any()))
        .thenReturn(Future.successful(taxCodeChange))

      val result = SUT.taxCodes()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      val taxYearSuffix = Messages("tai.taxCode.title.pt2", startOfTaxYear, endOfTaxYear)
      doc.title must include(s"${Messages("tai.taxCode.single.code.title.pt1")} ${taxYearSuffix}")
    }

    "display error when there is TaiFailure in service" in {
      val SUT = createSUT
      when(SUT.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiTaxAccountFailureResponse("error occurred")))
      val result = SUT.taxCodes()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "display any error" in {
      val SUT = createSUT
      when(SUT.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.failed(new InternalError("error occurred")))
      val result = SUT.taxCodes()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "prevTaxCodes" must {
    "display tax code page" in {
      val SUT = createSUT
      val startOfTaxYear: String = TaxYear().prev.start.toString("d MMMM yyyy")
      val endOfTaxYear: String = TaxYear().prev.end.toString("d MMMM yyyy")
      val taxCodeIncomes = Seq(TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L",
        "employment", OtherBasisOfOperation, Live))

      when(SUT.taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))
      when(SUT.taxAccountService.scottishBandRates(any(), any(), any())(any()))
        .thenReturn(Future.successful(Map.empty[String, BigDecimal]))

      val startDate = TaxYearResolver.startOfCurrentTaxYear
      val previousTaxCodeRecord1 = TaxCodeRecord("1185L", startDate, startDate.plusMonths(1), OtherBasisOfOperation,"A Employer 1", false, Some("1234"), false)
      val currentTaxCodeRecord1 = previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)

      val taxCodeChange = TaxCodeChange(Seq(previousTaxCodeRecord1), Seq(currentTaxCodeRecord1))

      when(SUT.taxCodeChangeService.taxCodeChange(any(), any())(any()))
        .thenReturn(Future.successful(taxCodeChange))

      val result = SUT.prevTaxCodes(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      val taxYearSuffix = Messages("tai.taxCode.title.pt2", startOfTaxYear, endOfTaxYear)
      doc.title must include(s"${Messages("tai.taxCode.prev.single.code.title.pt1")} ${taxYearSuffix}")
    }

    "display error when there is TaiFailure in service" in {
      val SUT = createSUT
      when(SUT.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiTaxAccountFailureResponse("error occurred")))
      val result = SUT.prevTaxCodes(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "display any error" in {
      val SUT = createSUT
      when(SUT.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.failed(new InternalError("error occurred")))
      val result = SUT.prevTaxCodes(TaxYear().prev)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  val nino = new Generator(new Random).nextNino

  private def createSUT = new SUT

  private class SUT extends YourTaxCodeController {
    override val personService: PersonService = mock[PersonService]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val authConnector: AuthConnector = mock[AuthConnector]
    override val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
    override val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]

    override val taxCodeChangeEnabled = true

    when(authConnector.currentAuthority(any(), any())).thenReturn(Future.successful(Some(AuthBuilder.createFakeAuthority(nino.nino))))
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

  }

}
