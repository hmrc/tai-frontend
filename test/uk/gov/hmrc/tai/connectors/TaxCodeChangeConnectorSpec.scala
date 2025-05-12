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

package uk.gov.hmrc.tai.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{get, ok}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.when
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import utils.{BaseSpec, WireMockHelper}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class TaxCodeChangeConnectorSpec extends BaseSpec with WireMockHelper {

  private def sut = new TaxCodeChangeConnector(httpHandler, servicesConfig, httpClientResponse)

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure("microservice.services.tai.port" -> server.port)
    .build()

  val httpHandler: HttpHandler = mock[HttpHandler]
  val httpClientResponse: HttpClientResponse = inject[HttpClientResponse]

  "tax code change url" must {
    "fetch the url to connect to TAI to retrieve tax code change" in {
      sut.taxCodeChangeUrl(nino.nino) mustBe s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-change"
    }
  }

  "tax Code change" should {
    "fetch the tax code change" when {
      "provided with valid nino" in {

        val taxCodeChangeUrl = s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-change"

        val startDate = TaxYear().start
        val taxCodeRecord1 = TaxCodeRecord(
          "code",
          startDate,
          startDate.plusDays(1),
          OtherBasisOfOperation,
          "Employer 1",
          pensionIndicator = false,
          Some("1234"),
          primary = true
        )
        val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(2), endDate = TaxYear().end)

        val json = Json.obj(
          "data" -> Json.obj(
            "previous" -> Json.arr(
              Json.obj(
                "taxCode"          -> "code",
                "startDate"        -> startDate.toString(),
                "endDate"          -> startDate.plusDays(1).toString(),
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
                "startDate"        -> startDate.plusDays(2).toString(),
                "endDate"          -> TaxYear().end.toString(),
                "basisOfOperation" -> "Cumulative",
                "employerName"     -> "Employer 1",
                "pensionIndicator" -> false,
                "payrollNumber"    -> "1234",
                "primary"          -> true
              )
            )
          ),
          "links" -> JsArray(List())
        )

        val expectedResult = TaxCodeChange(List(taxCodeRecord1), List(taxCodeRecord2))
        when(httpHandler.getFromApiV2(meq(taxCodeChangeUrl), any())(any(), any())).thenReturn(Future.successful(json))

        val result = Await.result(sut.taxCodeChange(nino), 5 seconds)
        result mustEqual expectedResult
      }
    }

    "throw RuntimeException" when {
      "tax code change returns 500" in {

        val taxCodeChangeUrl = s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-change"

        val expectedMessage = s"GET of '$taxCodeChangeUrl' returned 500. Response body: ''"

        when(httpHandler.getFromApiV2(meq(taxCodeChangeUrl), any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException(expectedMessage)))

        val ex = the[RuntimeException] thrownBy Await.result(sut.taxCodeChange(nino), 5 seconds)
        ex.getMessage must include(s"GET of '$taxCodeChangeUrl' returned 500. Response body: ''")
      }
    }
  }

  "lastTaxCodeRecords" must {
    "fetch the most recent tax code records when given valid nino and year" in {
      val year = TaxYear().prev.year

      val latestTaxCodeRecordUrl = s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/$year/tax-code/latest"

      val startDate = TaxYear().start
      val taxCodeRecord = TaxCodeRecord(
        "code",
        startDate,
        startDate.plusDays(1),
        OtherBasisOfOperation,
        "Employer 1",
        pensionIndicator = false,
        Some("1234"),
        primary = true
      )
      val taxCodeRecord2 = TaxCodeRecord(
        "code2",
        startDate,
        startDate.plusDays(1),
        OtherBasisOfOperation,
        "Employer 2",
        pensionIndicator = false,
        Some("1239"),
        primary = true
      )

      val json = Json.obj(
        "data" -> Json.arr(
          Json.obj(
            "taxCode"          -> "code",
            "startDate"        -> startDate.toString(),
            "endDate"          -> startDate.plusDays(1).toString(),
            "basisOfOperation" -> "Cumulative",
            "employerName"     -> "Employer 1",
            "pensionIndicator" -> false,
            "payrollNumber"    -> "1234",
            "primary"          -> true
          ),
          Json.obj(
            "taxCode"          -> "code2",
            "startDate"        -> startDate.toString(),
            "endDate"          -> startDate.plusDays(1).toString(),
            "basisOfOperation" -> "Cumulative",
            "employerName"     -> "Employer 2",
            "pensionIndicator" -> false,
            "payrollNumber"    -> "1239",
            "primary"          -> true
          )
        ),
        "links" -> JsArray(List())
      )

      val expectedResult = List(taxCodeRecord, taxCodeRecord2)

      when(httpHandler.getFromApiV2(meq(latestTaxCodeRecordUrl), any())(any(), any()))
        .thenReturn(Future.successful(json))

      val result = Await.result(sut.lastTaxCodeRecords(nino, TaxYear().prev), 5 seconds)
      result mustEqual expectedResult
    }

    "return a empty List when the api returns no records" in {
      val year = TaxYear().prev.year

      val latestTaxCodeRecordUrl = s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/$year/tax-code/latest"

      val json = Json.obj(
        "data"  -> Json.arr(),
        "links" -> JsArray(List())
      )

      when(httpHandler.getFromApiV2(meq(latestTaxCodeRecordUrl), any())(any(), any()))
        .thenReturn(Future.successful(json))

      val result = Await.result(sut.lastTaxCodeRecords(nino, TaxYear().prev), 5 seconds)
      result mustEqual List.empty
    }

    "throw Exception" when {
      "tax code change returns 500" in {
        val year = TaxYear().prev.year
        val latestTaxCodeRecordUrl = s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/$year/tax-code/latest"
        val expectedMessage = s"Couldn't retrieve tax code records for $nino for year $year with exception: bad request"
        when(httpHandler.getFromApiV2(meq(latestTaxCodeRecordUrl), any())(any(), any()))
          .thenReturn(Future.failed(new BadRequestException(expectedMessage)))

        val expected = the[BadRequestException] thrownBy Await
          .result(sut.lastTaxCodeRecords(nino, TaxYear().prev), 5 seconds)
        expected.getMessage must include(expectedMessage)
      }
    }
  }

  "has tax code changed" must {
    "tax code change url" must {
      "fetch the url to connect to TAI to retrieve tax code change" in {
        sut.hasTaxCodeChangedUrl(
          nino.nino
        ) mustBe s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-change/exists"
      }
    }
  }

  "tax Code changed" should {
    "fetch if the tax code has changed" when {
      "provided with valid nino" in {

        lazy val sut: TaxCodeChangeConnector = app.injector.instanceOf[TaxCodeChangeConnector]
        val hasTaxCodeChangedUrl = s"/tai/${nino.nino}/tax-account/tax-code-change/exists"
        val json = true

        server.stubFor(
          get(hasTaxCodeChangedUrl)
            .willReturn(ok(json.toString))
        )

        val result = Await.result(sut.hasTaxCodeChanged(nino).value, 5 seconds)
        result mustBe Right(true)
      }
    }
  }

  "taxCodeMismatchUrl" must {
    "tax code change url" must {
      "fetch the url to connect to TAI to retrieve tax code change" in {
        sut.hasTaxCodeChangedUrl(
          nino.nino
        ) mustBe s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-change/exists"
      }
    }
  }
}
