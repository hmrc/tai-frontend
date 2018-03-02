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

package controllers.viewModels

import org.scalatestplus.play.PlaySpec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

class DomainBuilderErrorHandlerSpec extends PlaySpec {
  private implicit val hc = HeaderCarrier()

  "Domain builder error handler" should {
    "return runtime exception" when {
      "passed function throws runtime exception" in {
        val func = Future.failed(new RuntimeException("Test"))

        val resp = DomainBuilderErrorHandler.errorWrapper(func)

        val ex = the[RuntimeException] thrownBy Await.result(resp, 5.seconds)
        ex.getMessage mustBe "Test"
      }
    }

    "return  exception" when {
      "passed function throws  exception" in {
        val func = Future.failed(new Exception("Test"))

        val resp = DomainBuilderErrorHandler.errorWrapper(func)

        val ex = the[Exception] thrownBy Await.result(resp, 5.seconds)
        ex.getMessage mustBe "Test"
      }
    }
  }
}
