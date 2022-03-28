/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{AddPensionProvider, IncorrectPensionProvider}
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PensionProviderConnectorSpec extends BaseSpec {

  "PensionProviderConnector addPensionProvider" must {
    "return an envelope id on a successful invocation" in {
      val addPensionProvider =
        AddPensionProvider("testPension", LocalDate.of(2017, 6, 6), "12345", "Yes", Some("123456789"))
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(
        httpHandler.postToApi(Matchers.eq(sut.addPensionProviderServiceUrl(nino)), Matchers.eq(addPensionProvider))(
          any(),
          any(),
          any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

      val result = Await.result(sut.addPensionProvider(nino, addPensionProvider), 5.seconds)

      result mustBe Some("123-456-789")
    }
  }

  "PensionProviderConnector incorrectPensionProvider" must {
    "return an envelope id on a successful invocation" in {
      val incorrectPensionProvider = IncorrectPensionProvider(
        whatYouToldUs = "TEST",
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("123456789"))
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(
        httpHandler.postToApi(
          Matchers.eq(sut.incorrectPensionProviderServiceUrl(nino, 1)),
          Matchers.eq(incorrectPensionProvider))(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, Some(json))))

      val result = Await.result(sut.incorrectPensionProvider(nino, 1, incorrectPensionProvider), 5.seconds)

      result mustBe Some("123-456-789")
    }
  }

  private val year: TaxYear = TaxYear(DateTime.now().getYear)

  val httpHandler: HttpHandler = mock[HttpHandler]

  def sut: PensionProviderConnector = new PensionProviderConnector(httpHandler, servicesConfig) {
    override val serviceUrl: String = "testUrl"
  }

}
