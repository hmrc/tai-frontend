/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.auth.AuthenticatedRequest
import org.mockito.ArgumentMatchers.{any, eq => meq}
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.yourTaxFreeAmount.{DescribedYourTaxFreeAmountService, TaxCodeChangeReasonsService}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.TaxFreeInfo
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import utils.BaseSpec
import views.html.taxCodeChange.{TaxCodeComparisonView, WhatHappensNextView, YourTaxFreeAmountView}

import java.time.LocalDate
import scala.concurrent.Future

class TaxCodeChangeControllerSpec extends BaseSpec with ControllerViewTestHelper {

  "whatHappensNext" must {
    "show 'What happens next' page" when {
      "the request has an authorised session" in {
        implicit val request: AuthenticatedRequest[AnyContentAsFormUrlEncoded] =
          AuthenticatedRequest(RequestBuilder.buildFakeRequestWithAuth("GET"), authedUser, fakePerson(nino))

        val result = createController().whatHappensNext()(request)

        status(result) mustBe OK

        result rendersTheSameViewAs whatHappensNextView()
      }
    }
  }

  "yourTaxFreeAmount" must {
    "show 'Your tax-free amount' page with comparison" when {
      "taxFreeAmountComparison is enabled and the request has an authorised session" in {
        val expectedViewModel: YourTaxFreeAmountViewModel =
          YourTaxFreeAmountViewModel(
            Some(TaxFreeInfo("previousTaxDate", 0, 0)),
            TaxFreeInfo("currentTaxDate", 0, 0),
            Seq.empty,
            Seq.empty
          )

        when(describedYourTaxFreeAmountService.taxFreeAmountComparison(meq(FakeAuthAction.nino))(any(), any(), any()))
          .thenReturn(Future.successful(expectedViewModel))

        val result = createController().yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        result rendersTheSameViewAs yourTaxFreeAmountView(expectedViewModel)(
          AuthenticatedRequest(
            RequestBuilder
              .buildFakeRequestWithAuth("GET"),
            authedUser,
            fakePerson(nino)
          ),
          implicitly,
          implicitly
        )
      }
    }
  }

  "taxCodeComparison" must {
    "show 'Your tax code comparison' page" in {
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")

      val taxCodeChange = TaxCodeChange(List(taxCodeRecord1), List(taxCodeRecord2))
      val scottishRates = Map.empty[String, BigDecimal]

      when(taxAccountService.scottishBandRates(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Map[String, BigDecimal]()))
      when(taxAccountService.totalTax(meq(FakeAuthAction.nino), any())(any()))
        .thenReturn(Future.successful(TotalTax(0, Seq.empty, None, None, None)))
      when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))
      when(yourTaxFreeAmountService.taxFreeAmountComparison(any())(any(), any(), any()))
        .thenReturn(Future.successful(mock[YourTaxFreeAmountComparison]))

      val reasons = Seq("a reason")
      when(taxCodeChangeReasonsService.combineTaxCodeChangeReasons(any(), any(), meq(taxCodeChange))(any()))
        .thenReturn(reasons)
      when(taxCodeChangeReasonsService.isAGenericReason(meq(reasons))(any())).thenReturn(false)

      val result = createController().taxCodeComparison()(request)

      val expectedViewModel =
        TaxCodeChangeViewModel(
          taxCodeChange,
          scottishRates,
          reasons,
          isAGenericReason = false,
          maybeUserName = Some("Firstname Surname")
        )

      status(result) mustBe OK
      result rendersTheSameViewAs taxCodeComparisonView(expectedViewModel, appConfig)
    }
  }

  val giftAmount = 1000

  val startDate: LocalDate = TaxYear().start

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

  val personService: PersonService = mock[PersonService]
  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val describedYourTaxFreeAmountService: DescribedYourTaxFreeAmountService = mock[DescribedYourTaxFreeAmountService]
  val yourTaxFreeAmountService: YourTaxFreeAmountService = mock[YourTaxFreeAmountService]
  val taxCodeChangeReasonsService: TaxCodeChangeReasonsService = mock[TaxCodeChangeReasonsService]

  private def createController() = new TaxCodeChangeTestController

  private val whatHappensNextView = inject[WhatHappensNextView]

  private val yourTaxFreeAmountView = inject[YourTaxFreeAmountView]

  private val taxCodeComparisonView = inject[TaxCodeComparisonView]

  private class TaxCodeChangeTestController
      extends TaxCodeChangeController(
        taxCodeChangeService,
        taxAccountService,
        describedYourTaxFreeAmountService,
        mockAuthJourney,
        yourTaxFreeAmountService,
        taxCodeChangeReasonsService,
        appConfig,
        mcc,
        taxCodeComparisonView,
        yourTaxFreeAmountView,
        whatHappensNextView
      ) {}

}
