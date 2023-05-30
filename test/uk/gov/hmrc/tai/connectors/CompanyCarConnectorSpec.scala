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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, okJson, urlPathMatching}
import play.api.Application
import play.api.http.Status._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit}
import uk.gov.hmrc.webchat.client.WebChatClient
import uk.gov.hmrc.webchat.testhelpers.WebChatClientStub

import java.time.LocalDate
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class CompanyCarConnectorSpec extends ConnectorSpec {

  implicit val ec: ExecutionContext = inject[ExecutionContext]

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

  def connector: CompanyCarConnector = inject[CompanyCarConnector]

  "CompanyCarConnector" when {
    "companyCarsForCurrentYearEmployments is run" must {
      val url = s"/tai/$nino/tax-account/tax-components/benefits/company-cars"
      "return an OK response status and company car data" in {
        server.stubFor(
          get(urlPathMatching(url))
            .willReturn(okJson(Json.toJson(companyCar).toString))
        )

        connector.companyCarsForCurrentYearEmployments(nino).value.futureValue.map { result =>
          result.status mustBe OK
          result.json mustBe Json.toJson(companyCar)
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

          connector.companyCarsForCurrentYearEmployments(nino).value.futureValue.swap.map { result =>
            result.statusCode mustBe errorStatus
          }
        }
      }
    }
  }
//    "return empty sequence of company car benefit" when { // Replace in service spec
//      "company car service returns no car" in {
//        when(httpHandler.getFromApiV2(any())(any()))
//          .thenReturn(EitherT[Future, UpstreamErrorResponse, JsValue](Future.successful(Right(emptyCompanyCars))))
//
//        val result = connector.companyCarsForCurrentYearEmployments(nino)
//        Await.result(result, 5 seconds) mustBe Seq.empty[CompanyCarBenefit]
//      }

  val companyCar: CompanyCarBenefit = CompanyCarBenefit(
    10,
    1000,
    List(
      CompanyCar(
        10,
        "Make Model",
        hasActiveFuelBenefit = true,
        dateMadeAvailable = Some(LocalDate.parse("2016-10-10")),
        dateActiveFuelBenefitMadeAvailable = Some(LocalDate.parse("2016-10-11")),
        dateWithdrawn = None
      )
    ),
    Some(1)
  )

  val companyCarForEmploymentJson: JsObject =
    Json.obj(
      "data" -> Json.obj(
        "employmentSeqNo" -> 10,
        "grossAmount"     -> 1000,
        "companyCars" -> Json.arr(
          Json.obj(
            "carSeqNo"                           -> 10,
            "makeModel"                          -> "Make Model",
            "hasActiveFuelBenefit"               -> true,
            "dateMadeAvailable"                  -> "2016-10-10",
            "dateActiveFuelBenefitMadeAvailable" -> "2016-10-11"
          )
        ),
        "version" -> 1
      ),
      "links" -> Json.arr()
    )

  val corruptJsonResponse: JsObject =
    Json.obj(
      "data" -> Json.obj(
        "companyCars" -> Json.arr(
          Json.obj(
            "carSeqNo"  -> 10,
            "makeModel" -> "Make Model"
          )
        )
      )
    )

  val companyCars: JsObject =
    Json.obj(
      "data" -> Json.obj(
        "companyCarBenefits" -> Json.arr(
          Json.obj(
            "employmentSeqNo" -> 10,
            "grossAmount"     -> 1000,
            "companyCars" -> Json.arr(
              Json.obj(
                "carSeqNo"                           -> 10,
                "makeModel"                          -> "Make Model",
                "hasActiveFuelBenefit"               -> true,
                "dateMadeAvailable"                  -> "2016-10-10",
                "dateActiveFuelBenefitMadeAvailable" -> "2016-10-11"
              )
            ),
            "version" -> 1
          )
        )
      ),
      "links" -> Json.arr()
    )

  val emptyCompanyCars: JsObject =
    Json.obj("data" -> Json.obj("companyCarBenefits" -> Json.arr()), "links" -> Json.arr())

}
