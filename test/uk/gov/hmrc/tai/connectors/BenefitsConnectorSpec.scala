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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api
import play.api.Application
import play.api.http.Status._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.test.Injecting
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tai.model.domain.benefits._
import uk.gov.hmrc.webchat.client.WebChatClient
import uk.gov.hmrc.webchat.testhelpers.WebChatClientStub
import utils.WireMockHelper

class BenefitsConnectorSpec extends ConnectorSpec {

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
      api.inject.bind[WebChatClient].toInstance(new WebChatClientStub)
    )
    .build()

  override def messagesApi: MessagesApi = inject[MessagesApi]

  def connector: BenefitsConnector = inject[BenefitsConnector]

  "BenefitsConnector" when {
    "benefits is run" must {
      def benefitsUrl(nino: String, taxYear: Int): String = s"/tai/$nino/tax-account/$taxYear/benefits"

      "return an OK response with the correct data" in {
        val companyCarsJson: JsObject =
          Json.obj(
            "employmentSeqNo" -> 10,
            "grossAmount"     -> 1000,
            "companyCars" -> Json.arr(
              Json.obj(
                "carSeqNo"                           -> 100,
                "makeModel"                          -> "Make Model",
                "hasActiveFuelBenefit"               -> true,
                "dateMadeAvailable"                  -> "2016-10-10",
                "dateActiveFuelBenefitMadeAvailable" -> "2016-10-11"
              )
            ),
            "version" -> 1
          )
        val otherBenefitsJson: JsObject = Json.obj(
          "benefitType"  -> "MedicalInsurance",
          "employmentId" -> 10,
          "amount"       -> 1000
        )
        val benefitsJson: JsObject =
          Json.obj(
            "data" -> Json
              .obj("companyCarBenefits" -> Json.arr(companyCarsJson), "otherBenefits" -> Json.arr(otherBenefitsJson)),
            "links" -> Json.arr()
          )
        server.stubFor(
          get(urlPathMatching(benefitsUrl(nino.nino, currentTaxYear)))
            .willReturn(okJson(benefitsJson.toString))
        )

        connector.benefits(nino, currentTaxYear).value.futureValue.map { result =>
          result.status mustBe OK
          result.json mustBe benefitsJson
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
            get(urlPathMatching(benefitsUrl(nino.nino, currentTaxYear)))
              .willReturn(aResponse().withStatus(errorStatus))
          )

          val result =
            connector.benefits(nino, currentTaxYear).value.futureValue.swap.getOrElse(UpstreamErrorResponse("", OK))
          result.statusCode mustBe errorStatus
        }
      }
    }

    "endedCompanyBenefit" must {
      def endedCompanyBenefitUrl(nino: String, employmentId: Int): String =
        s"/tai/$nino/tax-account/tax-component/employments/$employmentId/benefits/ended-benefit"

      val endedCompanyBenefit =
        EndedCompanyBenefit("Accommodation", "Before 6th April", Some("1000000"), "Yes", Some("0123456789"))
      val json = Json.obj("data" -> JsString("123-456-789"))

      "return an OK response containing the correct String" in {
        server.stubFor(
          post(urlPathMatching(endedCompanyBenefitUrl(nino.nino, employmentId)))
            .willReturn(okJson(json.toString))
        )

        val result = connector
          .endedCompanyBenefit(nino, 1, endedCompanyBenefit)
          .value
          .futureValue
          .getOrElse(HttpResponse(BAD_REQUEST, ""))
        result.status mustBe OK
        result.json mustBe json
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
            post(urlPathMatching(endedCompanyBenefitUrl(nino.nino, 1)))
              .willReturn(aResponse().withStatus(errorStatus))
          )

          val result = connector
            .endedCompanyBenefit(nino, 1, endedCompanyBenefit)
            .value
            .futureValue
            .swap
            .getOrElse(UpstreamErrorResponse("", OK))
          result.statusCode mustBe errorStatus
        }
      }
    }
  }
}
