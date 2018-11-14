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
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.{AuditConstants, TaiConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class TaxAccountSummaryControllerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport with AuditConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "onPageLoad" must {


    "display the income tax summary page" in {
      val sut = createSUT
      when(sut.employmentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(employment)))
      when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
      when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )
      when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
      )
      when(sut.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(true))

      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      val expectedTitle = s"${messagesApi("tai.incomeTaxSummary.heading.part1")} ${TaxYearRangeUtil.currentTaxYearRange}"
      doc.title() must include(expectedTitle)
    }

    "raise an audit event" in {
      val sut = createSUT
      when(sut.employmentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(employment)))
      when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
      when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )
      when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
      )
      when(sut.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(true))
      when(sut.auditService.createAndSendAuditEvent(Matchers.eq(TaxAccountSummary_UserEntersSummaryPage), Matchers.eq(Map("nino" -> nino.nino)))(any(), any())).thenReturn(Future.successful(Success))
      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      verify(sut.auditService, times(1)).createAndSendAuditEvent(Matchers.eq(TaxAccountSummary_UserEntersSummaryPage), Matchers.eq(Map("nino" -> nino.nino)))(Matchers.any(), Matchers.any())
    }

    "display an error page" when {
      "a downstream error has occurred in one of the TaiResponse responding service methods" in {
        val sut = createSUT
        when(sut.employmentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(employment)))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
        )
        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future(TaiTaxAccountFailureResponse("Data retrieval failure")))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
      "a downstream error has occurred in the employment service (which does not reply with TaiResponse type)" in {
        val sut = createSUT
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
        )
        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )
        when(sut.employmentService.employments(any(), any())(any())).thenReturn(Future.failed(new BadRequestException("no employments recorded for this individual")))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }


      "a downstream error has occurred in the tax code income service (which does not reply with TaiResponse type)" in {
        val sut = createSUT
        when(sut.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(true))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
        )
        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )
        when(sut.employmentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(employment)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }


      "a downstream error has occurred in one of the TaiResponse responding service methods due to no found primary employment information" in {
        val sut = createSUT
        when(sut.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(true))
        when(sut.authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData(nino))
        when(sut.personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future(TaiTaxAccountFailureResponse(TaiConstants.NpsTaxAccountDataAbsentMsg.toLowerCase)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url)

      }
      "a downstream error has occurred in one of the TaiResponse responding service methods due to no employments recorded for current tax year" in {
        val sut = createSUT
        when(sut.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(true))
        when(sut.authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData(nino))
        when(sut.personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future(TaiTaxAccountFailureResponse(TaiConstants.NpsNoEmploymentForCurrentTaxYear.toLowerCase)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url)

      }
    }

  }


  val nino = new Generator(new Random).nextNino

  val employment = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1, None, false, false)

  val taxAccountSummary = TaxAccountSummary(111,222, 333.33, 444.44, 111.11)

  val taxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live))

  val nonTaxCodeIncome = NonTaxCodeIncome(Some(uk.gov.hmrc.tai.model.domain.income.UntaxedInterest(UntaxedInterestIncome,
    None, 100, "Untaxed Interest", Seq.empty[BankAccount])), Seq(
    OtherNonTaxCodeIncome(Profit, None, 100, "Profit")
  ))

  def createSUT = new SUT()

  class SUT() extends TaxAccountSummaryController {
    override val personService: PersonService = mock[PersonService]
    override val auditService: AuditService = mock[AuditService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val trackingService: TrackingService = mock[TrackingService]
    override val authConnector: AuthConnector = mock[AuthConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever

    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData(nino))
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
  }
}
