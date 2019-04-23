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

import builders.{AuthBuilder, RequestBuilder}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.{AuditConstants, TaiConstants}
import uk.gov.hmrc.tai.viewModels.{IncomesSources, TaxAccountSummaryViewModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class TaxAccountSummaryControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with AuditConstants
  with BeforeAndAfterEach {


  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override def beforeEach: Unit = {
    Mockito.reset(auditService)
  }

  "onPageLoad" must {

    "display the income tax summary page" in {
      val sut = createSUT
      when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
      )

      when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
        Future.successful(TaxAccountSummaryViewModel(taxAccountSummary,
          ThreeWeeks,
          nonTaxCodeIncome,
          IncomesSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
          nonMatchedEmployments))
      )

      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      val expectedTitle = s"${messagesApi("tai.incomeTaxSummary.heading.part1", TaxYearRangeUtil.currentTaxYearRangeSingleLine)}"
      doc.title() must include(expectedTitle)
    }

    "raise an audit event" in {
      val sut = createSUT
      when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
      )

      when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
        Future.successful(TaxAccountSummaryViewModel(taxAccountSummary,
          ThreeWeeks,
          nonTaxCodeIncome,
          IncomesSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
          nonMatchedEmployments))
      )

      when(auditService.createAndSendAuditEvent(Matchers.eq(TaxAccountSummary_UserEntersSummaryPage), Matchers.eq(Map("nino" -> nino.nino)))(any(), any()))
        .thenReturn(Future.successful(Success))


      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      verify(auditService, times(1))
        .createAndSendAuditEvent(Matchers.eq(TaxAccountSummary_UserEntersSummaryPage), Matchers.eq(Map("nino" -> nino.nino)))(Matchers.any(), Matchers.any())
    }

    "display an error page" when {
      "a downstream error has occurred in one of the TaiResponse responding service methods" in {
        val sut = createSUT
        when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
          Future.successful(TaxAccountSummaryViewModel(taxAccountSummary,
            ThreeWeeks,
            nonTaxCodeIncome,
            IncomesSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
            nonMatchedEmployments))
        )

        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future(TaiTaxAccountFailureResponse("Data retrieval failure")))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
      "a downstream error has occurred in the employment service (which does not reply with TaiResponse type)" in {
        val sut = createSUT
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
          Future.failed(new RuntimeException("Failed to fetch income details"))
        )

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }


      "a downstream error has occurred in the tax code income service (which does not reply with TaiResponse type)" in {
        val sut = createSUT
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
        )
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(employment)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }


      "a downstream error has occurred in one of the TaiResponse responding service methods due to no found primary employment information" in {
        val sut = createSUT

        when(sut.authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData(nino))
        when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future(TaiTaxAccountFailureResponse(TaiConstants.NpsTaxAccountDataAbsentMsg.toLowerCase)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url)

      }
      "a downstream error has occurred in one of the TaiResponse responding service methods due to no employments recorded for current tax year" in {
        val sut = createSUT

        when(sut.authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData(nino))
        when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future(TaiTaxAccountFailureResponse(TaiConstants.NpsNoEmploymentForCurrentTaxYear.toLowerCase)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url)

      }
    }

  }


  val nino: Nino = new Generator(new Random).nextNino

  val employment = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1, None, false, false)

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(111, 222, 333.33, 444.44, 111.11)

  val employmentIncomeLive = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live)
  val pensionIncomeLive = TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)

  val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    employmentIncomeLive,
    pensionIncomeLive
  )

  val nonTaxCodeIncome = NonTaxCodeIncome(Some(uk.gov.hmrc.tai.model.domain.income.UntaxedInterest(UntaxedInterestIncome,
    None, 100, "Untaxed Interest", Seq.empty[BankAccount])), Seq(
    OtherNonTaxCodeIncome(Profit, None, 100, "Profit")
  ))

  val liveEmployment1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
  val liveEmployment2 = TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment", "BR", "employer2", Week1Month1BasisOfOperation, Live)
  val livePension3 = TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)
  val livePension4 = TaxCodeIncome(PensionIncome, Some(4), 4444, "employment", "BR", "employer4", Week1Month1BasisOfOperation, Live)
  val potentiallyCeasedEmployment9 = TaxCodeIncome(EmploymentIncome, Some(9), 1111, "employment", "1150L", "employer9", OtherBasisOfOperation, PotentiallyCeased)
  val ceasedEmployment10 = TaxCodeIncome(EmploymentIncome, Some(10), 2222, "employment", "BR", "employer10", Week1Month1BasisOfOperation, Ceased)
  val empEmployment1 = Employment("Employer name1", Some("1ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST1", "PAYE1", 1, None, false, false)
  val empEmployment2 = Employment("Employer name2", Some("1ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST2", "PAYE2", 2, None, false, false)
  val pensionEmployment3 = Employment("Pension name1", Some("3ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST3", "PAYE3", 3, None, false, false)
  val pensionEmployment4 = Employment("Pension name2", Some("4ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST4", "PAYE4", 4, None, false, false)
  val empEmployment9 = Employment("Employer name3", Some("9ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST9", "PAYE9", 9, None, false, false)
  val empEmployment10 = Employment("Employer name4", Some("10ABC"), new LocalDate(2017, 3, 1), Some(new LocalDate(2018, 4, 21)), Seq.empty[AnnualAccount], "DIST10", "PAYE10", 10, None, false, false)


  val livePensionIncomeSources: Seq[TaxedIncome] = Seq(
    TaxedIncome(livePension3, pensionEmployment3),
    TaxedIncome(livePension4, pensionEmployment4)
  )

  val liveEmploymentIncomeSources: Seq[TaxedIncome] = Seq(
    TaxedIncome(liveEmployment1, empEmployment1),
    TaxedIncome(liveEmployment2, empEmployment2)
  )

  val ceasedEmploymentIncomeSources: Seq[TaxedIncome] = Seq(
    TaxedIncome(potentiallyCeasedEmployment9, empEmployment9),
    TaxedIncome(ceasedEmployment10, empEmployment10)
  )
  val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
  val taxCodeIncomeCeased = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Ceased)
  val payment = Payment(new LocalDate(), BigDecimal(123.45), BigDecimal(678.90), BigDecimal(123.12), BigDecimal(444.44), BigDecimal(555.55), BigDecimal(666.66), Monthly)
  val annualAccount = AnnualAccount("key", uk.gov.hmrc.tai.model.TaxYear(), Available, Seq(payment), Nil)
  val ceasedEmployment = Employment("Ceased employer name", Some("123ABC"), new LocalDate(2017, 3, 1), Some(new LocalDate(2018, 4, 21)), Seq(annualAccount), "DIST123", "PAYE543", 1, None, false, false)

  val nonMatchedEmployments = Seq(
    ceasedEmployment.copy(sequenceNumber = 998),
    Employment("Pension name1", Some("3ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST3", "PAYE3", 999, None, false, false)
  )

  def createSUT = new SUT()

  val personService: PersonService = mock[PersonService]
  val trackingService = mock[TrackingService]
  val auditService = mock[AuditService]
  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]
  val taxAccountSummaryService = mock[TaxAccountSummaryService]

  class SUT() extends TaxAccountSummaryController(
    trackingService,
    employmentService,
    taxAccountService,
    taxAccountSummaryService,
    auditService,
    personService,
    mock[AuditConnector],
    mock[DelegationConnector],
    mock[AuthConnector],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {
    when(trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(ThreeWeeks))

    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData(nino))
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()


}
