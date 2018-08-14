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

package uk.gov.hmrc.tai.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, serverError, urlEqualTo}
import controllers.FakeTaiPlayApplication
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.{TaxCodeHistory, TaxCodeRecord}
import uk.gov.hmrc.time.TaxYearResolver
import utils.WireMockHelper

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class TaxCodeChangeConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with WireMockHelper {

  "tax code history url" must {
    "fetch the url to connect to TAI to retrieve tax code history" in {
      val testConnector = createTestConnector
      val nino = generateNino.nino

      testConnector.taxCodeHistoryUrl(nino) mustBe s"${testConnector.serviceUrl}/tai/$nino/tax-account/tax-code-change"

    }
  }

  "tax Code history" should {
    "fetch the tax code history" when {
      "provided with valid nino" in {

        val testConnector = createTestConnector
        val nino = generateNino

        val taxCodeHistoryUrl = s"/tai/${nino.nino}/tax-account/tax-code-change"

        val startDate = TaxYearResolver.startOfCurrentTaxYear
        val taxCodeRecord1 = TaxCodeRecord("code", startDate, startDate.plusDays(1),"Employer 1")
        val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(2), endDate = TaxYearResolver.endOfCurrentTaxYear)

        val json = Json.obj(
          "data" -> Json.obj(
            "previous" -> Json.obj(
              "taxCode" -> "code",
              "startDate" -> startDate,
              "endDate" -> startDate.plusDays(1),
              "employerName" -> "Employer 1"
            ),
            "current" -> Json.obj(
              "taxCode" -> "code",
              "startDate" -> startDate.plusDays(2),
              "endDate" -> TaxYearResolver.endOfCurrentTaxYear,
              "employerName" -> "Employer 1"
            )
          ),
          "links" -> JsArray(Seq()))

        val expectedResult = TaxCodeHistory(taxCodeRecord1, taxCodeRecord2)

        server.stubFor(
          get(urlEqualTo(taxCodeHistoryUrl)).willReturn(ok(json.toString()))
        )

        val result = Await.result(testConnector.taxCodeHistory(nino), 5 seconds)
        result mustEqual TaiSuccessResponseWithPayload(expectedResult)
      }
    }

    "return failure" when {
      "tax code history returns 500" in {
        val testConnector = createTestConnector
        val nino = generateNino

        val taxCodeHistoryUrl = s"/tai/${nino.nino}/tax-account/tax-code-change"

        server.stubFor(
          get(urlEqualTo(taxCodeHistoryUrl)).willReturn(serverError())
        )

        val expectedMessage = s"GET of '${testConnector.serviceUrl}/tai/$nino/tax-account/tax-code-change' returned 500. Response body: ''"
        val result = Await.result(testConnector.taxCodeHistory(nino), 5 seconds)

        result mustBe TaiTaxAccountFailureResponse(expectedMessage)
      }
    }
  }


  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def createTestConnector = new testTaxCodeChangeConnector

  private def generateNino: Nino = new Generator(new Random).nextNino

  private class testTaxCodeChangeConnector extends TaxCodeChangeConnector {
    override val serviceUrl: String = s"http://localhost:${server.port()}"
    override val httpHandler: HttpHandler = HttpHandler
  }

}
