/*
 * Copyright 2024 HM Revenue & Customs
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
import org.mockito.Mockito
import org.mockito.Mockito.when
import play.api.libs.json.{JsResultException, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.connectors.IabdConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{IabdDetails, ManualTelephone}
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.Future

class IabdServiceSpec extends BaseSpec {

  val mockIabdConnector: IabdConnector = mock[IabdConnector]
  val sut                              = new IabdService(mockIabdConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockIabdConnector)
  }

  "getIbabds" must {
    "parse an expected iabd response into a seq[IabdDetails]" in {
      when(mockIabdConnector.getIabds(any(), any())(any())).thenReturn(
        EitherT.rightT[Future, UpstreamErrorResponse](
          Json.obj(
            "data" -> Json.obj(
              "iabdDetails" -> Json.arr(
                Json.obj(
                  "employmentSequenceNumber" -> 1,
                  "source"                   -> "ManualTelephone",
                  "type"                     -> 1,
                  "receiptDate"              -> "2026-01-01",
                  "captureDate"              -> "2026-01-01",
                  "grossAmount"              -> 200.4
                ),
                Json.obj()
              )
            )
          )
        )
      )

      val result = sut.getIabds(nino, TaxYear()).value.futureValue

      result mustBe Right(
        Seq(
          IabdDetails(
            Some(1),
            Some(ManualTelephone),
            Some(1),
            Some(LocalDate.of(2026, 1, 1)),
            Some(LocalDate.of(2026, 1, 1)),
            Some(BigDecimal(200.40))
          ),
          IabdDetails(None, None, None, None, None)
        )
      )
    }

    "throw an exception on invalid json" in {
      when(mockIabdConnector.getIabds(any(), any())(any())).thenReturn(
        EitherT.rightT[Future, UpstreamErrorResponse](Json.obj("invalid" -> true))
      )

      val result = intercept[JsResultException] {
        await(sut.getIabds(nino, TaxYear()).value)
      }

      result.getMessage mustBe "JsResultException(errors:List((,List(JsonValidationError(List('data' is undefined on object. Available keys are 'invalid'),ArraySeq())))))"
    }
  }

}
