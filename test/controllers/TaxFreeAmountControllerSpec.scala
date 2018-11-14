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
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{GiftAidPayments, GiftsSharesCharity}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, PersonService}
import uk.gov.hmrc.tai.util.{HtmlFormatter, TaxYearRangeUtil}
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future
import scala.util.Random

class TaxFreeAmountControllerSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "taxFreeAmount" must {
    "show tax free amount page" in {
      val SUT = createSUT()
      when(SUT.codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(Future.successful(codingComponents))
      when(SUT.companyCarService.companyCarOnCodingComponents(any(), any())(any())).thenReturn(Future.successful(Nil))
      when(SUT.employmentService.employmentNames(any(), any())(any())).thenReturn(Future.successful(Map.empty[Int, String]))
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
        when(SUT.codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(Future.failed(new InternalError("error occurred")))

        val result = SUT.taxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  private def createSUT(newPageEnabled: Boolean = true) = new SUT()

  private val nino = new Generator(new Random).nextNino

  val codingComponents = Seq(CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
    CodingComponent(GiftsSharesCharity, None, 1000, "GiftsSharesCharity description"))

  private class SUT() extends TaxFreeAmountController {
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val authConnector: AuthConnector = mock[AuthConnector]
    override val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever
    override val personService: PersonService = mock[PersonService]
    override val codingComponentService: CodingComponentService = mock[CodingComponentService]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val companyCarService: CompanyCarService = mock[CompanyCarService]

    val ad = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
  }

}
