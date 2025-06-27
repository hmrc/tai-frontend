/*
 * Copyright 2025 HM Revenue & Customs
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
import cats.instances.future.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{BadRequestException, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class TaxCodeChangeServiceSpec extends BaseSpec {

  "taxCodeChange" must {
    "return the tax code change given a valid nino" in {
      val testService = createTestService

      val taxCodeChange = TaxCodeChange(List(taxCodeRecord1), List(taxCodeRecord2))

      when(taxCodeChangeConnector.taxCodeChange(any())(any()))
        .thenReturn(EitherT.rightT(taxCodeChange))

      val result = testService.taxCodeChange(nino).value
      Await.result(result, 5.seconds) mustBe Right(taxCodeChange)
    }
  }

  "has tax code changed" must {
    "return a HasTaxCodeChanged object" when {
      "success response from the connectors" in {
        val testService = createTestService

        when(taxCodeChangeConnector.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT(true))

        val result = testService.hasTaxCodeChanged(nino).value
        Await.result(result, 5.seconds) mustBe Right(true)
      }
    }

    "returns hasTaxCodeChanged = false" when {
      "invalid response is returned from taxCodeChangeConnector.taxCodeMismatch" in {
        val testService = createTestService

        when(taxCodeChangeConnector.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.rightT(true))

        val result = testService.hasTaxCodeChanged(nino).value
        Await.result(result, 5.seconds) mustBe Right(true)
      }
    }

    "and could not fetch tax code mismatch" when {
      "returns a valid error response from taxCodeChangeConnector.hasTaxCodeChanged" in {
        val testService = createTestService

        val error        = UpstreamErrorResponse("server error", INTERNAL_SERVER_ERROR)
        val taxCodeError = Left(error)

        when(taxCodeChangeConnector.hasTaxCodeChanged(any())(any()))
          .thenReturn(EitherT.leftT(error))

        Await.result(testService.hasTaxCodeChanged(nino).value, 5 seconds) mustBe taxCodeError
      }
    }
  }

  "lastTaxCodeRecordsInYearPerEmployment" must {
    "return a sequence of TaxCodeRecords when given a valid nino and year" in {
      val testService    = createTestService
      val taxCodeRecords = List(taxCodeRecord1)

      when(taxCodeChangeConnector.lastTaxCodeRecords(any(), any())(any()))
        .thenReturn(Future.successful(taxCodeRecords))

      val result = testService.lastTaxCodeRecordsInYearPerEmployment(nino, TaxYear().prev)
      Await.result(result, 5.seconds) mustBe taxCodeRecords
    }
  }

  "hasTaxCodeRecordsInYearPerEmployment" must {
    "return true when a nonEmpty sequence of tax code records is returned" in {
      val testService = createTestService

      val taxCodeRecords = List(taxCodeRecord1)

      when(taxCodeChangeConnector.lastTaxCodeRecords(any(), any())(any()))
        .thenReturn(Future.successful(taxCodeRecords))

      val result = testService.hasTaxCodeRecordsInYearPerEmployment(nino, TaxYear().prev)
      Await.result(result, 5.seconds) mustBe true
    }

    "return false when an empty sequence of tax code records is returned" in {
      val testService = createTestService

      val taxCodeRecords = List()

      when(taxCodeChangeConnector.lastTaxCodeRecords(any(), any())(any()))
        .thenReturn(Future.successful(taxCodeRecords))

      val result = testService.hasTaxCodeRecordsInYearPerEmployment(nino, TaxYear().prev)
      Await.result(result, 5.seconds) mustBe false
    }

    "return false when a BadRequestException is returned" in {
      val testService = createTestService

      when(taxCodeChangeConnector.lastTaxCodeRecords(any(), any())(any()))
        .thenReturn(Future.failed(new BadRequestException("Bad Request")))

      val result = testService.hasTaxCodeRecordsInYearPerEmployment(nino, TaxYear().prev)
      Await.result(result, 5.seconds) mustBe false
    }
  }

  val startDate: LocalDate          = TaxYear().start
  val taxCodeRecord1: TaxCodeRecord = TaxCodeRecord(
    "code",
    startDate,
    startDate.plusDays(1),
    OtherBasisOfOperation,
    "Employer 1",
    false,
    Some("1234"),
    true
  )
  val taxCodeRecord2: TaxCodeRecord = taxCodeRecord1.copy(startDate = startDate.plusDays(2), endDate = TaxYear().end)

  private def createTestService = new TestService

  val taxCodeChangeConnector: TaxCodeChangeConnector = mock[TaxCodeChangeConnector]

  private class TestService extends TaxCodeChangeService(taxCodeChangeConnector, ec)
}
