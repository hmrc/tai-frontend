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
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{GiftAidPayments, GiftsSharesCharity, TaxCodeHistory, TaxCodeRecord}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, PersonService, TaxCodeChangeService}

import scala.concurrent.Future
import scala.util.Random


class TaxCodeChangeControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "whatHappensNext" must {
    "show 'What happens next' page" when {
      "the request has an authorised session" in {
        val SUT = createSUT
        val result = SUT.whatHappensNext()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
      }
    }
  }

  "yourTaxFreeAmount" must {
    "show 'Your tax-free amount' page" when {
      "the request has an authorised session" in {
        val SUT = createSUT

        val taxCodeHistory = TaxCodeHistory(generateNino.nino, Some(List(TaxCodeRecord("1185L","Employer 1","operated","2017-06-23"))))

        when(SUT.codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(Future.successful(codingComponents))
        when(SUT.companyCarService.companyCarOnCodingComponents(any(), any())(any())).thenReturn(Future.successful(Nil))
        when(SUT.employmentService.employmentNames(any(), any())(any())).thenReturn(Future.successful(Map.empty[Int, String]))
        when(SUT.taxCodeChangeService.taxCodeHistory(any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeHistory)))

        val result = SUT.yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

  }

  "taxCodeComparison" must {
    "show 'Your tax code comparison' page" when {
      "the request has an authorised session" in {
        val SUT = createSUT
        val result = SUT.taxCodeComparison()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
      }
    }
  }


  private def createSUT = new SUT
  def generateNino: Nino = new Generator(new Random).nextNino

  val codingComponents = Seq(CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
    CodingComponent(GiftsSharesCharity, None, 1000, "GiftsSharesCharity description"))



  private class SUT extends TaxCodeChangeController {

    override implicit val partialRetriever: FormPartialRetriever = mock[FormPartialRetriever]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override val personService: PersonService = mock[PersonService]
    override val codingComponentService: CodingComponentService = mock[CodingComponentService]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val companyCarService: CompanyCarService = mock[CompanyCarService]
    override val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(generateNino)))
  }

}
