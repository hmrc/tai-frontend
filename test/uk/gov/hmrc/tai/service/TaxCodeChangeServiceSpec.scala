/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{HasTaxCodeChanged, TaxCodeChange, TaxCodeRecord}
import utils.BaseSpec
import utils.factories.TaxCodeMismatchFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TaxCodeChangeServiceSpec extends BaseSpec {

  "taxCodeChange" must {
    "return the tax code change given a valid nino" in {
      val testService = createTestService

      val taxCodeChange = TaxCodeChange(List(taxCodeRecord1), List(taxCodeRecord2))

      when(taxCodeChangeConnector.taxCodeChange(any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeChange)))

      val result = testService.taxCodeChange(nino)
      Await.result(result, 5.seconds) mustBe taxCodeChange
    }
  }

  "has tax code changed" must {
    "return a HasTaxCodeChanged object" when {
      "success response from the connectors" in {
        val testService = createTestService

        val taxCodeMismatch = TaxCodeMismatchFactory.matchedTaxCode
        val hasTaxCodeChanged = HasTaxCodeChanged(changed = true, Some(taxCodeMismatch))

        when(taxCodeChangeConnector.hasTaxCodeChanged(any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(true)))
        when(taxCodeChangeConnector.taxCodeMismatch(any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeMismatch)))

        val result = testService.hasTaxCodeChanged(nino)
        Await.result(result, 5.seconds) mustBe hasTaxCodeChanged
      }
    }

    "returns hasTaxCodeChanged = false" when {
      "invalid response is returned fromm taxCodeChangeConnector.taxCodeMismatch" in {
        val testService = createTestService

        val hasTaxCodeChanged = HasTaxCodeChanged(changed = false, None)

        when(taxCodeChangeConnector.hasTaxCodeChanged(any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(true)))
        when(taxCodeChangeConnector.taxCodeMismatch(any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse("ERROR")))

        val result = testService.hasTaxCodeChanged(nino)
        Await.result(result, 5.seconds) mustBe hasTaxCodeChanged
      }
    }

    "throws a could not fetch tax code mismatch" when {
      "invalid response is returned fromm taxCodeChangeConnector.hasTaxCodeChanged" in {
        val testService = createTestService

        val taxCodeMismatch = TaxCodeMismatchFactory.matchedTaxCode

        when(taxCodeChangeConnector.hasTaxCodeChanged(any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse("ERROR")))
        when(taxCodeChangeConnector.taxCodeMismatch(any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeMismatch)))

        val ex = the[RuntimeException] thrownBy Await.result(testService.hasTaxCodeChanged(nino), 5 seconds)
        ex.getMessage must include("Could not fetch tax code change")
      }
    }
  }

  "lastTaxCodeRecordsInYearPerEmployment" must {
    "return a sequence of TaxCodeRecords when given a valid nino and year" in {
      val testService = createTestService

      val taxCodeRecords = Seq(taxCodeRecord1)

      when(taxCodeChangeConnector.lastTaxCodeRecords(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeRecords)))

      val result = testService.lastTaxCodeRecordsInYearPerEmployment(nino, TaxYear().prev)
      Await.result(result, 5.seconds) mustBe taxCodeRecords
    }

    "throw an exception if could not get last tax codes for given year" in {
      val testService = createTestService

      when(taxCodeChangeConnector.lastTaxCodeRecords(any(), any())(any()))
        .thenReturn(Future.successful(TaiTaxAccountFailureResponse("ERROR")))

      val ex = the[RuntimeException] thrownBy Await
        .result(testService.lastTaxCodeRecordsInYearPerEmployment(nino, TaxYear().prev), 5 seconds)
      ex.getMessage must include(s"Could not fetch last tax code records for year ${TaxYear().prev}")
    }
  }

  "hasTaxCodeRecordsInYearPerEmployment" must {
    "return true when a nonEmpty sequence of tax code records is returned" in {
      val testService = createTestService

      val taxCodeRecords = Seq(taxCodeRecord1)

      when(taxCodeChangeConnector.lastTaxCodeRecords(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeRecords)))

      val result = testService.hasTaxCodeRecordsInYearPerEmployment(nino, TaxYear().prev)
      Await.result(result, 5.seconds) mustBe true
    }

    "return false when a empty sequence of tax code records is returned" in {
      val testService = createTestService

      val taxCodeRecords = Seq()

      when(taxCodeChangeConnector.lastTaxCodeRecords(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeRecords)))

      val result = testService.hasTaxCodeRecordsInYearPerEmployment(nino, TaxYear().prev)
      Await.result(result, 5.seconds) mustBe false
    }

    "return false when a TaiTaxAccountFailureResponse is returned" in {
      val testService = createTestService

      when(taxCodeChangeConnector.lastTaxCodeRecords(any(), any())(any()))
        .thenReturn(Future.successful(TaiTaxAccountFailureResponse("ERROR")))

      val result = testService.hasTaxCodeRecordsInYearPerEmployment(nino, TaxYear().prev)
      Await.result(result, 5.seconds) mustBe false
    }
  }

  val startDate = TaxYear().start
  val taxCodeRecord1 = TaxCodeRecord(
    "code",
    startDate,
    startDate.plusDays(1),
    OtherBasisOfOperation,
    "Employer 1",
    false,
    Some("1234"),
    true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(2), endDate = TaxYear().end)

  private def createTestService = new TestService

  val taxCodeChangeConnector = mock[TaxCodeChangeConnector]

  private class TestService extends TaxCodeChangeService(taxCodeChangeConnector)
}
