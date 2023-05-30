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

import java.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => meq}
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tai.model.domain.MedicalInsurance
import uk.gov.hmrc.tai.model.domain.benefits._
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class BenefitsConnectorSpec extends BaseSpec {

  "getCompanyCarBenefits" must {
    "fetch the company car details" when {
      "provided with valid nino" in {
        when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(benefitsJson))

        val result = sut.benefits(nino, 2018)
        Await.result(result, 5 seconds) mustBe benefits
      }
    }

    "thrown exception" when {
      "benefit type is invalid" in {
        when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(invalidBenefitsJson))

        val ex = the[RuntimeException] thrownBy Await.result(sut.benefits(nino, 2018), 5 seconds)
        ex.getMessage must include(s"Couldn't retrieve benefits for nino: $nino")
      }
    }
  }

  "removeCompanyBenefit" must {

    "return an envelope id on a successful invocation" in {
      val employmentId = 1
      val endedCompanyBenefit =
        EndedCompanyBenefit("Accommodation", "Before 6th April", Some("1000000"), "Yes", Some("0123456789"))
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(
        httpHandler.postToApi(
          meq(
            s"${sut.serviceUrl}/tai/$nino/tax-account/tax-component/employments/$employmentId/benefits/ended-benefit"
          ),
          meq(endedCompanyBenefit)
        )(any(), any(), any())
      ).thenReturn(Future.successful(HttpResponse.apply(200, json.toString())))

      val result = Await.result(sut.endedCompanyBenefit(nino, employmentId, endedCompanyBenefit), 5.seconds)

      result mustBe Some("123-456-789")
    }

  }

  val companyCars = List(
    CompanyCar(
      100,
      "Make Model",
      hasActiveFuelBenefit = true,
      dateMadeAvailable = Some(LocalDate.parse("2016-10-10")),
      dateActiveFuelBenefitMadeAvailable = Some(LocalDate.parse("2016-10-11")),
      dateWithdrawn = None
    )
  )

  val companyCarBenefit = CompanyCarBenefit(10, 1000, companyCars, Some(1))
  val genericBenefit = GenericBenefit(MedicalInsurance, Some(10), 1000)
  val benefits = Benefits(Seq(companyCarBenefit), Seq(genericBenefit))

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

  val invalidOtherBenefitsJson: JsObject = Json.obj(
    "benefitType"  -> "GiftAidPayments",
    "employmentId" -> 10,
    "amount"       -> 1000
  )

  val invalidBenefitsJson: JsObject =
    Json.obj(
      "data" -> Json.obj(
        "companyCarBenefits" -> Json.arr(invalidOtherBenefitsJson),
        "otherBenefits"      -> Json.arr(otherBenefitsJson)
      ),
      "links" -> Json.arr()
    )

  val httpHandler = mock[HttpHandler]

  def sut: BenefitsConnector = new BenefitsConnector(httpHandler, servicesConfig) {
    override val serviceUrl: String = "mockUrl"
  }

}
