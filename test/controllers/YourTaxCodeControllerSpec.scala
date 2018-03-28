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
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.EmploymentIncome
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service.{TaiService, TaxAccountService}

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
        "employment", OtherBasisOperation, Live))
      when(SUT.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))
      when(SUT.taxAccountService.scottishBandRates(any(), any(), any())(any())).thenReturn(Future.successful(Map.empty[String, BigDecimal]))
      val result = SUT.taxCodes()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      val taxYearSuffix = Messages("tai.taxCode.title.pt2", startOfTaxYear, endOfTaxYear)
      doc.title() must include(s"${Messages("tai.taxCode.single.code.title.pt1")} ${taxYearSuffix}")
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

  val nino = new Generator(new Random).nextNino

  private def createSUT = new SUT

  private class SUT extends YourTaxCodeController {
    override val taiService: TaiService = mock[TaiService]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val authConnector: AuthConnector = mock[AuthConnector]
    override val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: PartialRetriever = MockPartialRetriever
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]

    when(authConnector.currentAuthority(any(), any())).thenReturn(Future.successful(Some(AuthBuilder.createFakeAuthority(nino.nino))))
    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(fakeTaiRoot(nino)))

  }

}
