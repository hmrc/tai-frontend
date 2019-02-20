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

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.yourTaxFreeAmount.DescribedYourTaxFreeAmountService
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.TaxFreeInfo
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import scala.concurrent.Future
import scala.util.Random


class TaxCodeChangeControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with ControllerViewTestHelper {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "whatHappensNext" must {
    "show 'What happens next' page" when {
      "the request has an authorised session" in {
        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = createController.whatHappensNext()(request)

        status(result) mustBe OK

        result rendersTheSameViewAs views.html.taxCodeChange.whatHappensNext()
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

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")



        when(describedYourTaxFreeAmountService.taxFreeAmountComparison(Matchers.eq(FakeAuthAction.nino))(any(), any()))
          .thenReturn(Future.successful(expectedViewModel))

        val result = createController.yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        result rendersTheSameViewAs views.html.taxCodeChange.yourTaxFreeAmount(expectedViewModel)
      }
    }
  }

  "taxCodeComparison" must {
    "show 'Your tax code comparison' page" when {
      "the request has an authorised session" in {
        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))
        val scottishRates = Map.empty[String, BigDecimal]

        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))
        when(taxAccountService.scottishBandRates(any(), any(), any())(any())).thenReturn(Future.successful(Map[String, BigDecimal]()))

        val result = createController.taxCodeComparison()(request)

        val taxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange, scottishRates)

        status(result) mustBe OK
        result rendersTheSameViewAs views.html.taxCodeChange.taxCodeComparison(taxCodeChangeViewModel)
      }
    }
  }

  val nino: Nino = new Generator(new Random).nextNino

  val giftAmount = 1000

  val startDate = TaxYear().start

  val taxCodeRecord1 = TaxCodeRecord("D0", startDate, startDate.plusDays(1), OtherBasisOfOperation, "Employer 1", false, Some("1234"), true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(1), endDate = TaxYear().end)

  val personService: PersonService = mock[PersonService]
  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val yourTaxFreeAmountService: YourTaxFreeAmountService = mock[YourTaxFreeAmountService]
  val describedYourTaxFreeAmountService: DescribedYourTaxFreeAmountService = mock[DescribedYourTaxFreeAmountService]

  private def createController() = new TaxCodeChangeTestController()

  private class TaxCodeChangeTestController() extends TaxCodeChangeController(
    taxCodeChangeService,
    taxAccountService,
    describedYourTaxFreeAmountService,
    FakeAuthAction,
    FakeValidatePerson,
    mock[YourTaxFreeAmountService],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(taxCodeChangeService.latestTaxCodeChangeDate(nino)).thenReturn(Future.successful(new LocalDate(2018, 6, 11)))
  }
}
