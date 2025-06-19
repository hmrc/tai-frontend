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

package uk.gov.hmrc.tai.connectors

import cats.data.EitherT
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, anyUrl, get}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.invocation.InvocationOnMock
import play.api.Application
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, Available, Monthly, Payment}
import utils.{BaseSpec, WireMockHelper}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RtiConnectorSpec extends BaseSpec with WireMockHelper {

  private val mockHttpClientResponse: HttpClientResponse = mock[HttpClientResponse]

  private val year: TaxYear    = TaxYear(LocalDateTime.now().getYear)
  val httpHandler: HttpHandler = mock[HttpHandler]

  private lazy val url = s"/tai/$nino/rti-payments/years/${year.year}"

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure("microservice.services.tai.port" -> server.port())
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClientResponse)
    when(mockHttpClientResponse.read(any())).thenAnswer((invocation: InvocationOnMock) =>
      EitherT(invocation.getArgument(0))
    )
  }

  private def sut: RtiConnector = new RtiConnector(inject[HttpClientV2], mockHttpClientResponse, appConfig)

  private val firstPayment: Payment        = Payment(LocalDate.now.minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  private val secondPayment: Payment       = Payment(LocalDate.now.minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  private val annualAccount: AnnualAccount = AnnualAccount(
    7,
    taxYear = uk.gov.hmrc.tai.model.TaxYear(),
    realTimeStatus = Available,
    payments = Seq(firstPayment, secondPayment),
    endOfTaxYearUpdates = Nil
  )

  val seqAnnualAccountAsJson: String = Json
    .obj(
      "data" -> Json.toJson(Seq(annualAccount))
    )
    .toString()

  "getPaymentsForYear" must {
    "return RTI data when present" in {
      server.stubFor(
        get(url)
          .willReturn(
            aResponse.withBody(seqAnnualAccountAsJson)
          )
      )

      val result = Await.result(sut.getPaymentsForYear(nino, year).value, Duration.Inf)
      result mustBe Right(Seq(annualAccount))
    }

    "return left when internal server error" in {
      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("internal server error"))
      )

      val result = Await.result(sut.getPaymentsForYear(nino, year).value, Duration.Inf)
      result.isLeft mustBe true
    }
  }
}
