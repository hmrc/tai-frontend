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

package uk.gov.hmrc.tai.connectors

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import utils.factories.TaxCodeMismatchFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class TaxCodeChangeConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "tax code change url" must {
    "fetch the url to connect to TAI to retrieve tax code change" in {
      val testConnector = createTestConnector
      val nino = generateNino.nino

      testConnector.taxCodeChangeUrl(nino) mustBe s"${testConnector.serviceUrl}/tai/$nino/tax-account/tax-code-change"
    }
  }

  "tax Code change" should {
    "fetch the tax code change" when {
      "provided with valid nino" in {

        val testConnector = createTestConnector
        val nino = generateNino

        val taxCodeChangeUrl = s"${testConnector.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-change"

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

        val json = Json.obj(
          "data" -> Json.obj(
            "previous" -> Json.arr(
              Json.obj(
                "taxCode"          -> "code",
                "startDate"        -> startDate,
                "endDate"          -> startDate.plusDays(1),
                "basisOfOperation" -> "Cumulative",
                "employerName"     -> "Employer 1",
                "pensionIndicator" -> false,
                "payrollNumber"    -> "1234",
                "primary"          -> true
              )
            ),
            "current" -> Json.arr(
              Json.obj(
                "taxCode"          -> "code",
                "startDate"        -> startDate.plusDays(2),
                "endDate"          -> TaxYear().end,
                "basisOfOperation" -> "Cumulative",
                "employerName"     -> "Employer 1",
                "pensionIndicator" -> false,
                "payrollNumber"    -> "1234",
                "primary"          -> true
              )
            )
          ),
          "links" -> JsArray(Seq())
        )

        val expectedResult = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))
        when(httpHandler.getFromApi(Matchers.eq(taxCodeChangeUrl))(any())).thenReturn(Future.successful(json))

        val result = Await.result(testConnector.taxCodeChange(nino), 5 seconds)
        result mustEqual TaiSuccessResponseWithPayload(expectedResult)
      }
    }

    "return failure" when {
      "tax code change returns 500" in {
        val testConnector = createTestConnector
        val nino = generateNino

        val taxCodeChangeUrl = s"${testConnector.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-change"

        val expectedMessage = s"GET of '$taxCodeChangeUrl' returned 500. Response body: ''"

        when(httpHandler.getFromApi(Matchers.eq(taxCodeChangeUrl))(any()))
          .thenReturn(Future.failed(new RuntimeException(expectedMessage)))

        val result = Await.result(testConnector.taxCodeChange(nino), 5.seconds)

        result mustBe TaiTaxAccountFailureResponse(expectedMessage)
      }
    }
  }

  "lastTaxCodeRecords" must {
    "fetch the most recent tax code records when given valid nino and year" in {
      val testConnector = createTestConnector
      val nino = generateNino
      val year = TaxYear().prev.year

      val latestTaxCodeRecordUrl = s"${testConnector.serviceUrl}/tai/${nino.nino}/tax-account/$year/tax-code/latest"

      val startDate = TaxYear().start
      val taxCodeRecord = TaxCodeRecord(
        "code",
        startDate,
        startDate.plusDays(1),
        OtherBasisOfOperation,
        "Employer 1",
        false,
        Some("1234"),
        true)
      val taxCodeRecord2 = TaxCodeRecord(
        "code2",
        startDate,
        startDate.plusDays(1),
        OtherBasisOfOperation,
        "Employer 2",
        false,
        Some("1239"),
        true)

      val json = Json.obj(
        "data" -> Json.arr(
          Json.obj(
            "taxCode"          -> "code",
            "startDate"        -> startDate,
            "endDate"          -> startDate.plusDays(1),
            "basisOfOperation" -> "Cumulative",
            "employerName"     -> "Employer 1",
            "pensionIndicator" -> false,
            "payrollNumber"    -> "1234",
            "primary"          -> true
          ),
          Json.obj(
            "taxCode"          -> "code2",
            "startDate"        -> startDate,
            "endDate"          -> startDate.plusDays(1),
            "basisOfOperation" -> "Cumulative",
            "employerName"     -> "Employer 2",
            "pensionIndicator" -> false,
            "payrollNumber"    -> "1239",
            "primary"          -> true
          )
        ),
        "links" -> JsArray(Seq())
      )

      val expectedResult = Seq(taxCodeRecord, taxCodeRecord2)

      when(httpHandler.getFromApi(Matchers.eq(latestTaxCodeRecordUrl))(any())).thenReturn(Future.successful(json))

      val result = Await.result(testConnector.lastTaxCodeRecords(nino, TaxYear().prev), 5 seconds)
      result mustEqual TaiSuccessResponseWithPayload(expectedResult)
    }

    "return a empty sequence when the api returns no records" in {
      val testConnector = createTestConnector
      val nino = generateNino
      val year = TaxYear().prev.year

      val latestTaxCodeRecordUrl = s"${testConnector.serviceUrl}/tai/${nino.nino}/tax-account/$year/tax-code/latest"

      val json = Json.obj(
        "data"  -> Json.arr(),
        "links" -> JsArray(Seq())
      )

      when(httpHandler.getFromApi(Matchers.eq(latestTaxCodeRecordUrl))(any())).thenReturn(Future.successful(json))

      val result = Await.result(testConnector.lastTaxCodeRecords(nino, TaxYear().prev), 5 seconds)
      result mustEqual TaiSuccessResponseWithPayload(Seq.empty)
    }
  }

  "has tax code changed" must {
    "tax code change url" must {
      "fetch the url to connect to TAI to retrieve tax code change" in {
        val testConnector = createTestConnector
        val nino = generateNino.nino

        testConnector.hasTaxCodeChangedUrl(nino) mustBe s"${testConnector.serviceUrl}/tai/$nino/tax-account/tax-code-change/exists"
      }
    }
  }

  "tax Code changed" should {
    "fetch if the tax code has changed" when {
      "provided with valid nino" in {

        val testConnector = createTestConnector
        val nino = generateNino
        val hasTaxCodeChangedUrl = s"${testConnector.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-change/exists"

        val json = Future.successful(Json.toJson(true))

        when(httpHandler.getFromApi(Matchers.eq(hasTaxCodeChangedUrl))(any())).thenReturn(json)

        val result = Await.result(testConnector.hasTaxCodeChanged(nino), 5 seconds)
        result mustEqual TaiSuccessResponseWithPayload(true)
      }
    }
  }

  "taxCodeMismatchUrl" must {
    "tax code change url" must {
      "fetch the url to connect to TAI to retrieve tax code change" in {
        val testConnector = createTestConnector
        val nino = generateNino.nino

        testConnector.hasTaxCodeChangedUrl(nino) mustBe s"${testConnector.serviceUrl}/tai/$nino/tax-account/tax-code-change/exists"
      }
    }
  }

  "taxCodeMismatch" should {
    "return if the is a tax code is umatched" when {
      "provided with a valid nino" in {

        val testConnector = createTestConnector
        val nino = generateNino

        val expectedResult = TaxCodeMismatchFactory.matchedTaxCode
        val json = Future.successful(TaxCodeMismatchFactory.matchedTaxCodeJson)

        val url = s"${testConnector.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-mismatch"
        when(httpHandler.getFromApi(Matchers.eq(url))(any())).thenReturn(json)

        val result = Await.result(testConnector.taxCodeMismatch(nino), 5 seconds)
        result mustEqual TaiSuccessResponseWithPayload(expectedResult)
      }
    }

    "return if the is a tax code matched" when {
      "provided with a valid nino" in {

        val testConnector = createTestConnector
        val nino = generateNino

        val expectedResult = TaxCodeMismatchFactory.mismatchedTaxCode
        val json = Future.successful(TaxCodeMismatchFactory.mismatchedTaxCodeJson)

        val url = s"${testConnector.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-mismatch"
        when(httpHandler.getFromApi(Matchers.eq(url))(any())).thenReturn(json)

        val result = Await.result(testConnector.taxCodeMismatch(nino), 5 seconds)
        result mustEqual TaiSuccessResponseWithPayload(expectedResult)
      }
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def createTestConnector = new TaxCodeChangeConnector(httpHandler)

  private def generateNino: Nino = new Generator(new Random).nextNino

  val httpHandler: HttpHandler = mock[HttpHandler]

}
