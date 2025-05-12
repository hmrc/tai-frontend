/*
 * Copyright 2024 HM Revenue & Customs
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
import org.jsoup.nodes.Element
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.http._
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.admin.{CyPlusOneToggle, IncomeTaxHistoryToggle}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import utils.BaseSpec
import views.html.WhatDoYouWantToDoTileView

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.language.postfixOps

class WhatDoYouWantToDoControllerSpec extends BaseSpec with JsoupMatchers {

  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val auditService: AuditService = mock[AuditService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val jrsService: JrsService = mock[JrsService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  private def createTestController() =
    new WhatDoYouWantToDoControllerTest()

  class WhatDoYouWantToDoControllerTest()
      extends WhatDoYouWantToDoController(
        employmentService,
        taxCodeChangeService,
        taxAccountService,
        mock[AuditConnector],
        auditService,
        jrsService,
        mockAuthJourney,
        mockAppConfig,
        mcc,
        inject[WhatDoYouWantToDoTileView],
        mockFeatureFlagService,
        inject[ErrorPagesHandler]
      ) {

    when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(fakeEmploymentData))
    when(auditService.sendUserEntryAuditEvent(any(), any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(AuditResult.Success))
    when(taxAccountService.taxAccountSummary(any(), any())(any()))
      .thenReturn(EitherT.rightT(taxAccountSummary))
    when(jrsService.checkIfJrsClaimsDataExist(any())(any()))
      .thenReturn(EitherT.rightT(false))
  }

  val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(
      EmploymentIncome,
      Some(1),
      BigDecimal(39107),
      "EmploymentIncome",
      "277L",
      "TestName",
      OtherBasisOfOperation,
      Live,
      None,
      Some(LocalDate.of(2015, 11, 26)),
      Some(LocalDate.of(2015, 11, 26))
    )
  )
  val taxCodeNotChanged = false
  val taxCodeChanged = true

  val startDate: LocalDate = LocalDate.now()
  val taxCodeRecord1: TaxCodeRecord = TaxCodeRecord(
    "D0",
    startDate,
    startDate.plusDays(1),
    OtherBasisOfOperation,
    "Employer 1",
    pensionIndicator = false,
    Some("1234"),
    primary = true
  )
  val taxCodeRecord2: TaxCodeRecord = taxCodeRecord1.copy(startDate = startDate.plusDays(1), endDate = TaxYear().end)
  val taxCodeChange: TaxCodeChange = TaxCodeChange(List(taxCodeRecord1), List(taxCodeRecord2))
  val mostRecentTaxCodeChangeDate: String =
    TaxYearRangeUtil.formatDate(taxCodeChange.mostRecentTaxCodeChangeDate).replace("\u00A0", " ")

  private val taxAccountSummary = TaxAccountSummary(111, 222, 333, 444, 111)

  private val fakeEmploymentData = Seq(
    Employment(
      "TEST",
      Live,
      Some("12345"),
      Some(LocalDate.now()),
      None,
      List(AnnualAccount(TaxYear(), Available, Nil, Nil)),
      "",
      "",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    ),
    Employment(
      "TEST1",
      Live,
      Some("123456"),
      Some(LocalDate.now()),
      None,
      List(AnnualAccount(TaxYear(), Unavailable, Nil, Nil)),
      "",
      "",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(auditService, employmentService, mockAppConfig, taxCodeChangeService, jrsService, taxAccountService)
    when(mockFeatureFlagService.getAsEitherT(org.mockito.ArgumentMatchers.eq(CyPlusOneToggle))) thenReturn
      EitherT.rightT(FeatureFlag(CyPlusOneToggle, isEnabled = true))
    when(mockFeatureFlagService.getAsEitherT(org.mockito.ArgumentMatchers.eq(IncomeTaxHistoryToggle))) thenReturn
      EitherT.rightT(FeatureFlag(IncomeTaxHistoryToggle, isEnabled = true))
    when(mockAppConfig.numberOfPreviousYearsToShowIncomeTaxHistory).thenReturn(5)

  }

  "Calling the What do you want to do page method" must {

    "call whatDoYouWantToDoPage() successfully with an authorised session" when {

      "cy plus one data is available and cy plus one is enabled" in {

        val controller = createTestController()

        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT(fakeEmploymentData))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.rightT(taxAccountSummary))
        when(mockFeatureFlagService.getAsEitherT(org.mockito.ArgumentMatchers.eq(CyPlusOneToggle))) thenReturn
          EitherT.rightT(FeatureFlag(CyPlusOneToggle, isEnabled = true))

        val result = controller.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }

      "there has not been a tax code change" in {
        val testController = createTestController()

        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT(fakeEmploymentData))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.rightT(taxAccountSummary))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
        val element: Element = doc.body()
        element.toString mustNot include(Messages("check.tax.hasChanged.header"))
      }

      "there has been a tax code change and cyPlusOne is enabled and jrs claim data does not exist" in {
        val testController = createTestController()

        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT(fakeEmploymentData))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeChanged))
        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(EitherT.rightT(taxCodeChange))
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.rightT(taxAccountSummary))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
        doc.body().toStringBreak must include(
          Messages("tai.WhatDoYouWantToDo.ChangedTaxCode", mostRecentTaxCodeChangeDate)
        )

        doc.select(".card").size mustBe 3
      }

      "cyPlusOne is disabled and jrs claim data does not exist" in {

        val testController = createTestController()

        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT(fakeEmploymentData))
        when(mockFeatureFlagService.getAsEitherT(org.mockito.ArgumentMatchers.eq(CyPlusOneToggle))) thenReturn
          EitherT.rightT(FeatureFlag(CyPlusOneToggle, isEnabled = false))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))

        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeChanged))
        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(EitherT.rightT(taxCodeChange))
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.rightT(taxAccountSummary))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
        doc.body().toStringBreak must include(
          Messages("tai.WhatDoYouWantToDo.ChangedTaxCode", mostRecentTaxCodeChangeDate)
        )
        doc.select(".card").size mustBe 2
      }

      "there has been a tax code change and cyPlusOne is enabled and jrs claim data exist" in {
        val testController = createTestController()

        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT(fakeEmploymentData))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))

        when(jrsService.checkIfJrsClaimsDataExist(any())(any()))
          .thenReturn(EitherT.rightT(true))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeChanged))
        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(EitherT.rightT(taxCodeChange))
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.rightT(taxAccountSummary))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
        val body = doc.body().toStringBreak
        body must include(Messages("tai.WhatDoYouWantToDo.ChangedTaxCode", mostRecentTaxCodeChangeDate))
        body must include(Messages("check.jrs.claims"))

        doc.select(".card").size mustBe 3
      }

      "cyPlusOne is disabled and jrs claim data exist" in {

        val testController = createTestController()
        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT(fakeEmploymentData))
        when(mockFeatureFlagService.getAsEitherT(org.mockito.ArgumentMatchers.eq(CyPlusOneToggle))) thenReturn
          EitherT.rightT(FeatureFlag(CyPlusOneToggle, isEnabled = false))
        when(jrsService.checkIfJrsClaimsDataExist(any())(any()))
          .thenReturn(EitherT.rightT(true))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeChanged))
        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(EitherT.rightT(taxCodeChange))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
        val body = doc.body().toStringBreak
        body must include(Messages("tai.WhatDoYouWantToDo.ChangedTaxCode", mostRecentTaxCodeChangeDate))
        body must include(Messages("check.jrs.claims"))

        doc.select(".card").size mustBe 2
      }
    }

    "return the general 500 error page" when {

      "an internal server error from TaxCodeIncomes is returned from any HOD call" in {
        val testController = createTestController()
        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR)))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR)))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Sorry, there is a problem with the service")
        doc must haveHeadingWithText(Messages("tai.technical.error.heading"))
        doc must haveParagraphWithText(Messages("tai.technical.error.message"))
      }
    }

    "return the 'you can't use this service page'" when {
      "employments hod call has returned a not found for current year, " +
        "and no previous year employment data is present" in {
          val testController = createTestController()
          when(employmentService.employmentsOnly(any(), any())(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))
          when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
            .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))
          when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
            .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))

          val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
          redirectLocation(result) mustBe Some("/check-income-tax/income-tax/no-info")
        }
    }

    "display the WDYWTD page (not redirect)" when {
      "employments hod call has returned not found, indicating no current year data is present, " +
        "but previous year employment data IS present" in {

          val testController = createTestController()

          when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
            .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))
          when(employmentService.employmentsOnly(any(), meq(TaxYear()))(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev))(any()))
            .thenReturn(EitherT.rightT(fakeEmploymentData))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev.prev))(any()))
            .thenReturn(EitherT.rightT(fakeEmploymentData))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev.prev.prev))(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev.prev.prev.prev))(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev.prev.prev.prev.prev))(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))
          when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
            .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))

          val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
          status(result) mustBe OK
          verify(employmentService, times(1)).employmentsOnly(any(), meq(TaxYear()))(any())
          verify(employmentService, times(1)).employmentsOnly(any(), meq(TaxYear().prev))(any())
          // previous years are not called because CY-1 has some data
          verify(employmentService, times(0)).employmentsOnly(any(), meq(TaxYear().prev.prev))(any())
          verify(employmentService, times(0)).employmentsOnly(any(), meq(TaxYear().prev.prev.prev))(any())
          verify(employmentService, times(0)).employmentsOnly(any(), meq(TaxYear().prev.prev.prev.prev))(any())
          verify(employmentService, times(0)).employmentsOnly(any(), meq(TaxYear().prev.prev.prev.prev.prev))(any())
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("your.paye.income.tax.overview"))
        }

      "employments hod call has returned not found, indicating no current year data is present, " +
        "but previous year employment data IS present and CY-1, CY-2 return errors" in {

          val testController = createTestController()

          when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
            .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))
          when(employmentService.employmentsOnly(any(), meq(TaxYear()))(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev))(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR)))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev.prev))(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR)))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev.prev.prev))(any()))
            .thenReturn(EitherT.rightT(fakeEmploymentData))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev.prev.prev.prev))(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))
          when(employmentService.employmentsOnly(any(), meq(TaxYear().prev.prev.prev.prev.prev))(any()))
            .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))
          when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
            .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))

          val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
          status(result) mustBe OK
          verify(employmentService, times(1)).employmentsOnly(any(), meq(TaxYear()))(any())
          verify(employmentService, times(1)).employmentsOnly(any(), meq(TaxYear().prev))(any())
          verify(employmentService, times(1)).employmentsOnly(any(), meq(TaxYear().prev.prev))(any())
          verify(employmentService, times(1)).employmentsOnly(any(), meq(TaxYear().prev.prev.prev))(any())
          verify(employmentService, times(0)).employmentsOnly(any(), meq(TaxYear().prev.prev.prev.prev))(any())
          verify(employmentService, times(0)).employmentsOnly(any(), meq(TaxYear().prev.prev.prev.prev.prev))(any())
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("your.paye.income.tax.overview"))
        }

      "cy plus one data is available and cy plus one is enabled" in {
        val testController = createTestController()

        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT(fakeEmploymentData))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))
        when(mockFeatureFlagService.getAsEitherT(org.mockito.ArgumentMatchers.eq(CyPlusOneToggle))) thenReturn
          EitherT.rightT(FeatureFlag(CyPlusOneToggle, isEnabled = true))
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.rightT(taxAccountSummary))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))
        val cyPlusOne = Option(doc.getElementById("nextTaxYear")).flatMap(_.asScala.toList.headOption)

        status(result) mustBe OK
        verify(taxAccountService, times(1)).taxAccountSummary(any(), meq(TaxYear().next))(any())
        cyPlusOne mustBe a[Some[_]]

        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }

      "cy plus one data is not available and cy plus one is enabled" in {
        val testController = createTestController()

        when(mockFeatureFlagService.getAsEitherT(org.mockito.ArgumentMatchers.eq(CyPlusOneToggle))) thenReturn
          EitherT.rightT(FeatureFlag(CyPlusOneToggle, isEnabled = true))
        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT(fakeEmploymentData))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))
        when(taxAccountService.taxAccountSummary(any(), meq(TaxYear().next))(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))
        verify(taxAccountService, times(1)).taxAccountSummary(any(), meq(TaxYear().next))(any())
        val cyPlusOne = Option(doc.getElementById("nextTaxYear")).flatMap(_.asScala.toList.headOption)

        status(result) mustBe OK
        cyPlusOne mustBe None

        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }

      "cy plus one data is available and cy plus one is disabled" in {

        val testController = createTestController()
        when(mockFeatureFlagService.getAsEitherT(org.mockito.ArgumentMatchers.eq(CyPlusOneToggle))) thenReturn
          EitherT.rightT(FeatureFlag(CyPlusOneToggle, isEnabled = false))
        when(employmentService.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT(fakeEmploymentData))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))
        when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))
        val cyPlusOne = Option(doc.getElementById("nextTaxYear")).flatMap(_.asScala.toList.headOption)
        verify(taxAccountService, times(0)).taxAccountSummary(any(), meq(TaxYear().next))(any())

        status(result) mustBe OK
        cyPlusOne mustBe None

        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }

    }
  }

  "send an user entry audit event" when {
    "landed to the page and get TaiSuccessResponseWithPayload" in {
      val testController = createTestController()

      when(employmentService.employmentsOnly(any(), any())(any()))
        .thenReturn(EitherT.rightT(fakeEmploymentData))
      when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
        .thenReturn(EitherT.rightT(Seq.empty[TaxCodeIncome]))
      when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))

      val result =
        Await.result(testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5.seconds)

      result.header.status mustBe OK

      verify(auditService, times(1)).sendUserEntryAuditEvent(any(), any(), any(), any(), any())(any())
    }
    "landed to the page and get TaiSuccessResponse" in {
      val testController = createTestController()

      when(employmentService.employmentsOnly(any(), any())(any()))
        .thenReturn(EitherT.rightT(fakeEmploymentData))
      when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
        .thenReturn(EitherT.rightT(taxCodeIncomes))
      when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))

      val result =
        Await.result(testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5.seconds)

      result.header.status mustBe OK

      verify(auditService, times(1)).sendUserEntryAuditEvent(any(), any(), any(), any(), any())(any())
    }

    "landed to the page and get not found from taxCodeIncomes" in {
      val testController = createTestController()

      when(employmentService.employmentsOnly(any(), any())(any()))
        .thenReturn(EitherT.rightT(fakeEmploymentData))
      when(taxAccountService.newTaxCodeIncomes(any(), any())(any()))
        .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))
      when(taxCodeChangeService.hasTaxCodeChanged(any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](taxCodeNotChanged))

      val result =
        Await.result(testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5.seconds)

      result.header.status mustBe OK

    }
  }

  "employments retrieval for CY-1" must {

    "supply employment data where data is found" in {
      val testController = createTestController()
      val employments = Await.result(testController.previousYearEmployments(nino), 5 seconds)
      employments mustBe fakeEmploymentData
    }

    "supply an empty list in the event of a downstream failure" in {
      val testController = createTestController()
      when(employmentService.employments(any(), meq(TaxYear().prev))(any()))
        .thenReturn(Future.failed(new NotFoundException("no data found")))
      val employments = Await.result(testController.previousYearEmployments(nino), 5 seconds)
      employments mustBe Nil
    }
  }

}
