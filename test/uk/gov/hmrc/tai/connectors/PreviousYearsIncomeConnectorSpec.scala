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

import controllers.FakeTaiPlayApplication
import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.IncorrectIncome

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class PreviousYearsIncomeConnectorSpec extends PlaySpec
  with MockitoSugar
  with ServicesConfig
  with FakeTaiPlayApplication {

  "PreviousYearsIncomeConnector" must {

    "return an envelope id on a successful invocation" in {
      val sut = createSUT()
      val model = IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(httpHandler.postToApi(Matchers.eq(s"/tai/$nino/employments/years/2016/update"), Matchers.eq(model))
      (any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

      val result = Await.result(sut.incorrectIncome(nino, 2016, model), 5.seconds)

      result mustBe Some("123-456-789")
    }


  }

  private val year: TaxYear = TaxYear(DateTime.now().getYear)
  private val nino: Nino = new Generator(new Random).nextNino
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def createSUT(servUrl: String = "") = new PreviousYearsIncomeConnectorTest(servUrl)

  val httpHandler: HttpHandler = mock[HttpHandler]

  private class PreviousYearsIncomeConnectorTest(servUrl: String = "") extends PreviousYearsIncomeConnector(httpHandler) {
    override val serviceUrl: String = servUrl
  }
}


