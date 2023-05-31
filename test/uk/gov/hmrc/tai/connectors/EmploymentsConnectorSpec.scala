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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, okJson, post, put, urlPathMatching}
import play.api.Application
import play.api.http.Status._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.webchat.client.WebChatClient
import uk.gov.hmrc.webchat.testhelpers.WebChatClientStub

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class EmploymentsConnectorSpec extends ConnectorSpec {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.tai.port"                      -> server.port(),
      "microservice.services.tai-frontend.port"             -> server.port(),
      "microservice.services.contact-frontend.port"         -> "6666",
      "microservice.services.pertax-frontend.port"          -> "1111",
      "microservice.services.personal-tax-summary.port"     -> "2222",
      "microservice.services.activity-logger.port"          -> "5555",
      "tai.cy3.enabled"                                     -> true,
      "microservice.services.feedback-survey-frontend.port" -> "3333",
      "microservice.services.company-auth.port"             -> "4444",
      "microservice.services.citizen-auth.port"             -> "9999"
    ) // TODO - Trim down on configs
    .overrides(
      play.api.inject.bind[WebChatClient].toInstance(new WebChatClientStub)
    )
    .build()

  override def messagesApi: MessagesApi = inject[MessagesApi]

  implicit lazy val ec: ExecutionContext = inject[ExecutionContext]

  def connector: EmploymentsConnector = inject[EmploymentsConnector]

  private val id = "1"
  private val year: TaxYear = TaxYear(LocalDateTime.now().getYear)

  "EmploymentsConnector" when {
    val employment = Employment(
      "company name 1",
      Live,
      Some("123"),
      LocalDate.parse("2016-05-26"),
      Some(LocalDate.parse("2016-05-26")),
      Nil,
      "",
      "",
      1,
      None,
      false,
      false
    )
    "employments is run" must {
      val url = s"/tai/$nino/employments/years/${year.year}"
      "return an OK response if successful" in {
        server.stubFor(
          get(urlPathMatching(url))
            .willReturn(okJson(Json.toJson(employment).toString))
        )
        connector.employments(nino, year).value.futureValue.map { result =>
          result.status mustBe OK
          result.json mustBe Json.toJson(employment)
        }
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        UNPROCESSABLE_ENTITY,
        TOO_MANY_REQUESTS,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse with the status $errorStatus" in {
          server.stubFor(
            get(urlPathMatching(url))
              .willReturn(aResponse().withStatus(errorStatus))
          )
          connector.employments(nino, year).value.futureValue.swap.map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "ceasedEmployments is run" must {
      val url = s"/tai/$nino/employments/year/${year.year}/status/ceased"
      "return an OK response if successful" in {
        server.stubFor(
          get(urlPathMatching(url))
            .willReturn(okJson(Json.toJson(employment).toString))
        )
        connector.ceasedEmployments(nino, year).value.futureValue.map { result =>
          result.status mustBe OK
          result.json mustBe Json.toJson(employment)
        }
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        UNPROCESSABLE_ENTITY,
        TOO_MANY_REQUESTS,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse with the status $errorStatus" in {
          server.stubFor(
            get(urlPathMatching(url))
              .willReturn(aResponse().withStatus(errorStatus))
          )
          connector.ceasedEmployments(nino, year).value.futureValue.swap.map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "employment is run" must {
      val url = s"/tai/$nino/employments/$id"
      "return an OK response if successful" in {
        server.stubFor(
          get(urlPathMatching(url))
            .willReturn(okJson(Json.toJson(employment).toString))
        )
        connector.employment(nino, id).value.futureValue.map { result =>
          result.status mustBe OK
          result.json mustBe Json.toJson(employment)
        }
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        UNPROCESSABLE_ENTITY,
        TOO_MANY_REQUESTS,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse with the status $errorStatus" in {
          server.stubFor(
            get(urlPathMatching(url))
              .willReturn(aResponse().withStatus(errorStatus))
          )
          connector.employment(nino, id).value.futureValue.swap.map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "endEmployment is run" must {
      val url = s"/tai/$nino/employments/$id/end-date"
      val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))
      "return an OK response if successful" in {
        server.stubFor(
          put(urlPathMatching(url))
            .willReturn(aResponse().withStatus(CREATED).withBody(Json.toJson("123-456-789").toString))
        )
        connector.endEmployment(nino, id.toInt, endEmploymentData).value.futureValue.map { result =>
          result.status mustBe CREATED
          result.json mustBe Json.toJson("123-456-789")
        }
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        UNPROCESSABLE_ENTITY,
        TOO_MANY_REQUESTS,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse with the status $errorStatus" in {
          server.stubFor(
            put(urlPathMatching(url))
              .willReturn(aResponse().withStatus(errorStatus))
          )
          connector
            .endEmployment(nino, id.toInt, endEmploymentData)
            .value
            .futureValue
            .swap
            .map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "addEmployment is run" must {
      val url = s"/tai/$nino/employments"
      val addEmployment = AddEmployment(
        employerName = "testEmployment",
        payrollNumber = "12345",
        startDate = LocalDate.of(2017, 6, 6),
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("123456789")
      )
      "return an OK response if successful" in {
        server.stubFor(
          post(urlPathMatching(url))
            .willReturn(aResponse().withStatus(CREATED).withBody(Json.toJson("123-456-789").toString))
        )
        connector.addEmployment(nino, addEmployment).value.futureValue.map { result =>
          result.status mustBe CREATED
          result.json mustBe Json.toJson("123-456-789")
        }
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        UNPROCESSABLE_ENTITY,
        TOO_MANY_REQUESTS,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse with the status $errorStatus" in {
          server.stubFor(
            post(urlPathMatching(url))
              .willReturn(aResponse().withStatus(errorStatus))
          )
          connector
            .addEmployment(nino, addEmployment)
            .value
            .futureValue
            .swap
            .map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "incorrectEmployment is run" must {
      val url = s"/tai/$nino/employments/$id/reason"
      val incorrectIncome =
        IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      "return an OK response if successful" in {
        server.stubFor(
          post(urlPathMatching(url))
            .willReturn(aResponse().withStatus(CREATED).withBody(Json.toJson("123-456-789").toString))
        )
        connector.incorrectEmployment(nino, id.toInt, incorrectIncome).value.futureValue.map { result =>
          result.status mustBe CREATED
          result.json mustBe Json.toJson("123-456-789")
        }
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        UNPROCESSABLE_ENTITY,
        TOO_MANY_REQUESTS,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse with the status $errorStatus" in {
          server.stubFor(
            post(urlPathMatching(url))
              .willReturn(aResponse().withStatus(errorStatus))
          )
          connector
            .incorrectEmployment(nino, id.toInt, incorrectIncome)
            .value
            .futureValue
            .swap
            .map(_.statusCode mustBe errorStatus)
        }
      }
    }
  }
}
