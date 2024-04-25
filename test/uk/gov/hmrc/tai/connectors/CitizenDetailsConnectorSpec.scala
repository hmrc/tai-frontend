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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.{CONTENT_TYPE, JSON}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tai.model.domain.Person
import utils.{BaseSpec, WireMockHelper}

class CitizenDetailsConnectorSpec extends BaseSpec with WireMockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.citizen-details.port" -> server.port())
    .build()

  val person: Person = fakePerson(nino)
  val designatoryDetailsUrl: String = s"/citizen-details/$nino/designatory-details"

  lazy val citizenDetailsConnector: CitizenDetailsConnector = {
    val httpClient: HttpClientV2 = inject[HttpClientV2]
    val httpHandler: HttpHandler = new HttpHandler(httpClient)
    new CitizenDetailsConnector(httpHandler, httpClient, appConfig)
  }

  def stubGet(url: String, responseStatus: Int, responseBody: Option[String]): StubMapping = server.stubFor {
    val baseResponse = aResponse().withStatus(responseStatus).withHeader(CONTENT_TYPE, JSON)
    val response = responseBody.fold(baseResponse)(body => baseResponse.withBody(body))
    get(url).willReturn(response)
  }

  "Calling retrieveCitizenDetails" must {

    "return OK when called with an existing nino" in {
      stubGet(designatoryDetailsUrl, OK, Some(Json.toJson(person).toString()))

      val result: Either[UpstreamErrorResponse, HttpResponse] =
        citizenDetailsConnector.retrieveCitizenDetails(nino).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(HttpResponse(BAD_REQUEST, "")).status mustBe OK
    }

    "return NOT_FOUND when called with an unknown nino" in {
      stubGet(designatoryDetailsUrl, NOT_FOUND, None)

      val result = citizenDetailsConnector
        .retrieveCitizenDetails(nino)
        .value
        .futureValue

      result mustBe a[Left[_, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe NOT_FOUND
    }

    "return LOCKED when a locked hidden record (MCI) is asked for" in {
      stubGet(designatoryDetailsUrl, LOCKED, None)

      val result = citizenDetailsConnector
        .retrieveCitizenDetails(nino)
        .value
        .futureValue

      result mustBe a[Left[_, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe LOCKED
    }

    "return given status code when an unexpected status is returned" in {
      stubGet(designatoryDetailsUrl, IM_A_TEAPOT, None)

      val result = citizenDetailsConnector
        .retrieveCitizenDetails(nino)
        .value
        .futureValue

      result mustBe a[Left[_, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe IM_A_TEAPOT
    }
  }
}
