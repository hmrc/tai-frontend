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

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random

class TaxAccountConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "tax account url" must {
    "fetch the url to connect to TAI to retrieve tax codes" in {
      val sut = createSUT
      val nino = generateNino.nino

      sut.taxAccountUrl(nino, currentTaxYear) mustBe s"${sut.serviceUrl}/tai/$nino/tax-account/${currentTaxYear.year}/income/tax-code-incomes"
    }
  }

  "taxCode" should {
    "fetch the tax codes" when {
      "provided with valid nino" in {
        val sut = createSUT
        when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(taxCodeIncomeJson))
        val result = sut.taxCodeIncomes(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(Seq(taxCodeIncome))
      }
    }

    "thrown exception" when {
      "tai sends an invalid json" in {
        val sut = createSUT
        when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(corruptJsonResponse))

        val ex = Await.result(sut.taxCodeIncomes(generateNino, currentTaxYear), 5 seconds)
        ex mustBe a[TaiTaxAccountFailureResponse]
      }
    }
  }

  "codingComponents" should {
    "fetch the coding components" when {
      "provided with valid nino" in {
        val sut = createSUT
        when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(codingComponentSampleJson))
        val result = sut.codingComponents(generateNino, currentTaxYear)

        Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(codingComponentSeq)
      }
    }
  }

  "taxAccountSummary" should {
    "fetch the tax account summary" when {
      "provided with valid nino" in {
        val sut = createSUT
        val taxAccountSummaryJson = Json.obj(
          "data" -> Json.obj(
            "totalEstimatedTax" -> 111,
            "taxFreeAmount" -> 222,
            "totalInYearAdjustment" -> 3131.12
          ),
          "links" -> Json.arr())

        when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(taxAccountSummaryJson))
        val result = sut.taxAccountSummary(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(TaxAccountSummary(111,222, 3131.12))
      }
    }

    "thrown exception" when {
      "tai sends an invalid json" in {
        val sut = createSUT
        val corruptTaxAccountSummaryJson = Json.obj(
          "data" -> Json.obj(
            "totalEstimatedTax222" -> 111,
            "taxFreeAmount11" -> 222
          ),
          "links" -> Json.arr())
        when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(corruptTaxAccountSummaryJson))

        val ex = Await.result(sut.taxAccountSummary(generateNino, currentTaxYear), 5 seconds)
        ex mustBe a[TaiTaxAccountFailureResponse]
      }
    }
  }

  "nonTaxCodeComponents" should {
    "fetch the non tax code incomes" when {
      "provided with valid nino" in {
        val sut = createSUT
        val nino = generateNino
        when(sut.httpHandler.getFromApi(Matchers.eq(s"mockUrl/tai/$nino/tax-account/${currentTaxYear.year}/income"))(any())).
          thenReturn(Future.successful(incomeJson))

        val result = sut.nonTaxCodeIncomes(nino, currentTaxYear)

        Await.result(result, 5.seconds) mustBe TaiSuccessResponseWithPayload(income.nonTaxCodeIncomes)
      }
    }

    "thrown exception" when {
      "tai sends an invalid json" in {
        val sut = createSUT
        val nino = generateNino
        when(sut.httpHandler.getFromApi(Matchers.eq(s"mockUrl/tai/$nino/tax-account/${currentTaxYear.year}/income"))(any())).
          thenReturn(Future.successful(corruptJsonResponse))

        val ex = Await.result(sut.nonTaxCodeIncomes(nino, currentTaxYear), 5 seconds)
        ex mustBe a[TaiTaxAccountFailureResponse]
      }
    }
  }

  "updateEstimatedIncome" must {
    "return Success" when {
      "update tax code income returns 200" in {
        val sut = createSUT
        val nino = generateNino
        when(sut.httpHandler.putToApi(Matchers.eq(sut.updateTaxCodeIncome(nino.nino, TaxYear(), 1)),
          Matchers.eq(UpdateTaxCodeIncomeRequest(100)))(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200)))

        val response = Await.result(sut.updateEstimatedIncome(nino, TaxYear(), 100, 1), 5 seconds)

        response mustBe TaiSuccessResponse
      }
    }

    "return failure" when {
      "update tax code income returns 500" in {
        val sut = createSUT
        val nino = generateNino
        when(sut.httpHandler.putToApi(Matchers.eq(sut.updateTaxCodeIncome(nino.nino, TaxYear(), 1)),
          Matchers.eq(UpdateTaxCodeIncomeRequest(100)))(any(), any(), any())).thenReturn(Future.failed(new InternalServerException("Failed")))

        val response = Await.result(sut.updateEstimatedIncome(nino, TaxYear(), 100, 1), 5 seconds)

        response mustBe TaiTaxAccountFailureResponse("Failed")
      }
    }
  }

  private val currentTaxYear = TaxYear()

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  def generateNino: Nino = new Generator(new Random).nextNino

  val taxCodeIncomeJson = Json.obj(
    "data" -> JsArray(Seq(Json.obj(
      "componentType" -> "EmploymentIncome",
      "employmentId" -> 1,
      "amount" -> 1111,
      "description" -> "employment",
      "taxCode" -> "1150L",
      "name" -> "Employer1",
      "basisOperation" -> "OtherBasisOperation",
      "status" -> "Live"
    ))),
  "links" -> JsArray(Seq()))

  val codingComponentSampleJson = Json.obj(
    "data" -> Json.arr(
      Json.obj(
        "componentType" -> "EmployerProvidedServices",
        "employmentId" -> 12,
        "amount" -> 12321,
        "description" -> "Some Description",
        "iabdCategory" -> "Benefit"),
      Json.obj(
        "componentType" -> "GiftsSharesCharity",
        "employmentId" -> 31,
        "amount" -> 12345,
        "description" -> "Some Description Some",
        "iabdCategory" -> "Allowance"
      )),
    "links" -> Json.arr())

  val corruptJsonResponse = Json.obj(
    "data" -> JsArray(Seq(Json.obj(
      "employmentId" -> 1,
      "amount" -> 1111,
      "description" -> "employment",
      "taxCode" -> "1150L",
      "name" -> "Employer1",
      "basisOperation" -> "OtherBasisOperation"
    ))),
    "links" -> JsArray(Seq()))

  val incomeJson = Json.obj(
    "data" -> Json.obj(
      "taxCodeIncomes" -> JsArray(),
      "nonTaxCodeIncomes" -> Json.obj(
        "otherNonTaxCodeIncomes" -> Json.arr(
          Json.obj(
            "incomeComponentType" -> "Profit" ,
            "amount" -> 100,
            "description" -> "Profit"
          )
        )
      )
    )
    ,
    "links" -> Json.arr()
  )

  private val income = uk.gov.hmrc.tai.model.domain.income.Incomes(Seq.empty[TaxCodeIncome], NonTaxCodeIncome(None, Seq(
    OtherNonTaxCodeIncome(Profit, None, 100, "Profit")
  )))

  val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "Employer1", OtherBasisOperation, Live)
  val codingComponentSeq = Seq(CodingComponent(EmployerProvidedServices, Some(12), 12321, "Some Description"),
    CodingComponent(GiftsSharesCharity, Some(31), 12345, "Some Description Some"))

  private def createSUT = new SUT

  private class SUT extends TaxAccountConnector {
    override val serviceUrl: String = "mockUrl"
    override val httpHandler: HttpHandler = mock[HttpHandler]
  }

}
