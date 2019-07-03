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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import controllers.FakeTaiPlayApplication
import org.mockito.Matchers
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{CarBenefit, TaxFreeAmountComparison}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random

class TaxFreeAmountComparisonConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "tax free amount url" must {
    "fetch the url to connect to TAI to retrieve tax free amount comparison" in {
      val testConnector = new TestTaxFreeAmountComparisonConnector
      val nino = new Generator(new Random).nextNino

      testConnector.taxFreeAmountComparisonUrl(nino.nino) mustBe s"${testConnector.serviceUrl}/tai/$nino/tax-account/tax-free-amount-comparison"
    }
  }

  "taxFreeAmountComparison" must {
    "return a tax free amount comparison" when {
      "the api responds with valid json" in {
        val testConnector = new TestTaxFreeAmountComparisonConnector
        val nino = new Generator(new Random).nextNino

        val json = Json.obj(
          "data" -> Json.obj(
            "previous" -> Json.arr(
              Json.obj(
                "componentType" -> CarBenefit,
                "employmentId" -> 1,
                "amount" -> 1,
                "description" -> "Car Benefit",
                "iabdCategory" -> "Benefit",
                "inputAmount" -> 1
              )
            ),
            "current" ->  Json.arr(
              Json.obj(
                "componentType" -> CarBenefit,
                "employmentId" -> 1,
                "amount" -> 1,
                "description" -> "Car Benefit",
                "iabdCategory" -> "Benefit",
                "inputAmount" -> 1
              )
            )
          )
        )

        val taxFreeAmountUrl = s"${testConnector.serviceUrl}/tai/${nino.nino}/tax-account/tax-free-amount-comparison"

        when(httpHandler.getFromApi(Matchers.eq(taxFreeAmountUrl))(any())).thenReturn(Future.successful(json))

        val codingComponents = Seq(CodingComponent(CarBenefit, Some(1), 1, "Car Benefit", Some(1)))

        val expected = TaiSuccessResponseWithPayload(TaxFreeAmountComparison(codingComponents, codingComponents))

        val result = testConnector.taxFreeAmountComparison(nino)

        Await.result(result, 5.seconds) mustBe expected
      }
    }

    "return a TaiTaxAccountFailureResponse" when {
      "the api responds with invalid json" in {
        val testConnector = new TestTaxFreeAmountComparisonConnector
        val nino = new Generator(new Random).nextNino
        val exceptionMessage = "bad request"
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new BadRequestException(exceptionMessage)))

        val expected = TaiTaxAccountFailureResponse(exceptionMessage)

        val result = testConnector.taxFreeAmountComparison(nino)

        Await.result(result, 5.seconds) mustBe expected
      }
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val httpHandler: HttpHandler = mock[HttpHandler]

  class TestTaxFreeAmountComparisonConnector extends TaxFreeAmountComparisonConnector(httpHandler)

}
