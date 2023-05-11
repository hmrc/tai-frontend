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

package uk.gov.hmrc.tai.service

import controllers.FakeTaiPlayApplication
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.tai.connectors.{TaxAccountConnector, TaxFreeAmountComparisonConnector}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
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
          .thenReturn(Future.successful(codingComponents))

        val result = service.taxFreeAmountComponents(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe codingComponents
      }
    }

    "filter out Zero Amount Coding Components" when {
      "provided with valid nino" in {
        val service = createSut
        val incomeType: CodingComponent = CodingComponent(BalancingCharge, None, 0, "BalancingCharge Description")
        when(taxAccountConnector.codingComponents(any(), any())(any()))
          .thenReturn(Future.successful(codingComponents :+ incomeType))

        val result = service.taxFreeAmountComponents(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe codingComponents
      }
    }

    "return empty CodingComponents" when {
      "error is NotFoundException at Connector level" in {
        val service = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any()))
          .thenReturn(Future.successful(Seq.empty[CodingComponent]))

        val result = service.taxFreeAmountComponents(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe Seq.empty
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
        .thenReturn(Future.successful(taxFreeAmountComparison))

      val result = service.taxFreeAmountComparison(generateNino)

      Await.result(result, 5.seconds) mustBe taxFreeAmountComparison
    }

    "filter out zero amount coding components" in {
      val service = createSut

      val codingComponent1 = CodingComponent(BalancingCharge, None, 0, "BalancingCharge Description")
      val codingComponent2 = CodingComponent(BalancingCharge, None, 0, "Description2")

      val taxFreeAmountComparison = TaxFreeAmountComparison(Seq(codingComponent1), Seq(codingComponent2))

      when(taxFreeAmountComparisonConnector.taxFreeAmountComparison(any())(any()))
        .thenReturn(Future.successful(taxFreeAmountComparison))

      val result = service.taxFreeAmountComparison(generateNino)

      Await.result(result, 5.seconds) mustBe TaxFreeAmountComparison(Seq.empty, Seq.empty)
    }

    "return BadRequestException" when {
      "a BadRequestException is received" in {
        val service = createSut

        when(taxFreeAmountComparisonConnector.taxFreeAmountComparison(any())(any()))
          .thenReturn(Future.failed(new BadRequestException("Bad request")))

        val exceptionThrown = the[BadRequestException] thrownBy Await
          .result(service.taxFreeAmountComparison(generateNino), 5.seconds)
        exceptionThrown.getMessage must include("Bad request")
      }

      "an unknown exception is received" in {
        val service = createSut

        when(taxFreeAmountComparisonConnector.taxFreeAmountComparison(any())(any()))
          .thenReturn(Future.successful(null))

        assertThrows[NullPointerException] {
          Await.result(service.taxFreeAmountComparison(generateNino), 5.seconds)
        }
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
