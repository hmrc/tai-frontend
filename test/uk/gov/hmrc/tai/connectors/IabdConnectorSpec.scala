/*
 * Copyright 2025 HM Revenue & Customs
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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, verify, when}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import utils.BaseSpec

import java.net.URL
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class IabdConnectorSpec extends BaseSpec {

  private val httpClientV2: HttpClientV2             = mock[HttpClientV2]
  private val servicesConfig: ServicesConfig         = mock[ServicesConfig]
  private val httpClientResponse: HttpClientResponse = mock[HttpClientResponse]
  private val requestBuilder: RequestBuilder         = mock[RequestBuilder]

  private val baseUrl = "http://test-tai"
  private val nino    = Nino("AA123456A")
  private val year    = TaxYear(2024)

  private def ok: HttpResponse = HttpResponse(200, "")

  def sut: IabdConnector =
    new IabdConnector(httpClientV2, servicesConfig, httpClientResponse)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClientV2)
    reset(servicesConfig)
    reset(httpClientResponse)
    reset(requestBuilder)
    when(servicesConfig.baseUrl(meq("tai"))).thenReturn(baseUrl)
    when(httpClientV2.get(any[URL])(any())).thenReturn(requestBuilder)

    when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
      .thenReturn(Future.successful(Right(ok)))

    when(httpClientResponse.read(any[Future[Either[UpstreamErrorResponse, HttpResponse]]])).thenAnswer { inv =>
      val fut = inv.getArgument(0).asInstanceOf[Future[Either[UpstreamErrorResponse, HttpResponse]]]
      EitherT(fut)
    }
  }

  "getIabds" must {

    "call the IABD endpoint without query when iabdType is None" in {
      val res = Await.result(sut.getIabds(nino, year, None).value, 5.seconds)
      res match {
        case Right(r) => r.status mustBe 200
        case Left(e)  => fail(s"Unexpected Left: $e")
      }

      val captor = ArgumentCaptor.forClass(classOf[URL])
      verify(httpClientV2).get(captor.capture())(any())

      val calledUrl = captor.getValue.toString
      calledUrl.startsWith(baseUrl) mustBe true
      calledUrl must endWith(s"/tai/${nino.nino}/iabds/years/${year.year}")
      calledUrl.contains("?type=") mustBe false
    }

    "add type=New Estimated Pay (027) when iabdType is provided (URL-encoded)" in {
      val res = Await.result(sut.getIabds(nino, year, Some("New Estimated Pay (027)")).value, 5.seconds)
      res match {
        case Right(r) => r.status mustBe 200
        case Left(e)  => fail(s"Unexpected Left: $e")
      }

      val captor = ArgumentCaptor.forClass(classOf[URL])
      verify(httpClientV2).get(captor.capture())(any())

      val calledUrl = captor.getValue.toString
      calledUrl.startsWith(baseUrl) mustBe true
      calledUrl must endWith(
        s"http://test-tai/tai/AA123456A/iabds/years/2024?type=New+Estimated+Pay+(027)"
      )
    }
  }
}
