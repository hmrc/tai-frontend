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

package uk.gov.hmrc.tai.connectors

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{CarBenefit, TaxFreeAmountComparison}
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TaxFreeAmountComparisonConnectorSpec extends BaseSpec {

  "tax free amount url" must {
    "fetch the url to connect to TAI to retrieve tax free amount comparison" in {
      sut.taxFreeAmountComparisonUrl(nino.nino) mustBe s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/tax-free-amount-comparison"
    }
  }

  "taxFreeAmountComparison" must {
    "return a tax free amount comparison" when {
      "the api responds with valid json" in {
        val json = Json.obj(
          "data" -> Json.obj(
            "previous" -> Json.arr(
              Json.obj(
                "componentType" -> CarBenefit,
                "employmentId"  -> 1,
                "amount"        -> 1,
                "description"   -> "Car Benefit",
                "iabdCategory"  -> "Benefit",
                "inputAmount"   -> 1
              )
            ),
            "current" -> Json.arr(
              Json.obj(
                "componentType" -> CarBenefit,
                "employmentId"  -> 1,
                "amount"        -> 1,
                "description"   -> "Car Benefit",
                "iabdCategory"  -> "Benefit",
                "inputAmount"   -> 1
              )
            )
          )
        )

        val taxFreeAmountUrl = s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/tax-free-amount-comparison"

        when(httpHandler.getFromApi(Matchers.eq(taxFreeAmountUrl))(any())).thenReturn(Future.successful(json))

        val codingComponents = Seq(CodingComponent(CarBenefit, Some(1), 1, "Car Benefit", Some(1)))

        val expected = TaiSuccessResponseWithPayload(TaxFreeAmountComparison(codingComponents, codingComponents))

        val result = sut.taxFreeAmountComparison(nino)

        Await.result(result, 5.seconds) mustBe expected
      }
    }

    "return a TaiTaxAccountFailureResponse" when {
      "the api responds with invalid json" in {
        val exceptionMessage = "bad request"
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new BadRequestException(exceptionMessage)))

        val expected = TaiTaxAccountFailureResponse(exceptionMessage)

        val result = sut.taxFreeAmountComparison(nino)

        Await.result(result, 5.seconds) mustBe expected
      }
    }
  }

  val httpHandler: HttpHandler = mock[HttpHandler]

  def sut = new TaxFreeAmountComparisonConnector(httpHandler, servicesConfig)

}
