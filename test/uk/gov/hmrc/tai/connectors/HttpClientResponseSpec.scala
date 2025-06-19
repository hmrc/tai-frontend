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

import cats.data.EitherT
import org.scalatest.RecoverMethods
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import utils.{BaseSpec, WireMockHelper}

import scala.concurrent.Future

class HttpClientResponseSpec
    extends BaseSpec
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with RecoverMethods
    with LogCapturing
    with Logging {

  private lazy val httpClientResponseUsingMockLogger: HttpClientResponse = new HttpClientResponse() with Logging

  private val dummyContent = "error message"

  "read" must {
    behave like clientResponseLogger(
      httpClientResponseUsingMockLogger.read,
      infoLevel = Set(NOT_FOUND),
      errorLevelWithThrowable = Set(UNPROCESSABLE_ENTITY, UNAUTHORIZED, FORBIDDEN),
      errorLevelWithoutThrowable = Set(TOO_MANY_REQUESTS, INTERNAL_SERVER_ERROR)
    )
  }

  private def clientResponseLogger(
    block: Future[Either[UpstreamErrorResponse, HttpResponse]] => EitherT[Future, UpstreamErrorResponse, HttpResponse],
    infoLevel: Set[Int],
    errorLevelWithThrowable: Set[Int],
    errorLevelWithoutThrowable: Set[Int]
  ): Unit = {
    infoLevel.foreach { httpResponseCode =>
      s"log message: INFO level only when response code is $httpResponseCode" in {
        withCaptureOfLoggingFrom(logger) { capturedLogs =>
          val response = Future.successful(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
          whenReady(block(response).value) { actual =>
            actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
            capturedLogs.exists(_.getMessage.contains(dummyContent)) mustBe true
          }
        }
      }
    }

    errorLevelWithThrowable.foreach { httpResponseCode =>
      s"log message: ERROR level WITH throwable when response code is $httpResponseCode" in {
        val response = Future.successful(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
        withCaptureOfLoggingFrom(logger) { capturedLogs =>
          whenReady(block(response).value) { actual =>
            actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
            capturedLogs.exists(_.getMessage.contains(dummyContent)) mustBe true
          }
        }
      }
    }

    errorLevelWithoutThrowable.foreach { httpResponseCode =>
      s"log message: ERROR level WITHOUT throwable when response code is $httpResponseCode" in {
        val response = Future.successful(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
        withCaptureOfLoggingFrom(logger) { capturedLogs =>
          whenReady(block(response).value) { actual =>
            actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
            capturedLogs.exists(_.getMessage.contains(dummyContent)) mustBe true
          }
        }
      }
    }

    "log message: ERROR level WITHOUT throwable when future fails with HttpException & recovers to BAD GATEWAY" in {
      val response = Future.failed(new HttpException(dummyContent, GATEWAY_TIMEOUT))
      withCaptureOfLoggingFrom(logger) { capturedLogs =>
        whenReady(block(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse(dummyContent, BAD_GATEWAY))
          capturedLogs.exists(_.getMessage.contains(dummyContent)) mustBe true
        }
      }
    }

    "log nothing when future fails with a non-HttpException" in {
      val response = Future.failed(new RuntimeException(dummyContent))
      recoverToSucceededIf[RuntimeException] {
        block(response).value
      }
    }
  }

}
