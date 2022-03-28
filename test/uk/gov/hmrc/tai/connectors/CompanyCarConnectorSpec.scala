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

import java.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsResultException, Json}
import uk.gov.hmrc.http.{HttpException, HttpResponse}
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit, WithdrawCarAndFuel}
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class CompanyCarConnectorSpec extends BaseSpec {

  "Company car url" should {
    "fetch the correct Url" in {
      sut
        .companyCarEmploymentUrl(nino, employmentId) mustBe s"${sut.serviceUrl}/tai/$nino/tax-account/tax-components/employments/$employmentId/benefits/company-car"
    }
  }

  "getCompanyCarBenefits" should {
    "fetch the company car details" when {
      "provided with valid nino" in {
        when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(companyCarForEmploymentJson))

        val result = sut.companyCarBenefitForEmployment(nino, employmentId)
        Await.result(result, 5 seconds) mustBe Some(companyCar)
      }
    }

    "thrown exception" when {
      "tai sends an invalid json" in {
        when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(corruptJsonResponse))

        val ex = the[JsResultException] thrownBy Await
          .result(sut.companyCarBenefitForEmployment(nino, employmentId), 5 seconds)
        ex.getMessage must include("List(JsonValidationError(List(error.path.missing)")
      }
    }
  }

  "withdrawCompanyCar" must {
    "return TaiSuccessResponse" in {
      val carWithdrawDate = LocalDate.of(2017, 4, 24)
      val fuelWithdrawDate = Some(LocalDate.of(2017, 4, 24))
      val carSeqNum = 10
      val employmentSeqNum = 11
      val withdrawCarAndFuel = WithdrawCarAndFuel(10, carWithdrawDate, fuelWithdrawDate)
      val url = s"${sut.companyCarEmploymentUrl(nino, employmentSeqNum)}/$carSeqNum/withdrawn"

      when(httpHandler.putToApi(Matchers.eq(url), Matchers.eq(withdrawCarAndFuel))(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson("123456")))))

      val result =
        Await.result(sut.withdrawCompanyCarAndFuel(nino, employmentSeqNum, carSeqNum, withdrawCarAndFuel), 5 seconds)

      result mustBe TaiSuccessResponse
    }
  }

  "companyCarsForCurrentYearEmployments" must {
    "return CompanyCarBenefit" when {
      "provided with valid nino" in {
        when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(companyCars))

        val result = sut.companyCarsForCurrentYearEmployments(nino)
        Await.result(result, 5 seconds) mustBe Seq(companyCar)
      }
    }

    "return empty sequence of company car benefit" when {
      "company car service returns no car" in {
        when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(emptyCompanyCars))

        val result = sut.companyCarsForCurrentYearEmployments(nino)
        Await.result(result, 5 seconds) mustBe Seq.empty[CompanyCarBenefit]
      }

      "company car service returns a failure response" in {
        when(httpHandler.getFromApiV2(any())(any()))
          .thenReturn(Future.failed(new HttpException("company car strange response", UNPROCESSABLE_ENTITY)))

        val result = sut.companyCarsForCurrentYearEmployments(nino)
        Await.result(result, 5 seconds) mustBe Seq.empty[CompanyCarBenefit]
      }
    }

  }

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
      )),
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
          )),
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
          ))
      )
    )

  val companyCars: JsObject =
    Json.obj(
      "data" -> Json.obj(
        "companyCarBenefits" -> Json.arr(Json.obj(
          "employmentSeqNo" -> 10,
          "grossAmount"     -> 1000,
          "companyCars" -> Json.arr(Json.obj(
            "carSeqNo"                           -> 10,
            "makeModel"                          -> "Make Model",
            "hasActiveFuelBenefit"               -> true,
            "dateMadeAvailable"                  -> "2016-10-10",
            "dateActiveFuelBenefitMadeAvailable" -> "2016-10-11"
          )),
          "version" -> 1
        ))),
      "links" -> Json.arr()
    )

  val emptyCompanyCars: JsObject =
    Json.obj("data" -> Json.obj("companyCarBenefits" -> Json.arr()), "links" -> Json.arr())

  val employmentId: Int = 2

  val httpHandler: HttpHandler = mock[HttpHandler]

  def sut: CompanyCarConnector = new CompanyCarConnector(httpHandler, servicesConfig) {
    override val serviceUrl: String = "mockUrl"
  }

}
