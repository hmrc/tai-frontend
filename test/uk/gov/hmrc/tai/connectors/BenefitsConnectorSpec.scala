/*
 * Copyright 2020 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.model.domain.MedicalInsurance
import uk.gov.hmrc.tai.model.domain.benefits._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class BenefitsConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "getCompanyCarBenefits" must {
    "fetch the company car details" when {
      "provided with valid nino" in {
        val sut = createSUT
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(benefitsJson))

        val result = sut.benefits(generateNino, 2018)
        Await.result(result, 5 seconds) mustBe benefits
      }
    }

    "thrown exception" when {
      "benefit type is invalid" in {
        val sut = createSUT
        val nino = generateNino
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(invalidBenefitsJson))

        val ex = the[RuntimeException] thrownBy Await.result(sut.benefits(nino, 2018), 5 seconds)
        ex.getMessage must include(s"Couldn't retrieve benefits for nino: $nino")
      }
    }
  }

  "removeCompanyBenefit" must {

    "return an envelope id on a successful invocation" in {
      val sut = createSUT
      val employmentId = 1
      val nino = generateNino
      val endedCompanyBenefit =
        EndedCompanyBenefit("Accommodation", "Before 6th April", Some("1000000"), "Yes", Some("0123456789"))
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(
        httpHandler.postToApi(
          Matchers.eq(
            s"${sut.serviceUrl}/tai/$nino/tax-account/tax-component/employments/$employmentId/benefits/ended-benefit"),
          Matchers.eq(endedCompanyBenefit)
        )(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

      val result = Await.result(sut.endedCompanyBenefit(nino, employmentId, endedCompanyBenefit), 5.seconds)

      result mustBe Some("123-456-789")
    }

  }

  val companyCars = List(
    CompanyCar(
      100,
      "Make Model",
      hasActiveFuelBenefit = true,
      dateMadeAvailable = Some(new LocalDate("2016-10-10")),
      dateActiveFuelBenefitMadeAvailable = Some(new LocalDate("2016-10-11")),
      dateWithdrawn = None
    ))

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
        )),
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
        "otherBenefits"      -> Json.arr(otherBenefitsJson)),
      "links" -> Json.arr()
    )

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  def generateNino: Nino = new Generator(new Random).nextNino

  private def createSUT = new SUT

  val httpHandler = mock[HttpHandler]

  private class SUT extends BenefitsConnector(httpHandler) {
    override val serviceUrl: String = "mockUrl"
  }

}
