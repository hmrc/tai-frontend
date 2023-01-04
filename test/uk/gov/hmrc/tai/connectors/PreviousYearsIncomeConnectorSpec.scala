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

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.IncorrectIncome
import utils.BaseSpec

import java.time.LocalDateTime
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PreviousYearsIncomeConnectorSpec extends BaseSpec {

  "PreviousYearsIncomeConnector" must {

    "return an envelope id on a successful invocation" in {
      val model =
        IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(
        httpHandler
          .postToApi(Matchers.eq(s"/tai/$nino/employments/years/2016/update"), Matchers.eq(model))(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, Some(json))))

      val result = Await.result(sut().incorrectIncome(nino, 2016, model), 5.seconds)

      result mustBe Some("123-456-789")
    }

  }

  private val year: TaxYear = TaxYear(LocalDateTime.now().getYear)

  val httpHandler: HttpHandler = mock[HttpHandler]

  def sut(servUrl: String = ""): PreviousYearsIncomeConnector =
    new PreviousYearsIncomeConnector(httpHandler, servicesConfig) {
      override val serviceUrl: String = servUrl
    }
}
