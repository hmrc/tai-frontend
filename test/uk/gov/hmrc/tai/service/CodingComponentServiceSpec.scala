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

package uk.gov.hmrc.tai.service

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.connectors.TaxAccountConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class CodingComponentServiceSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "Coding component service" must {
    "return sequence of tax free amount components" when {
      "provided with valid nino" in {
        val sut = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(codingComponents)))

        val result = sut.taxFreeAmountComponents(generateNino, currrentTaxYear)
        Await.result(result, 5 seconds) mustBe codingComponents
      }
    }

    "filter out Zero Amount Coding Components" when {
      "provided with valid nino" in {
        val sut = createSut
        val incomeType: CodingComponent = CodingComponent(BalancingCharge, None, 0, "BalancingCharge Description")
        when(taxAccountConnector.codingComponents(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(codingComponents :+ incomeType)))

        val result = sut.taxFreeAmountComponents(generateNino, currrentTaxYear)
        Await.result(result, 5 seconds) mustBe codingComponents
      }
    }

    "return Tai Failure response" when {
      "error is received from TAI" in {
        val sut = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any())).thenReturn(Future.successful(TaiTaxAccountFailureResponse("could not fetch coding components")))

        val ex = the[RuntimeException] thrownBy Await.result(sut.taxFreeAmountComponents(generateNino, currrentTaxYear), 5 seconds)
        ex.getMessage must include("could not fetch coding components")
      }
    }

    "return exception" when {
      "error is received from TAI" in {
        val sut = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any())).thenReturn(Future.successful(TaiTaxAccountFailureResponse("could not fetch coding components")))


        val ex = the[RuntimeException] thrownBy Await.result(sut.taxFreeAmountComponents(generateNino, currrentTaxYear), 5 seconds)
        ex.getMessage must include("could not fetch coding components")
      }
      "other error is received from TAI" in {
        val sut = createSut
        when(taxAccountConnector.codingComponents(any(), any())(any())).thenReturn(Future.successful(TaiNotFoundResponse("error description")))


        val ex = the[RuntimeException] thrownBy Await.result(sut.taxFreeAmountComponents(generateNino, currrentTaxYear), 5 seconds)
        ex.getMessage must include("could not fetch coding components")
      }
    }
  }

  private val currrentTaxYear = TaxYear()

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  def generateNino: Nino = new Generator(new Random).nextNino

  val codingComponents: Seq[CodingComponent] = Seq(CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
    CodingComponent(GiftsSharesCharity, None, 1000, "GiftAidAfterEndOfTaxYear description"))

  private def createSut = new SUT

  val taxAccountConnector = mock[TaxAccountConnector]

  private class SUT extends CodingComponentService(taxAccountConnector)

}