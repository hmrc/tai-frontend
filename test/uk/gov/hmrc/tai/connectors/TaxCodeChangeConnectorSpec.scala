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

import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, urlEqualTo}
import controllers.FakeTaiPlayApplication
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class TaxCodeChangeConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with WireMockHelper{


  "tax code history url" must {
    "fetch the url to connect to TAI to retrieve tax code history" in {
      val sut = createSUT
      val nino = generateNino.nino

      sut.taxCodeHistoryUrl(nino) mustBe s"${sut.serviceUrl}/tai/$nino/tax-account/tax-code-history"

    }
  }

  "tax Code history" should {
    "fetch the tax code history" when {
      "provided with valid nino" in {

        val sut = createSUT
        val nino = generateNino

        val taxCodeHistoryUrl = s"${sut.serviceUrl}/tai/${nino.nino}/tax-account/tax-code-history"

        server.stubFor(
          get(urlEqualTo(taxCodeHistoryUrl)).willReturn(ok(temp))
        )

        sut.taxCodeHistory(nino) map { result =>
            result mustEqual ""
        }

      }
    }
  }

  val temp =
    """
       {
          "maxResults": 50,
          "startAt": 0,
          "isLast": true,
          "values": []
        }
    """

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private def createSUT = new SUT
  private def generateNino: Nino = new Generator(new Random).nextNino

  private class SUT extends TaxCodeChangeConnector {
    override val serviceUrl: String = "mockUrl"
    override val httpHandler: HttpHandler = mock[HttpHandler]
  }
}
