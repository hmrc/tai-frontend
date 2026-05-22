/*
 * Copyright 2026 HM Revenue & Customs
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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.*
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.connectors.RtiConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.*
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.Future

class RtiServiceSpec extends BaseSpec {

  val mockRtiConnector: RtiConnector = mock[RtiConnector]

  val sut = new RtiService(mockRtiConnector)

  private val payment = Payment(
    date = LocalDate.now(),
    amountYearToDate = 2000,
    taxAmountYearToDate = 200,
    nationalInsuranceAmountYearToDate = 100,
    amount = 1000,
    taxAmount = 100,
    nationalInsuranceAmount = 50,
    payFrequency = Monthly,
    duplicate = None
  )

  private val availableAnnualAccount = AnnualAccount(
    sequenceNumber = 1,
    taxYear = TaxYear(),
    realTimeStatus = Available,
    payments = Seq(payment),
    endOfTaxYearUpdates = Nil
  )

  private val temporarilyUnavailableAnnualAccount = AnnualAccount(
    sequenceNumber = 0,
    taxYear = TaxYear(),
    realTimeStatus = TemporarilyUnavailable,
    payments = Nil,
    endOfTaxYearUpdates = Nil
  )

  "getAllPaymentsForYear" must {
    "return all payments" in {
      val annualAccounts = Seq(availableAnnualAccount)

      when(mockRtiConnector.getAllPaymentsForYear(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](annualAccounts))

      val result = sut.getAllPaymentsForYear(nino, TaxYear()).value.futureValue

      result mustBe Right(annualAccounts)
    }
  }

  "getPaymentsForEmploymentAndYear" must {
    "return matching employment annual account" in {
      when(mockRtiConnector.getAllPaymentsForYear(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq(availableAnnualAccount)))

      val result = sut.getPaymentsForEmploymentAndYear(nino, TaxYear(), 1).value.futureValue

      result mustBe Right(Some(availableAnnualAccount))
    }

    "return TemporarilyUnavailable account when no matching employment payments exist and marker account is present" in {

      when(mockRtiConnector.getAllPaymentsForYear(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq(temporarilyUnavailableAnnualAccount)))

      val result = sut.getPaymentsForEmploymentAndYear(nino, TaxYear(), 1).value.futureValue

      result mustBe Right(Some(temporarilyUnavailableAnnualAccount))
    }

    "return None when no employment payments exist" in {
      when(mockRtiConnector.getAllPaymentsForYear(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty))

      val result = sut.getPaymentsForEmploymentAndYear(nino, TaxYear(), 1).value.futureValue

      result mustBe Right(None)
    }

    List(
      BAD_REQUEST,
      TOO_MANY_REQUESTS,
      REQUEST_TIMEOUT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE,
      BAD_GATEWAY
    ).foreach { errorResponse =>
      s"return an UpstreamErrorResponse containing $errorResponse when connector returns the same" in {
        when(mockRtiConnector.getAllPaymentsForYear(any(), any())(any())).thenReturn(
          EitherT.leftT[Future, Seq[AnnualAccount]](UpstreamErrorResponse("", errorResponse))
        )

        val result = sut.getPaymentsForEmploymentAndYear(nino, TaxYear(), 1).value.futureValue

        result mustBe a[Left[UpstreamErrorResponse, _]]
        result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe errorResponse
      }
    }
  }
}
