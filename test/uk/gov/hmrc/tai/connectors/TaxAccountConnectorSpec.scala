/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax._

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
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(taxCodeIncomeJson))
        val result = sut.taxCodeIncomes(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(Seq(taxCodeIncome))
      }
    }

    "thrown exception" when {
      "tai sends an invalid json" in {
        val sut = createSUT
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(corruptJsonResponse))

        val ex = Await.result(sut.taxCodeIncomes(generateNino, currentTaxYear), 5 seconds)
        ex mustBe a[TaiTaxAccountFailureResponse]
      }
    }
  }

  "incomeSources" should {
    "fetch the tax codes" when {
      "provided with valid nino" in {
        val sut = createSUT
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(incomeSourceJson))

        val result = Await.result(sut.incomeSources(generateNino, currentTaxYear, EmploymentIncome, Live), 5 seconds)
        result mustBe TaiSuccessResponseWithPayload(Seq(incomeSource))
      }
    }

    "fetch empty seq" when {
      "provided with nino that is not found" in {
        val sut = createSUT
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(incomeSourceEmpty))

        val result = Await.result(sut.incomeSources(generateNino, currentTaxYear, EmploymentIncome, Live), 5 seconds)
        result mustBe TaiSuccessResponseWithPayload(Seq.empty[TaxedIncome])
      }
    }


    "thrown exception" when {
      "tai sends an invalid json" in {
        val sut = createSUT
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(corruptJsonResponse))

        val ex = Await.result(sut.incomeSources(generateNino, currentTaxYear, EmploymentIncome, Live), 5 seconds)
        ex mustBe a[TaiTaxAccountFailureResponse]
      }
    }
  }

  "codingComponents" should {
    "fetch the coding components" when {
      "provided with valid nino" in {
        val sut = createSUT
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(codingComponentSampleJson))
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
            "totalInYearAdjustmentIntoCY" -> 1111.11,
            "totalInYearAdjustment" -> 2222.23,
            "totalInYearAdjustmentIntoCYPlusOne" -> 1111.12,
            "totalEstimatedIncome" -> 100,
            "taxFreeAllowance" -> 200
          ),
          "links" -> Json.arr())

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(taxAccountSummaryJson))
        val result = sut.taxAccountSummary(generateNino, currentTaxYear)
        Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(TaxAccountSummary(111, 222, 1111.11, 2222.23, 1111.12, 100, 200))
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
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(corruptTaxAccountSummaryJson))

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
        when(httpHandler.getFromApi(Matchers.eq(s"mockUrl/tai/$nino/tax-account/${currentTaxYear.year}/income"))(any())).
          thenReturn(Future.successful(incomeJson))

        val result = sut.nonTaxCodeIncomes(nino, currentTaxYear)

        Await.result(result, 5.seconds) mustBe TaiSuccessResponseWithPayload(income.nonTaxCodeIncomes)
      }
    }

    "thrown exception" when {
      "tai sends an invalid json" in {
        val sut = createSUT
        val nino = generateNino
        when(httpHandler.getFromApi(Matchers.eq(s"mockUrl/tai/$nino/tax-account/${currentTaxYear.year}/income"))(any())).
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
        when(httpHandler.putToApi(Matchers.eq(sut.updateTaxCodeIncome(nino.nino, TaxYear(), 1)),
          Matchers.eq(UpdateTaxCodeIncomeRequest(100)))(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200)))

        val response = Await.result(sut.updateEstimatedIncome(nino, TaxYear(), 100, 1), 5 seconds)

        response mustBe TaiSuccessResponse
      }
    }

    "return failure" when {
      "update tax code income returns 500" in {
        val sut = createSUT
        val nino = generateNino
        when(httpHandler.putToApi(Matchers.eq(sut.updateTaxCodeIncome(nino.nino, TaxYear(), 1)),
          Matchers.eq(UpdateTaxCodeIncomeRequest(100)))(any(), any(), any())).thenReturn(Future.failed(new InternalServerException("Failed")))

        val response = Await.result(sut.updateEstimatedIncome(nino, TaxYear(), 100, 1), 5 seconds)

        response mustBe TaiTaxAccountFailureResponse("Failed")
      }
    }
  }

  "total tax" must {
    "return the total tax details for the given year" when {
      "provided with nino" in {
        val sut = createSUT
        val nino = generateNino

        val expectedTotalTax = TotalTax(1000,
          List(IncomeCategory(UkDividendsIncomeCategory, 10, 20, 30, List(TaxBand("", "", 0, 0, None, None, 0), TaxBand("B", "BR", 10000, 500, Some(5000), Some(20000), 10)))),
          Some(tax.TaxAdjustment(100, List(TaxAdjustmentComponent(EnterpriseInvestmentSchemeRelief, 100)))),
          Some(tax.TaxAdjustment(100, List(TaxAdjustmentComponent(ExcessGiftAidTax, 100)))),
          Some(tax.TaxAdjustment(100, List(TaxAdjustmentComponent(TaxOnBankBSInterest, 100)))))

        when(httpHandler.getFromApi(Matchers.eq(s"mockUrl/tai/$nino/tax-account/${currentTaxYear.year}/total-tax"))(any())).
          thenReturn(Future.successful(totalTaxJson))

        val result = sut.totalTax(nino, currentTaxYear)
        Await.result(result, 5.seconds) mustBe TaiSuccessResponseWithPayload(expectedTotalTax)
      }
    }
  }

  private val currentTaxYear = TaxYear()

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  def generateNino: Nino = new Generator(new Random).nextNino

  val taxCodeIncomeJson: JsValue = Json.obj(
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

  val incomeSourceJson: JsValue = Json.obj(
    "data" -> Json.arr(
      Json.obj(
        "taxCodeIncome" -> Json.obj(
          "componentType" -> "EmploymentIncome",
          "employmentId" -> 1,
          "amount" -> 1111,
          "description" -> "employment",
          "taxCode" -> "1150L",
          "name" -> "Employer1",
          "basisOperation" -> "OtherBasisOperation",
          "status" -> "Live",
          "inYearAdjustmentIntoCY" -> 0,
          "totalInYearAdjustment" -> 0,
          "inYearAdjustmentIntoCYPlusOne" -> 0
        ),
        "employment" -> Json.obj(
          "name" -> "company name",
          "payrollNumber" -> "888",
          "startDate" -> "2019-05-26",
          "annualAccounts" -> Json.arr(),
          "taxDistrictNumber" -> "",
          "payeNumber" -> "",
          "sequenceNumber" -> 1,
          "cessationPay" -> 100,
          "hasPayrolledBenefit" -> false,
          "receivingOccupationalPension" -> true
        )
      )
    )
  )

  val incomeSourceEmpty: JsValue = Json.obj(
    "data" -> Json.arr()
  )

  val codingComponentSampleJson: JsValue = Json.obj(
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

  val corruptJsonResponse: JsValue = Json.obj(
    "data" -> JsArray(Seq(Json.obj(
      "employmentId" -> 1,
      "amount" -> 1111,
      "description" -> "employment",
      "taxCode" -> "1150L",
      "name" -> "Employer1",
      "basisOperation" -> "OtherBasisOperation"
    ))),
    "links" -> JsArray(Seq()))

  val incomeJson: JsValue = Json.obj(
    "data" -> Json.obj(
      "taxCodeIncomes" -> JsArray(),
      "nonTaxCodeIncomes" -> Json.obj(
        "otherNonTaxCodeIncomes" -> Json.arr(
          Json.obj(
            "incomeComponentType" -> "Profit",
            "amount" -> 100,
            "description" -> "Profit"
          )
        )
      )
    )
    ,
    "links" -> Json.arr()
  )

  private val totalTaxJson = Json.obj(
    "data" -> Json.obj(
      "amount" -> 1000,
      "incomeCategories" -> Json.arr(
        Json.obj(
          "incomeCategoryType" -> "UkDividendsIncomeCategory",
          "totalTax" -> 10,
          "totalTaxableIncome" -> 20,
          "totalIncome" -> 30,
          "taxBands" -> Json.arr(
            Json.obj(
              "bandType" -> "",
              "code" -> "",
              "income" -> 0,
              "tax" -> 0,
              "rate" -> 0
            ),
            Json.obj(
              "bandType" -> "B",
              "code" -> "BR",
              "income" -> 10000,
              "tax" -> 500,
              "lowerBand" -> 5000,
              "upperBand" -> 20000,
              "rate" -> 10
            )
          )
        )
      ),
      "reliefsGivingBackTax" -> Json.obj(
        "amount" -> 100,
        "taxAdjustmentComponents" -> Json.arr(
          Json.obj(
            "taxAdjustmentType" -> "EnterpriseInvestmentSchemeRelief",
            "taxAdjustmentAmount" -> 100
          )
        )
      ),
      "otherTaxDue" -> Json.obj(
        "amount" -> 100,
        "taxAdjustmentComponents" -> Json.arr(
          Json.obj(
            "taxAdjustmentType" -> "ExcessGiftAidTax",
            "taxAdjustmentAmount" -> 100
          )
        )
      ),
      "alreadyTaxedAtSource" -> Json.obj(
        "amount" -> 100,
        "taxAdjustmentComponents" -> Json.arr(
          Json.obj(
            "taxAdjustmentType" -> "TaxOnBankBSInterest",
            "taxAdjustmentAmount" -> 100
          )
        )
      )
    ),
    "links" -> Json.arr()
  )

  private val income = uk.gov.hmrc.tai.model.domain.income.Incomes(Seq.empty[TaxCodeIncome], NonTaxCodeIncome(None, Seq(
    OtherNonTaxCodeIncome(Profit, None, 100, "Profit")
  )))

  val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "Employer1", OtherBasisOfOperation, Live)
  val employment = Employment("company name", Some("888"), new LocalDate(2019, 5, 26), None,
    Seq.empty[AnnualAccount], "", "", 1, Some(BigDecimal(100)), hasPayrolledBenefit = false, receivingOccupationalPension = true)
  val codingComponentSeq = Seq(CodingComponent(EmployerProvidedServices, Some(12), 12321, "Some Description"),
    CodingComponent(GiftsSharesCharity, Some(31), 12345, "Some Description Some"))
  val incomeSource = TaxedIncome(taxCodeIncome, employment)

  private def createSUT = new SUT

  val httpHandler: HttpHandler = mock[HttpHandler]

  private class SUT extends TaxAccountConnector(httpHandler) {
    override val serviceUrl: String = "mockUrl"
  }

}
