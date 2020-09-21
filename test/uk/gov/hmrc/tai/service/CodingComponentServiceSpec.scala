/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.connectors.{TaxAccountConnector, TaxFreeAmountComparisonConnector}
import uk.gov.hmrc.tai.connectors.responses.{TaiCacheError, TaiNotFoundResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class CodingComponentServiceSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "Coding component service" must {
    "return sequence of tax free amount components" when {
      "provided with valid nino" in {
        val service = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(codingComponents)))

        val result = service.taxFreeAmountComponents(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe codingComponents
      }
    }

    "filter out Zero Amount Coding Components" when {
      "provided with valid nino" in {
        val service = createSut
        val incomeType: CodingComponent = CodingComponent(BalancingCharge, None, 0, "BalancingCharge Description")
        when(taxAccountConnector.codingComponents(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(codingComponents :+ incomeType)))

        val result = service.taxFreeAmountComponents(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe codingComponents
      }
    }

    "return empty CodingComponents" when {
      "error is TaiNotFoundResponse" in {
        val service = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any()))
          .thenReturn(Future.successful(TaiNotFoundResponse("no coding components found")))

        val result = service.taxFreeAmountComponents(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe Seq.empty
      }
    }

    "return Tai Failure response" when {
      "error is received from TAI" in {
        val service = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse("could not fetch coding components")))

        val exceptionThrown = the[RuntimeException] thrownBy Await
          .result(service.taxFreeAmountComponents(generateNino, currentTaxYear), 5 seconds)
        exceptionThrown.getMessage must include("could not fetch coding components")
      }
    }

    "return exception" when {
      "error is received from TAI" in {
        val service = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse("could not fetch coding components")))

        val exceptionThrown = the[RuntimeException] thrownBy Await
          .result(service.taxFreeAmountComponents(generateNino, currentTaxYear), 5 seconds)
        exceptionThrown.getMessage must include("could not fetch coding components")
      }

      "other error is received from TAI" in {
        val service = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any()))
          .thenReturn(Future.successful(TaiCacheError("error description")))

        val exceptionThrown = the[RuntimeException] thrownBy Await
          .result(service.taxFreeAmountComponents(generateNino, currentTaxYear), 5 seconds)
        exceptionThrown.getMessage must include("could not fetch coding components")
      }
    }
  }

  "taxFreeAmountComparison" must {
    "return a TaxFreeAmountComparison" in {
      val service = createSut

      val codingComponent1 = CodingComponent(BalancingCharge, None, 100, "BalancingCharge Description")
      val codingComponent2 = CodingComponent(BalancingCharge, None, 100, "Description2")

      val taxFreeAmountComparison = TaxFreeAmountComparison(Seq(codingComponent1), Seq(codingComponent2))

      when(taxFreeAmountComparisonConnector.taxFreeAmountComparison(any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxFreeAmountComparison)))

      val result = service.taxFreeAmountComparison(generateNino)

      Await.result(result, 5.seconds) mustBe taxFreeAmountComparison
    }

    "filter out zero amount coding components" in {
      val service = createSut

      val codingComponent1 = CodingComponent(BalancingCharge, None, 0, "BalancingCharge Description")
      val codingComponent2 = CodingComponent(BalancingCharge, None, 0, "Description2")

      val taxFreeAmountComparison = TaxFreeAmountComparison(Seq(codingComponent1), Seq(codingComponent2))

      when(taxFreeAmountComparisonConnector.taxFreeAmountComparison(any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxFreeAmountComparison)))

      val result = service.taxFreeAmountComparison(generateNino)

      Await.result(result, 5.seconds) mustBe TaxFreeAmountComparison(Seq.empty, Seq.empty)
    }

    "throw a runtime exception" when {
      "a TaiTaxAccountFailureResponse is received" in {
        val service = createSut

        when(taxFreeAmountComparisonConnector.taxFreeAmountComparison(any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse("Help")))

        val exceptionThrown = the[RuntimeException] thrownBy Await
          .result(service.taxFreeAmountComparison(generateNino), 5.seconds)
        exceptionThrown.getMessage must include("Help")
      }

      "a unknown exception is received" in {
        val service = createSut

        when(taxFreeAmountComparisonConnector.taxFreeAmountComparison(any())(any()))
          .thenReturn(Future.successful(null))

        val exceptionThrown = the[RuntimeException] thrownBy Await
          .result(service.taxFreeAmountComparison(generateNino), 5.seconds)
        exceptionThrown.getMessage must include("Could not fetch tax free amount comparison")
      }
    }
  }

  private val currentTaxYear = TaxYear()

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  def generateNino: Nino = new Generator(new Random).nextNino

  val codingComponents: Seq[CodingComponent] = Seq(
    CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
    CodingComponent(GiftsSharesCharity, None, 1000, "GiftAidAfterEndOfTaxYear description")
  )

  private def createSut = new SUT

  val taxAccountConnector = mock[TaxAccountConnector]
  val taxFreeAmountComparisonConnector = mock[TaxFreeAmountComparisonConnector]

  private class SUT extends CodingComponentService(taxAccountConnector, taxFreeAmountComparisonConnector)

}
