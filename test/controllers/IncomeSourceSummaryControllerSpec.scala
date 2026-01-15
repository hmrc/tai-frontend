/*
 * Copyright 2025 HM Revenue & Customs
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
import cats.data.EitherT
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.mockito.Mockito
import org.scalatest.AppendedClues.convertToClueful
import pages.benefits.EndCompanyBenefitsUpdateIncomePage
import play.api.i18n.Messages
import play.api.mvc.Results.NotFound
import play.api.test.Helpers._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service.{EmploymentService, IabdService, RtiService, TaxAccountService}
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import utils.BaseSpec
import views.html.IncomeSourceSummaryView

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceSummaryControllerSpec extends BaseSpec {

  private val firstPayment: Payment  = Payment(LocalDate.now.minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  private val secondPayment: Payment = Payment(LocalDate.now.minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  private val thirdPayment: Payment  = Payment(LocalDate.now.minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  private val latestPayment: Payment = Payment(LocalDate.now.minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)

  private val annualAccount: AnnualAccount = AnnualAccount(
    7,
    taxYear = uk.gov.hmrc.tai.model.TaxYear(),
    realTimeStatus = Available,
    payments = Seq(latestPayment, secondPayment, thirdPayment, firstPayment),
    endOfTaxYearUpdates = Nil
  )
  private val employment: Employment       = Employment(
    name = "test employment",
    employmentStatus = Live,
    payrollNumber = Some("EMPLOYER-1122"),
    startDate = Some(LocalDate.now()),
    endDate = None,
    taxDistrictNumber = "DD",
    payeNumber = "001",
    sequenceNumber = 2,
    cessationPay = None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false,
    EmploymentIncome
  )

  private val taxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1112, "employment2", "150L", "pension", Week1Month1BasisOfOperation, Live)
  )

  private val mockEmploymentService: EmploymentService = mock[EmploymentService]
  private val mockTaxAccountService: TaxAccountService = mock[TaxAccountService]
  private val mockRtiService                           = mock[RtiService]
  private val mockIabdService                          = mock[IabdService]
  private val baseUserAnswers: UserAnswers             = UserAnswers("testSessionId", nino.nino)

  private def sut = new IncomeSourceSummaryController(
    mock[AuditConnector],
    mockTaxAccountService,
    mockEmploymentService,
    mockIabdService,
    mockAuthJourney,
    mcc,
    inject[IncomeSourceSummaryView],
    mockRtiService,
    mockEmpIdCheck,
    inject[ErrorPagesHandler]
  )

  private def rtiResponse(
    aa: Option[AnnualAccount] = Some(annualAccount)
  ): EitherT[Future, UpstreamErrorResponse, Option[AnnualAccount]] =
    EitherT.rightT[Future, UpstreamErrorResponse](aa)

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(baseUserAnswers)
    Mockito.reset(mockRtiService)
    Mockito.reset(mockEmploymentService)
    Mockito.reset(mockTaxAccountService)
    Mockito.reset(mockIabdService)

    when(mockIabdService.getIabds(any(), any())(any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty[IabdDetails]))
  }

  private val employmentId = 1
  private val pensionId    = 2

  def taxAccountSummary(
    totalEstimatedTax: BigDecimal = BigDecimal(0),
    taxFreeAmount: BigDecimal = BigDecimal(0),
    totalInYearAdjustmentIntoCY: BigDecimal = BigDecimal(0),
    totalInYearAdjustment: BigDecimal = BigDecimal(0),
    totalInYearAdjustmentIntoCYPlusOne: BigDecimal = BigDecimal(0),
    totalEstimatedIncome: BigDecimal = BigDecimal(0),
    taxFreeAllowance: BigDecimal = BigDecimal(0),
    date: Option[LocalDate] = None
  ) =
    TaxAccountSummary(
      totalEstimatedTax,
      taxFreeAmount,
      totalInYearAdjustmentIntoCY,
      totalInYearAdjustment,
      totalInYearAdjustmentIntoCYPlusOne,
      totalEstimatedIncome,
      taxFreeAllowance,
      date
    )

  "onPageLoad" must {
    "display the income details page" when {
      "asked for employment details" in {
        val userAnswers = baseUserAnswers
        setup(userAnswers)

        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary())
        )
        when(mockEmploymentService.employment(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any())).thenReturn(rtiResponse())

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages(
            "tai.employment.income.details.mainHeading.gaTitle",
            TaxYearRangeUtil.currentTaxYearRangeBreak.replaceAll("\u00A0", " ")
          )
        )
        verify(mockEmploymentService, times(1)).employment(any(), any(), any())(any())
      }

      def setUpPension(
        annualAccount: Either[Int, Option[AnnualAccount]]
      ): OngoingStubbing[?] = {
        val userAnswers = baseUserAnswers
        setup(userAnswers)

        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              taxCodeIncomes
                .filter(_.componentType == PensionIncome)
                .map(
                  _.copy(employmentId =
                    Some(annualAccount.fold(_ => 1, accounts => accounts.headOption.fold(1)(_.sequenceNumber)))
                  )
                )
            )
          )
        )
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary())
        )
        when(mockEmploymentService.employment(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(
                employment
                  .copy(
                    sequenceNumber =
                      annualAccount.fold(_ => 1, accounts => accounts.headOption.fold(1)(_.sequenceNumber)),
                    receivingOccupationalPension = true
                  )
              )
            )
          )
        val rtiResponseOrError = annualAccount.fold(
          status => EitherT.leftT[Future, Option[AnnualAccount]](UpstreamErrorResponse(s"status $status", status)),
          accountRight => EitherT.rightT[Future, UpstreamErrorResponse](accountRight)
        )
        when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any())).thenReturn(
          rtiResponseOrError
        )
      }

      "asked for pension details and include RTI section where RTI data present" in {
        setUpPension(Right(Some(annualAccount)))

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages(
            "tai.pension.income.details.mainHeading.gaTitle",
            TaxYearRangeUtil.currentTaxYearRangeBreak.replaceAll("\u00A0", " ")
          )
        )
        Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some(
          "£1,112"
        ) withClue "html id estimatedIncome"
        Option(doc.getElementById("incomeReceivedToDate")).map(_.text()) mustBe Some(
          "£400"
        ) withClue "html id incomeReceivedToDate"
        Option(doc.getElementById("taxCode")).map(_.text()) mustBe Some("150L") withClue "html id taxCode"
        Option(doc.getElementById("empPayeRef")).map(_.text()) mustBe Some("DD/001") withClue "html id empPayeRef"
        Option(doc.getElementById("updatePension")).isDefined mustBe true withClue "html id updatePension"
        verify(mockEmploymentService, times(1)).employment(any(), any(), any())(any())
      }

      "asked for pension details and NOT include RTI section where RTI data NOT present" in {
        setUpPension(Right(None))
        when(mockRtiService.getAllPaymentsForYear(any(), any())(any()))
          .thenReturn(EitherT(Future.successful[Either[UpstreamErrorResponse, Seq[AnnualAccount]]](Right(Nil))))
        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc    = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some(
          "£1,112"
        ) withClue "html id estimatedIncome"
        Option(doc.getElementById("incomeReceivedToDate"))
          .map(_.text()) mustBe Some(
          "Your income received to date is unavailable. Try again later"
        ) withClue "html id incomeReceivedToDate"
        Option(doc.getElementById("taxCode")).map(_.text()) mustBe Some("150L") withClue "html id taxCode"
        Option(doc.getElementById("empPayeRef")).map(_.text()) mustBe Some("DD/001") withClue "html id empPayeRef"
        Option(doc.getElementById("updatePension")).isDefined mustBe false withClue "html id updatePension"
        verify(mockEmploymentService, times(1)).employment(any(), any(), any())(any())
      }

      "asked for pension details and NOT include RTI section where RTI data present but RTI unavailable" in {
        setUpPension(Right(Some(annualAccount.copy(realTimeStatus = TemporarilyUnavailable))))

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc    = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some(
          "£1,112"
        ) withClue "html id estimatedIncome"
        Option(doc.getElementById("incomeReceivedToDate"))
          .map(_.text()) mustBe Some(
          "Your income received to date is unavailable. Try again later"
        ) withClue "html id incomeReceivedToDate"
        Option(doc.getElementById("taxCode")).map(_.text()) mustBe Some("150L") withClue "html id taxCode"
        Option(doc.getElementById("empPayeRef")).map(_.text()) mustBe Some("DD/001") withClue "html id empPayeRef"
        Option(doc.getElementById("updatePension")).isDefined mustBe false withClue "html id updatePension"
        verify(mockEmploymentService, times(1)).employment(any(), any(), any())(any())
      }

      "asked for pension details and NOT include RTI section where RTI response is 500" in {
        setUpPension(Left(INTERNAL_SERVER_ERROR))
        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc    = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some(
          "£1,112"
        ) withClue "html id estimatedIncome"
        Option(doc.getElementById("incomeReceivedToDate"))
          .map(_.text()) mustBe Some(
          "Your income received to date is unavailable. Try again later"
        ) withClue "html id incomeReceivedToDate"
        Option(doc.getElementById("taxCode")).map(_.text()) mustBe Some("150L") withClue "html id taxCode"
        Option(doc.getElementById("empPayeRef")).map(_.text()) mustBe Some("DD/001") withClue "html id empPayeRef"
        Option(doc.getElementById("updatePension")).isDefined mustBe false withClue "html id updatePension"
        verify(mockEmploymentService, times(1)).employment(any(), any(), any())(any())
      }

      "asked for pension details and NOT include RTI section where left not found response" in {
        setUpPension(Left(NOT_FOUND))
        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc    = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some(
          "£1,112"
        ) withClue "html id estimatedIncome"
        Option(doc.getElementById("incomeReceivedToDate"))
          .map(_.text()) mustBe Some(
          "Your income received to date is unavailable. Try again later"
        ) withClue "html id incomeReceivedToDate"
        Option(doc.getElementById("taxCode")).map(_.text()) mustBe Some("150L") withClue "html id taxCode"
        Option(doc.getElementById("empPayeRef")).map(_.text()) mustBe Some("DD/001") withClue "html id empPayeRef"
        Option(doc.getElementById("updatePension")).isDefined mustBe false withClue "html id updatePension"
        verify(mockEmploymentService, times(1)).employment(any(), any(), any())(any())
      }

      "failed to read tax code incomes" in {
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary())
        )
        when(mockEmploymentService.employment(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any())).thenReturn(
          rtiResponse(
            Some(
              annualAccount
                .copy(sequenceNumber = employment.sequenceNumber)
            )
          )
        )

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some(
          "Your estimated taxable income is missing. Add your estimated taxable income."
        ) withClue "html id estimatedIncome"
        Option(doc.getElementById("incomeReceivedToDate"))
          .map(_.text()) mustBe Some("£400") withClue "html id incomeReceivedToDate"
        Option(doc.getElementById("taxCode")).map(_.text()) mustBe Some(
          "Your tax code is unavailable. Try again later"
        ) withClue "html id taxCode"
        Option(doc.getElementById("empPayeRef")).map(_.text()) mustBe Some("DD/001") withClue "html id empPayeRef"
        Option(doc.getElementById("updatePension")).isDefined mustBe false withClue "html id updatePension"
        verify(mockEmploymentService, times(1)).employment(any(), any(), any())(any())
      }
    }

    "throw error" when {
      "failed to read employment details" in {
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary())
        )
        when(mockEmploymentService.employment(any(), any(), any())(any())).thenReturn(Future.successful(None))
        when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](None)
        )

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
        verify(mockEmploymentService, times(1)).employment(any(), any(), any())(any())
      }
    }

    "flush the cache" when {
      "cache update amount is the same as the HOD amount" in {
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary())
        )
        when(mockEmploymentService.employment(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any())).thenReturn(
          rtiResponse(
            Some(annualAccount.copy(sequenceNumber = employment.sequenceNumber))
          )
        )

        val userAnswers = baseUserAnswers
          .setOrException(EndCompanyBenefitsUpdateIncomePage(employmentId), "1111")
        setup(userAnswers)

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages("tai.employment.income.details.mainHeading.gaTitle", TaxYearRangeUtil.currentTaxYearRangeBreak)
        )
        Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some(
          "£1,112"
        ) withClue "html id estimatedIncome"
        Option(doc.getElementById("incomeReceivedToDate"))
          .map(_.text()) mustBe Some("£400") withClue "html id incomeReceivedToDate"
        Option(doc.getElementById("taxCode")).map(_.text()) mustBe Some("150L") withClue "html id taxCode"
        Option(doc.getElementById("empPayeRef")).map(_.text()) mustBe Some("DD/001") withClue "html id empPayeRef"
        Option(doc.getElementById("updatePension")).isDefined mustBe false withClue "html id updatePension"

        verify(mockEmploymentService, times(1)).employment(any(), any(), any())(any())
      }
    }

    "must not call standard backend calls" when {
      "the empId is not found in the empIdCheck" in {
        when(mockEmpIdCheck.checkValidId(any(), any())(any()))
          .thenReturn(Future.successful(Some(NotFound("empId not found"))))

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe NOT_FOUND

        verify(mockEmploymentService, times(0)).employment(any(), any(), any())(any())
        verify(mockRtiService, times(0)).getAllPaymentsForYear(any(), any())(any())
        verify(mockTaxAccountService, times(0)).taxCodeIncomes(any(), any())(any())
        verify(mockIabdService, times(0)).getIabds(any(), any())(any())

      }
    }
  }

  "display estimated income from iabd instead of tax account details for the employment" when {
    "iabd is more recent than tax account details" in {
      val ua = baseUserAnswers
      setup(ua)

      when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomes)))
      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary(date = Some(LocalDate.now.minusWeeks(1))))
      )
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(employment.copy(sequenceNumber = employmentId))))
      when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any()))
        .thenReturn(rtiResponse(Some(annualAccount.copy(sequenceNumber = employmentId))))

      when(mockIabdService.getIabds(any(), any())(any()))
        .thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](
            Seq(
              IabdDetails(
                Some(employmentId),
                None,
                Some(27),
                None,
                Some(LocalDate.now.minusWeeks(2)),
                Some(BigDecimal(9994))
              ),
              IabdDetails(Some(45), None, Some(27), None, Some(LocalDate.now), Some(BigDecimal(9995))),
              IabdDetails(Some(employmentId), None, Some(27), None, Some(LocalDate.now), Some(BigDecimal(9999))),
              IabdDetails(
                Some(employmentId),
                None,
                Some(27),
                None,
                Some(LocalDate.now.minusWeeks(1)),
                Some(BigDecimal(9996))
              )
            )
          )
        )

      val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some("£9,999")
    }

    "no date is available for both iabd and tax account" in {
      val ua = baseUserAnswers
      setup(ua)

      when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomes)))
      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary(date = None))
      )
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(employment.copy(sequenceNumber = employmentId))))
      when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any()))
        .thenReturn(rtiResponse(Some(annualAccount.copy(sequenceNumber = employmentId))))

      when(mockIabdService.getIabds(any(), any())(any()))
        .thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](
            Seq(IabdDetails(Some(employmentId), None, Some(27), None, None, Some(BigDecimal(9999))))
          )
        )

      val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some("£9,999")
    }

    "There is no date in tax account details" in {
      val ua = baseUserAnswers
      setup(ua)

      when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomes)))
      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary(date = None))
      )
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(employment.copy(sequenceNumber = employmentId))))
      when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any()))
        .thenReturn(rtiResponse(Some(annualAccount.copy(sequenceNumber = employmentId))))

      when(mockIabdService.getIabds(any(), any())(any()))
        .thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](
            Seq(
              IabdDetails(
                Some(employmentId),
                None,
                Some(27),
                None,
                Some(LocalDate.now.minusMonths(6)),
                Some(BigDecimal(9999))
              )
            )
          )
        )

      val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some("£9,999")
    }

    "There is no date for iabd" in {
      val ua = baseUserAnswers
      setup(ua)

      when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomes)))
      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary(date = Some(LocalDate.now.minusWeeks(1))))
      )
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(employment.copy(sequenceNumber = employmentId))))
      when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any()))
        .thenReturn(rtiResponse(Some(annualAccount.copy(sequenceNumber = employmentId))))

      when(mockIabdService.getIabds(any(), any())(any()))
        .thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](
            Seq(IabdDetails(Some(employmentId), None, Some(27), None, None, Some(BigDecimal(9999))))
          )
        )

      val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some("£9,999")
    }

  }

  "display estimated income from tax account details for the employment" when {
    "iabd is too old" in {
      val ua = baseUserAnswers
      setup(ua)

      when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomes)))
      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        EitherT.rightT[Future, UpstreamErrorResponse](taxAccountSummary(date = Some(LocalDate.now)))
      )
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(employment.copy(sequenceNumber = employmentId))))
      when(mockRtiService.getPaymentsForEmploymentAndYear(any(), any(), any())(any()))
        .thenReturn(rtiResponse(Some(annualAccount.copy(sequenceNumber = employmentId))))

      when(mockIabdService.getIabds(any(), any())(any()))
        .thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](
            Seq(
              IabdDetails(
                Some(employmentId),
                None,
                Some(27),
                None,
                Some(LocalDate.now.minusWeeks(2)),
                Some(BigDecimal(9994))
              ),
              IabdDetails(Some(45), None, Some(27), None, Some(LocalDate.now), Some(BigDecimal(9995))),
              IabdDetails(
                Some(employmentId),
                None,
                Some(27),
                None,
                Some(LocalDate.now.minusWeeks(1)),
                Some(BigDecimal(9996))
              )
            )
          )
        )

      val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      Option(doc.getElementById("estimatedIncome")).map(_.text()) mustBe Some("£1,111")
    }
  }
}
