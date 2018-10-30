/*
 * Copyright 2018 HM Revenue & Customs
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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{HasTaxCodeChanged, TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.time.TaxYearResolver
import utils.factories.TaxCodeMismatchFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class TaxCodeChangeServiceSpec extends PlaySpec with MockitoSugar{

  "taxCodeChange" must {
    "return the tax code change given a valid nino" in {
      val sut = createSut
      val nino = generateNino

      val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))

      when(sut.taxCodeChangeConnector.taxCodeChange(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeChange)))

      val result = sut.taxCodeChange(nino)
      Await.result(result, 5.seconds) mustBe taxCodeChange
    }
  }

  "has tax code changed" must {
    "return a HasTaxCodeChanged object" when {
      "success response from the connectors" in {
        val sut = createSut
        val nino = generateNino

        val taxCodeMismatch = TaxCodeMismatchFactory.matchedTaxCode
        val hasTaxCodeChanged = HasTaxCodeChanged(true, Some(taxCodeMismatch))

        when(sut.taxCodeChangeConnector.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(true)))
        when(sut.taxCodeChangeConnector.taxCodeMismatch(any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeMismatch)))

        val result = sut.hasTaxCodeChanged(nino)
        Await.result(result, 5.seconds) mustBe hasTaxCodeChanged
      }
    }

    "throws a could not fetch tax code mismatch" when {
      "invalid response is returned fromm taxCodeChangeConnector.taxCodeMismatch" in {
        val sut = createSut
        val nino = generateNino

        when(sut.taxCodeChangeConnector.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(true)))
        when(sut.taxCodeChangeConnector.taxCodeMismatch(any())(any())).thenReturn(Future.successful(TaiTaxAccountFailureResponse("ERROR")))

        val ex = the[RuntimeException] thrownBy Await.result(sut.hasTaxCodeChanged(nino), 5 seconds)
        ex.getMessage must include("Could not fetch tax code mismatch")
      }
    }

    "throws a could not fetch tax code mismatch" when {
      "invalid response is returned fromm taxCodeChangeConnector.hasTaxCodeChanged" in {
        val sut = createSut
        val nino = generateNino

        val taxCodeMismatch = TaxCodeMismatchFactory.matchedTaxCode

        when(sut.taxCodeChangeConnector.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(TaiTaxAccountFailureResponse("ERROR")))
        when(sut.taxCodeChangeConnector.taxCodeMismatch(any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeMismatch)))

        val ex = the[RuntimeException] thrownBy Await.result(sut.hasTaxCodeChanged(nino), 5 seconds)
        ex.getMessage must include("Could not fetch tax code change")
      }
    }
  }

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("code", startDate, startDate.plusDays(1), OtherBasisOfOperation,"Employer 1", false, Some("1234"), true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(2), endDate = TaxYearResolver.endOfCurrentTaxYear)

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private def generateNino: Nino = new Generator(new Random).nextNino
  private def createSut = new TestService

  private class TestService extends TaxCodeChangeService {
    override val taxCodeChangeConnector: TaxCodeChangeConnector = mock[TaxCodeChangeConnector]
  }
}
