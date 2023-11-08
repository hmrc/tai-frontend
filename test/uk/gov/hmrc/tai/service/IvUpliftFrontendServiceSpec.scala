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

package uk.gov.hmrc.tai.service

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.connectors.IvUpliftFrontendConnector
import uk.gov.hmrc.tai.model.{Incomplete, InsufficientEvidence, InvalidResponse, PrecondFailed, TechnicalIssue}
import utils.BaseSpec

import scala.concurrent.Future

class IvUpliftFrontendServiceSpec extends BaseSpec {

  private val ivUpliftFrontendConnector = mock[IvUpliftFrontendConnector]

  val ivUpliftFrontendService: IvUpliftFrontendService = new IvUpliftFrontendService(ivUpliftFrontendConnector)

  "IdentityVerificationFrontendService" when {
    "successful getIVJourneyStatus is called" must {
      List(
        Incomplete,
        InsufficientEvidence,
        TechnicalIssue,
        PrecondFailed,
        InvalidResponse
      ).foreach { identityVerificationResponse =>
        s"$identityVerificationResponse if returned by the connector within its json response" in {
          when(ivUpliftFrontendConnector.getIVJourneyStatus(any())(any(), any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, JsValue](
                Future.successful(
                  Right(
                    Json.obj("result" -> identityVerificationResponse.toString)
                  )
                )
              )
            )

          val result = ivUpliftFrontendService
            .getIVJourneyStatus("1234")
            .value
            .futureValue

          result mustBe a[Right[_, _]]
          result mustBe Right(identityVerificationResponse)

        }
      }
    }

    List(
      INTERNAL_SERVER_ERROR,
      UNAUTHORIZED
    ).foreach { statusCode =>
      s"return Left when a $statusCode is retrieved" in {
        when(ivUpliftFrontendConnector.getIVJourneyStatus(any())(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, JsValue](
              Future.successful(
                Left(
                  UpstreamErrorResponse("", statusCode)
                )
              )
            )
          )

        val result = ivUpliftFrontendService
          .getIVJourneyStatus("1234")
          .value
          .futureValue

        result mustBe a[Left[_, _]]
        result mustBe UpstreamErrorResponse(_: String, statusCode)
      }
    }
  }

}
