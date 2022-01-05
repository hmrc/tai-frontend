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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.Application
import play.api.http.ContentTypes
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, UnauthorizedException}
import uk.gov.hmrc.tai.connectors.responses._
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.webchat.client.WebChatClient
import uk.gov.hmrc.webchat.testhelpers.WebChatClientStub
import utils.{BaseSpec, WireMockHelper}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class TaxAccountConnectorSpec extends BaseSpec with WireMockHelper with ScalaFutures with IntegrationPatience {

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure("microservice.services.tai.port" -> server.port)
    .overrides(
      bind[WebChatClient].toInstance(new WebChatClientStub)
    )
    .build()

  lazy val taxAccountUrl = s"/tai/$ninoAsString/tax-account/${currentTaxYear.year}/income/tax-code-incomes"
  lazy val codingComponentsUrl = s"/tai/$nino/tax-account/${currentTaxYear.year}/tax-components"
  lazy val incomeSourceUrl =
    s"/tai/$nino/tax-account/year/${currentTaxYear.year}/income/${EmploymentIncome.toString}/status/${Live.toString}"
  lazy val taxAccountSummaryUrl = s"/tai/$nino/tax-account/${currentTaxYear.year}/summary"
  lazy val nonTaxCodeIncomeUrl = s"/tai/$nino/tax-account/${currentTaxYear.year}/income"
  lazy val totalTaxUrl = s"/tai/$nino/tax-account/${currentTaxYear.year}/total-tax"

  "tax account url" must {
    "fetch the url to connect to TAI to retrieve tax codes" in {
      taxAccountConnector
        .taxAccountUrl(ninoAsString, currentTaxYear) mustBe s"${taxAccountConnector.serviceUrl}/tai/$ninoAsString/tax-account/${currentTaxYear.year}/income/tax-code-incomes"
    }
  }

  "taxCode" should {
    "fetch the tax codes" when {
      "provided with valid nino" in {
        server.stubFor(
          get(taxAccountUrl)
            .willReturn(
              aResponse.withBody(taxCodeIncomeJson.toString())
            ))

        taxAccountConnector.taxCodeIncomes(nino, currentTaxYear).futureValue mustBe TaiSuccessResponseWithPayload(
          Seq(taxCodeIncome))
      }
    }

    "return a TaiTaxAccountFailureResponse" when {
      "tai sends an invalid json" in {
        server.stubFor(
          get(taxAccountUrl)
            .willReturn(
              aResponse.withBody(corruptJsonResponse.toString())
            ))

        taxAccountConnector.taxCodeIncomes(nino, currentTaxYear).futureValue mustBe a[TaiTaxAccountFailureResponse]
      }
    }

    "return a TaiUnauthorisedResponse" when {
      "the http response is Unauthorized" in {

        server.stubFor(
          get(taxAccountUrl)
            .willReturn(unauthorized())
        )

        taxAccountConnector.taxCodeIncomes(nino, currentTaxYear).futureValue mustBe a[TaiUnauthorisedResponse]
      }
    }
  }

  "incomeSources" should {
    "fetch the tax codes" when {
      "provided with valid nino" in {

        server.stubFor(
          get(incomeSourceUrl)
            .willReturn(
              aResponse.withBody(incomeSourceJson.toString())
            ))

        val result = taxAccountConnector.incomeSources(nino, currentTaxYear, EmploymentIncome, Live).futureValue
        result mustBe TaiSuccessResponseWithPayload(Seq(incomeSource))
      }
    }

    "fetch empty seq" when {
      "provided with nino that is not found" in {

        server.stubFor(
          get(incomeSourceUrl)
            .willReturn(
              aResponse.withBody(incomeSourceEmpty.toString())
            ))

        val result = taxAccountConnector.incomeSources(nino, currentTaxYear, EmploymentIncome, Live).futureValue
        result mustBe TaiSuccessResponseWithPayload(Seq.empty[TaxedIncome])
      }
    }

    "return a TaiTaxAccountFailureResponse" when {
      "tai sends an invalid json" in {

        server.stubFor(
          get(incomeSourceUrl)
            .willReturn(
              aResponse.withBody(corruptJsonResponse.toString())
            ))

        val result = taxAccountConnector.incomeSources(nino, currentTaxYear, EmploymentIncome, Live).futureValue
        result mustBe a[TaiTaxAccountFailureResponse]

      }
    }

    "return a TaiUnauthorisedResponse" when {
      "the http response is Unauthorized" in {

        server.stubFor(
          get(incomeSourceUrl)
            .willReturn(
              unauthorized()
            ))

        val result = taxAccountConnector.incomeSources(nino, currentTaxYear, EmploymentIncome, Live).futureValue
        result mustBe a[TaiUnauthorisedResponse]
      }
    }
  }

  "codingComponents" should {
    "fetch the coding components" when {
      "provided with valid nino" in {

        server.stubFor(
          get(codingComponentsUrl)
            .willReturn(
              aResponse.withBody(codingComponentSampleJson.toString())
            ))

        val result = taxAccountConnector.codingComponents(nino, currentTaxYear).futureValue
        result mustBe TaiSuccessResponseWithPayload(codingComponentSeq)
      }
    }

    "return a TaiTaxAccountFailureResponse" when {
      "tai sends an invalid json" in {

        server.stubFor(
          get(codingComponentsUrl)
            .willReturn(
              aResponse.withBody(corruptJsonResponse.toString())
            ))

        val result = taxAccountConnector.codingComponents(nino, currentTaxYear).futureValue
        result mustBe a[TaiTaxAccountFailureResponse]
      }
    }

    "return a TaiUnauthorisedResponse" when {
      "the http response is Unauthorized" in {

        server.stubFor(
          get(codingComponentsUrl)
            .willReturn(
              unauthorized()
            ))

        val result = taxAccountConnector.codingComponents(nino, currentTaxYear).futureValue
        result mustBe a[TaiUnauthorisedResponse]
      }
    }

    "return a TaiNotFoundResponse" when {
      "the http response is NotFound" in {
        server.stubFor(
          get(codingComponentsUrl)
            .willReturn(
              notFound()
            ))

        val result = taxAccountConnector.codingComponents(nino, currentTaxYear).futureValue
        result mustBe a[TaiNotFoundResponse]
      }
    }
  }

  "taxAccountSummary" should {
    "fetch the tax account summary" when {
      "provided with valid nino" in {
        val taxAccountSummaryJson = Json.obj(
          "data" -> Json.obj(
            "totalEstimatedTax"                  -> 111,
            "taxFreeAmount"                      -> 222,
            "totalInYearAdjustmentIntoCY"        -> 1111.11,
            "totalInYearAdjustment"              -> 2222.23,
            "totalInYearAdjustmentIntoCYPlusOne" -> 1111.12,
            "totalEstimatedIncome"               -> 100,
            "taxFreeAllowance"                   -> 200
          ),
          "links" -> Json.arr()
        )

        server.stubFor(
          get(taxAccountSummaryUrl)
            .willReturn(
              aResponse.withBody(taxAccountSummaryJson.toString())
            ))

        val result = taxAccountConnector.taxAccountSummary(nino, currentTaxYear).futureValue
        result mustBe TaiSuccessResponseWithPayload(TaxAccountSummary(111, 222, 1111.11, 2222.23, 1111.12, 100, 200))
      }
    }

    "return a TaiTaxAccountFailureResponse" when {
      "tai sends an invalid json" in {
        val corruptTaxAccountSummaryJson = Json.obj(
          "data" -> Json.obj(
            "totalEstimatedTax222" -> 111,
            "taxFreeAmount11"      -> 222
          ),
          "links" -> Json.arr())

        server.stubFor(
          get(taxAccountSummaryUrl)
            .willReturn(
              aResponse.withBody(corruptTaxAccountSummaryJson.toString())
            ))

        val result = taxAccountConnector.taxAccountSummary(nino, currentTaxYear).futureValue
        result mustBe a[TaiTaxAccountFailureResponse]
      }
    }

    "return a TaiUnauthorisedResponse" when {
      "the http response is Unauthorized" in {

        server.stubFor(
          get(taxAccountSummaryUrl)
            .willReturn(
              unauthorized()
            ))

        val result = taxAccountConnector.taxAccountSummary(nino, currentTaxYear).futureValue
        result mustBe a[TaiUnauthorisedResponse]
      }
    }

    "return a TaiNotFoundResponse" when {
      "the http response is NotFound" in {

        server.stubFor(
          get(taxAccountSummaryUrl)
            .willReturn(
              notFound()
            ))

        val result = taxAccountConnector.taxAccountSummary(nino, currentTaxYear).futureValue
        result mustBe a[TaiNotFoundResponse]
      }
    }
  }

  "nonTaxCodeComponents" should {
    "fetch the non tax code incomes" when {
      "provided with valid nino" in {

        server.stubFor(
          get(nonTaxCodeIncomeUrl)
            .willReturn(
              aResponse.withBody(incomeJson.toString())
            ))

        val result = taxAccountConnector.nonTaxCodeIncomes(nino, currentTaxYear).futureValue
        result mustBe TaiSuccessResponseWithPayload(income.nonTaxCodeIncomes)
      }
    }

    "thrown exception" when {
      "tai sends an invalid json" in {

        server.stubFor(
          get(nonTaxCodeIncomeUrl)
            .willReturn(
              aResponse.withBody(corruptJsonResponse.toString())
            ))

        val result = taxAccountConnector.nonTaxCodeIncomes(nino, currentTaxYear).futureValue
        result mustBe a[TaiTaxAccountFailureResponse]
      }
    }

    "return a TaiUnauthorisedResponse" when {
      "the http response is Unauthorized" in {

        server.stubFor(
          get(nonTaxCodeIncomeUrl)
            .willReturn(
              unauthorized()
            ))

        val result = taxAccountConnector.nonTaxCodeIncomes(nino, currentTaxYear).futureValue
        result mustBe a[TaiUnauthorisedResponse]
      }
    }
  }

  "updateEstimatedIncome" must {
    "return Success" when {
      "update tax code income returns 200" in {
        val id = 1

        val url =
          s"/tai/$nino/tax-account/snapshots/${currentTaxYear.year}/incomes/tax-code-incomes/$id/estimated-pay"

        server.stubFor(
          put(url)
            .withHeader(CONTENT_TYPE, matching(ContentTypes.JSON))
            .willReturn(
              ok
            ))

        val result = taxAccountConnector.updateEstimatedIncome(nino, TaxYear(), 100, id).futureValue
        result mustBe TaiSuccessResponse
      }
    }
  }

  "total tax" must {
    "return the total tax details for the given year" when {
      "provided with nino" in {

        server.stubFor(
          get(totalTaxUrl)
            .willReturn(
              aResponse.withBody(totalTaxJson.toString())
            ))

        val expectedTotalTax = TotalTax(
          1000,
          List(
            IncomeCategory(
              UkDividendsIncomeCategory,
              10,
              20,
              30,
              List(TaxBand("", "", 0, 0, None, None, 0), TaxBand("B", "BR", 10000, 500, Some(5000), Some(20000), 10)))),
          Some(tax.TaxAdjustment(100, List(TaxAdjustmentComponent(EnterpriseInvestmentSchemeRelief, 100)))),
          Some(tax.TaxAdjustment(100, List(TaxAdjustmentComponent(ExcessGiftAidTax, 100)))),
          Some(tax.TaxAdjustment(100, List(TaxAdjustmentComponent(TaxOnBankBSInterest, 100))))
        )

        val result = taxAccountConnector.totalTax(nino, currentTaxYear).futureValue
        result mustBe TaiSuccessResponseWithPayload(expectedTotalTax)
      }
    }

    "return TaiNotFoundResponse" when {
      "the http response is NotFound" in {
        server.stubFor(
          get(totalTaxUrl)
            .willReturn(
              notFound()
            ))

        val result = taxAccountConnector.totalTax(nino, currentTaxYear).futureValue
        result mustBe a[TaiNotFoundResponse]
      }
    }

    "return failure" when {
      "update tax code income returns 500" in {

        server.stubFor(
          get(totalTaxUrl)
            .willReturn(
              aResponse.withBody(corruptJsonResponse.toString())
            ))

        val result = taxAccountConnector.totalTax(nino, currentTaxYear).futureValue
        result mustBe a[TaiTaxAccountFailureResponse]
      }
    }

    "return a TaiUnauthorisedResponse" when {
      "the http response is Unauthorized" in {

        server.stubFor(
          get(totalTaxUrl)
            .willReturn(
              unauthorized()
            ))

        val result = taxAccountConnector.totalTax(nino, currentTaxYear).futureValue
        result mustBe a[TaiUnauthorisedResponse]
      }
    }
  }

  private val currentTaxYear = TaxYear()

  def ninoAsString: String = nino.value

  val taxCodeIncomeJson: JsValue = Json.obj(
    "data" -> JsArray(
      Seq(Json.obj(
        "componentType"  -> "EmploymentIncome",
        "employmentId"   -> 1,
        "amount"         -> 1111,
        "description"    -> "employment",
        "taxCode"        -> "1150L",
        "name"           -> "Employer1",
        "basisOperation" -> "OtherBasisOperation",
        "status"         -> "Live"
      ))),
    "links" -> JsArray(Seq())
  )

  val incomeSourceJson: JsValue = Json.obj(
    "data" -> Json.arr(
      Json.obj(
        "taxCodeIncome" -> Json.obj(
          "componentType"                 -> "EmploymentIncome",
          "employmentId"                  -> 1,
          "amount"                        -> 1111,
          "description"                   -> "employment",
          "taxCode"                       -> "1150L",
          "name"                          -> "Employer1",
          "basisOperation"                -> "OtherBasisOperation",
          "status"                        -> "Live",
          "inYearAdjustmentIntoCY"        -> 0,
          "totalInYearAdjustment"         -> 0,
          "inYearAdjustmentIntoCYPlusOne" -> 0
        ),
        "employment" -> Json.obj(
          "name"                         -> "company name",
          "employmentStatus"             -> "Live",
          "payrollNumber"                -> "888",
          "startDate"                    -> "2019-05-26",
          "annualAccounts"               -> Json.arr(),
          "taxDistrictNumber"            -> "",
          "payeNumber"                   -> "",
          "sequenceNumber"               -> 1,
          "cessationPay"                 -> 100,
          "hasPayrolledBenefit"          -> false,
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
        "employmentId"  -> 12,
        "amount"        -> 12321,
        "description"   -> "Some Description",
        "iabdCategory"  -> "Benefit"),
      Json.obj(
        "componentType" -> "GiftsSharesCharity",
        "employmentId"  -> 31,
        "amount"        -> 12345,
        "description"   -> "Some Description Some",
        "iabdCategory"  -> "Allowance"
      )
    ),
    "links" -> Json.arr()
  )

  val corruptJsonResponse: JsValue = Json.obj(
    "data" -> JsArray(
      Seq(
        Json.obj(
          "employmentId"   -> 1,
          "amount"         -> 1111,
          "description"    -> "employment",
          "taxCode"        -> "1150L",
          "name"           -> "Employer1",
          "basisOperation" -> "OtherBasisOperation"
        ))),
    "links" -> JsArray(Seq())
  )

  val incomeJson: JsValue = Json.obj(
    "data" -> Json.obj(
      "taxCodeIncomes" -> JsArray(),
      "nonTaxCodeIncomes" -> Json.obj(
        "otherNonTaxCodeIncomes" -> Json.arr(
          Json.obj(
            "incomeComponentType" -> "Profit",
            "amount"              -> 100,
            "description"         -> "Profit"
          )
        )
      )
    ),
    "links" -> Json.arr()
  )

  private val totalTaxJson = Json.obj(
    "data" -> Json.obj(
      "amount" -> 1000,
      "incomeCategories" -> Json.arr(
        Json.obj(
          "incomeCategoryType" -> "UkDividendsIncomeCategory",
          "totalTax"           -> 10,
          "totalTaxableIncome" -> 20,
          "totalIncome"        -> 30,
          "taxBands" -> Json.arr(
            Json.obj(
              "bandType" -> "",
              "code"     -> "",
              "income"   -> 0,
              "tax"      -> 0,
              "rate"     -> 0
            ),
            Json.obj(
              "bandType"  -> "B",
              "code"      -> "BR",
              "income"    -> 10000,
              "tax"       -> 500,
              "lowerBand" -> 5000,
              "upperBand" -> 20000,
              "rate"      -> 10
            )
          )
        )
      ),
      "reliefsGivingBackTax" -> Json.obj(
        "amount" -> 100,
        "taxAdjustmentComponents" -> Json.arr(
          Json.obj(
            "taxAdjustmentType"   -> "EnterpriseInvestmentSchemeRelief",
            "taxAdjustmentAmount" -> 100
          )
        )
      ),
      "otherTaxDue" -> Json.obj(
        "amount" -> 100,
        "taxAdjustmentComponents" -> Json.arr(
          Json.obj(
            "taxAdjustmentType"   -> "ExcessGiftAidTax",
            "taxAdjustmentAmount" -> 100
          )
        )
      ),
      "alreadyTaxedAtSource" -> Json.obj(
        "amount" -> 100,
        "taxAdjustmentComponents" -> Json.arr(
          Json.obj(
            "taxAdjustmentType"   -> "TaxOnBankBSInterest",
            "taxAdjustmentAmount" -> 100
          )
        )
      )
    ),
    "links" -> Json.arr()
  )

  private val income = uk.gov.hmrc.tai.model.domain.income.Incomes(
    Seq.empty[TaxCodeIncome],
    NonTaxCodeIncome(
      None,
      Seq(
        OtherNonTaxCodeIncome(Profit, None, 100, "Profit")
      )))

  val taxCodeIncome =
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "Employer1", OtherBasisOfOperation, Live)
  val employment = Employment(
    "company name",
    Live,
    Some("888"),
    new LocalDate(2019, 5, 26),
    None,
    Seq.empty[AnnualAccount],
    "",
    "",
    1,
    Some(BigDecimal(100)),
    hasPayrolledBenefit = false,
    receivingOccupationalPension = true
  )
  val codingComponentSeq = Seq(
    CodingComponent(EmployerProvidedServices, Some(12), 12321, "Some Description"),
    CodingComponent(GiftsSharesCharity, Some(31), 12345, "Some Description Some")
  )
  val incomeSource = TaxedIncome(taxCodeIncome, employment)

  lazy val taxAccountConnector = new TaxAccountConnector(inject[HttpHandler], servicesConfig)
}
