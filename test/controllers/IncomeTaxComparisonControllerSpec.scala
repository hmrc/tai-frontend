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
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{verify, when}
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, TaxAccountService, UpdateNextYearsIncomeService}
import utils.BaseSpec
import views.html.incomeTaxComparison.MainView

import java.time.LocalDate
import scala.concurrent.Future

class IncomeTaxComparisonControllerSpec extends BaseSpec {

  implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")

  val employment: Employment =
    Employment(
      "employment1",
      Live,
      None,
      Some(LocalDate.now),
      None,
      Nil,
      "",
      "",
      1,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    )

  val employment2: Employment =
    Employment(
      "employment2",
      Live,
      None,
      Some(LocalDate.now),
      None,
      Nil,
      "",
      "",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    )

  val pension: Employment =
    Employment(
      "employment3",
      Live,
      None,
      Some(LocalDate.now),
      None,
      Nil,
      "",
      "",
      3,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = true
    )

  val pension2: Employment =
    Employment(
      "employment4",
      Live,
      None,
      Some(LocalDate.now),
      None,
      Nil,
      "",
      "",
      4,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = true
    )

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(111, 222, 333, 444, 111)

  val taxCodeIncomesNoEmpId: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, None, 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  val taxCodeIncomesMultiple: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(EmploymentIncome, Some(2), 3234, "employment2", "1050L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(3), 1234, "employment3", "150L", "employment", Week1Month1BasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(4), 4321, "employment4", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  val taxCodeIncomesCYPlusOne: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 2222, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 2222, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  val taxCodeIncomesCYPlusOneMultiple: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 2222, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(EmploymentIncome, Some(2), 4000, "employment2", "1050L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(3), 3333, "employment3", "150L", "employment", Week1Month1BasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(4), 4444, "employment4", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  val taxCodeIncomesCYPlusOne2: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 2222, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  val codingComponentService: CodingComponentService = mock[CodingComponentService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val updateNextYearsIncomeService: UpdateNextYearsIncomeService = mock[UpdateNextYearsIncomeService]

  class TestController()
      extends IncomeTaxComparisonController(
        mock[AuditConnector],
        taxAccountService,
        employmentService,
        codingComponentService,
        updateNextYearsIncomeService,
        mockAuthJourney,
        appConfig,
        mcc,
        inject[MainView],
        inject[ErrorPagesHandler]
      ) {

    when(taxAccountService.taxCodeIncomes(any(), any())(any()))
      .thenReturn(Future.successful(Right(taxCodeIncomes)))
    when(taxAccountService.taxAccountSummary(any(), any())(any()))
      .thenReturn(Future.successful(taxAccountSummary))
    when(codingComponentService.taxFreeAmountComponents(any(), any())(any()))
      .thenReturn(Future.successful(Seq.empty[CodingComponent]))
    when(employmentService.employments(any(), meq(TaxYear()))(any()))
      .thenReturn(Future.successful(Seq(employment)))
    when(updateNextYearsIncomeService.isEstimatedPayJourneyComplete(any(), any())).thenReturn(Future.successful(false))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers("testSessionId"))
  }

  "onPageLoad" must {
    "display the cy plus one page" in {
      val controller = new TestController
      val result = controller.onPageLoad()(request)
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.incomeTaxComparison.heading.more"))

      verify(employmentService).employments(any(), meq(TaxYear()))(any())
    }

    "throw an error page" when {
      "not able to fetch comparison details" in {
        val controller = new TestController
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Not Found")))

        val result = controller.onPageLoad()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  "rendered CY+1 page" must {
    "show estimated income for CY and CY+1 for single employment" in {
      val controller = new TestController
      when(taxAccountService.taxCodeIncomes(any(), meq(TaxYear().next))(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomesCYPlusOne)))

      val result = controller.onPageLoad()(request)
      status(result) mustBe OK

      def doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("amount-cy-0").text must equal("£1,111")
      doc.getElementById("amount-cy-plus-one-0").text must equal("£2,222")
    }

    "show estimated income for CY and CY+1 for multiple employments" in {
      val controller = new TestController
      when(taxAccountService.taxCodeIncomes(any(), meq(TaxYear()))(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomesMultiple)))
      when(taxAccountService.taxCodeIncomes(any(), meq(TaxYear().next))(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomesCYPlusOneMultiple)))
      when(employmentService.employments(any(), meq(TaxYear()))(any()))
        .thenReturn(Future.successful(Seq(employment, employment2)))

      val result = controller.onPageLoad()(request)
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("amount-cy-1").text must equal("£1,111")
      doc.getElementById("amount-cy-plus-one-1").text must equal("£2,222")
      doc.getElementById("amount-cy-0").text must equal("£3,234")
      doc.getElementById("amount-cy-plus-one-0").text must equal("£4,000")
    }

    "show estimated income for CY and CY+1 for multiple pensions" in {
      val controller = new TestController
      when(taxAccountService.taxCodeIncomes(any(), meq(TaxYear()))(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomesMultiple)))
      when(taxAccountService.taxCodeIncomes(any(), meq(TaxYear().next))(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomesCYPlusOneMultiple)))
      when(employmentService.employments(any(), meq(TaxYear()))(any()))
        .thenReturn(Future.successful(Seq(employment, employment2, pension, pension2)))

      val result = controller.onPageLoad()(request)
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("pension-amount-cy-0").text must equal("£4,321")
      doc.getElementById("pension-amount-cy-plus-one-0").text must equal("£4,444")
      doc.getElementById("pension-amount-cy-1").text must equal("£1,234")
      doc.getElementById("pension-amount-cy-plus-one-1").text must equal("£3,333")
    }

    "show not applicable when CY and CY+1 employment id's don't match" in {
      val controller = new TestController
      when(taxAccountService.taxCodeIncomes(any(), meq(TaxYear().next))(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomesCYPlusOne2)))
      when(employmentService.employments(any(), meq(TaxYear()))(any()))
        .thenReturn(Future.successful(Seq(employment, employment2)))

      val result = controller.onPageLoad()(request)
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("amount-cy-0").text must equal("£1,111")
      doc.getElementById("amount-cy-plus-one-0").text must equal("not applicable")
    }

    "show not applicable when employment id is missing for CY+1" in {
      val controller = new TestController
      when(taxAccountService.taxCodeIncomes(any(), meq(TaxYear().next))(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomesNoEmpId)))
      when(employmentService.employments(any(), meq(TaxYear()))(any()))
        .thenReturn(Future.successful(Seq(employment, employment2)))

      val result = controller.onPageLoad()(request)
      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("amount-cy-plus-one-0").text must equal("not applicable")
    }
  }
}
