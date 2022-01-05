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
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, GenericBenefit}
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import utils.BaseSpec
import views.html.IncomeSourceSummaryView

import scala.concurrent.Future

class IncomeSourceSummaryControllerSpec extends BaseSpec {

  val employmentId = 1
  val pensionId = 2

  "onPageLoad" must {
    "display the income details page" when {
      "asked for employment details" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(Matchers.eq(employmentId.toString))(any()))
          .thenReturn(Future.successful(true))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages("tai.employment.income.details.mainHeading.gaTitle", TaxYearRangeUtil.currentTaxYearRange))
      }

      "asked for pension details" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(Matchers.eq(pensionId.toString))(any()))
          .thenReturn(Future.successful(true))

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages("tai.pension.income.details.mainHeading.gaTitle", TaxYearRangeUtil.currentTaxYearRange))
      }
    }

    "throw error" when {
      "failed to read tax code incomes" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse("FAILED")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "failed to read employment details" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  val latestPayment = Payment(new LocalDate().minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)
  val annualAccount = AnnualAccount(
    uk.gov.hmrc.tai.model.TaxYear(),
    Available,
    Seq(latestPayment, secondPayment, thirdPayment, firstPayment),
    Nil)
  val employment = Employment(
    "test employment",
    Live,
    Some("EMPLOYER-1122"),
    LocalDate.now(),
    None,
    Seq(annualAccount),
    "",
    "",
    2,
    None,
    false,
    false)

  private val taxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "pension", Week1Month1BasisOfOperation, Live)
  )

  private val benefits = Benefits(Seq.empty[CompanyCarBenefit], Seq.empty[GenericBenefit])

  val personService: PersonService = mock[PersonService]
  val benefitsService: BenefitsService = mock[BenefitsService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService =
    mock[EstimatedPayJourneyCompletionService]

  def sut = new IncomeSourceSummaryController(
    mock[AuditConnector],
    taxAccountService,
    employmentService,
    benefitsService,
    estimatedPayJourneyCompletionService,
    FakeAuthAction,
    FakeValidatePerson,
    appConfig,
    mcc,
    inject[IncomeSourceSummaryView],
    templateRenderer,
    inject[ErrorPagesHandler]
  )
}
