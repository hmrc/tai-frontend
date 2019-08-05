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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.urls.Link

import scala.collection.immutable.ListMap
import scala.concurrent.Future

class TaxCodeViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val taxYear = uk.gov.hmrc.tai.model.TaxYear()
  val taxYearSuffix =
    Messages("tai.taxCode.title.pt2", taxYear.start.toString("d MMMM yyyy"), taxYear.end.toString("d MMMM yyyy"))
  val expectedNonBreakSuffix = Messages(
    "tai.taxCode.title.pt2",
    taxYear.start.toString("d MMMM yyyy").replaceAll(" ", "\u00A0"),
    taxYear.end.toString("d MMMM yyyy").replaceAll(" ", "\u00A0"))

  val prevTaxYear = uk.gov.hmrc.tai.model.TaxYear().prev
  val prevTaxYearSuffix = Messages(
    "tai.taxCode.title.pt2",
    prevTaxYear.start.toString("d MMMM yyyy"),
    prevTaxYear.end.toString("d MMMM yyyy"))
  val prevExpectedNonBreakSuffix = Messages(
    "tai.taxCode.title.pt2",
    prevTaxYear.start.toString("d MMMM yyyy").replaceAll(" ", "\u00A0"),
    prevTaxYear.end.toString("d MMMM yyyy").replaceAll(" ", "\u00A0")
  )

  "TaxCodeViewModel apply method" must {
    "be able to form view model object with single TaxCodeIncome" when {
      "provided with valid input" in {
        val result = testViewModel(Seq(taxCodeIncomes1))
        result.mainHeading mustBe Messages("tai.taxCode.single.code.title", taxYearStartNonBreak, taxYearEndNonBreak)
        result.ledeMessage mustBe Messages("tai.taxCode.single.info")
        result.title mustBe Messages("tai.taxCode.single.code.title", taxYearStartNonBreak, taxYearEndNonBreak)
      }
    }

    "be able to form view model object with multiple TaxCodeIncome" when {
      "provided with valid input" in {
        val result = testViewModel(Seq(taxCodeIncomes1, taxCodeIncomes2))
        result.mainHeading mustBe Messages("tai.taxCode.multiple.code.title", taxYearStartNonBreak, taxYearEndNonBreak)
        result.ledeMessage mustBe Messages("tai.taxCode.multiple.info")
        result.title mustBe Messages("tai.taxCode.multiple.code.title", taxYearStartNonBreak, taxYearEndNonBreak)
      }
    }

    "be able to create a tax code description table heading" when {
      "BasisOperations is OtherBasisOperation" in {
        val result = testViewModel(Seq(taxCodeIncomes1))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer1", "1150L")
      }

      "BasisOperations is Week1Month1BasisOperation" in {
        val result = testViewModel(Seq(taxCodeIncomes2))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer2", "BR")
      }
    }

    "be able to create table contents for scottish tax code" when {
      "taxCode has scottish indicator" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "S1150L")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems.head mustBe ("S", Messages(
          s"tai.taxCode.S",
          Link
            .toExternalPage(
              url = ApplicationConfig.scottishRateIncomeTaxUrl,
              value = Some(Messages("tai.taxCode.scottishIncomeText.link")))
            .toHtml
        ))
      }
    }

    "be able to create table contents for untaxed income tax code" when {
      "taxCode has untaxed indicator" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "K100", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "K100")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "K100", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "K"   -> Messages("tai.taxCode.K"),
          "100" -> Messages(s"tai.taxCode.untaxedAmount", 1000))
      }
    }

    "be able to create table contents for 0 personal allowance income tax code" when {
      "taxCode has untaxed indicator" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "0T", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "0T")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "0T", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("0T" -> Messages("tai.taxCode.0T"))
      }
    }

    "be able to create table contents for suffixes" when {
      "taxCode has basic tax free personal allowance" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "1150L", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "1150L")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "1150L", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "L"    -> Messages("tai.taxCode.L"))
      }

      "provide taxCodeExplanation when only single digit proceeds letter T" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "2T", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "2" -> Messages(s"tai.taxCode.amount", 20),
          "T" -> Messages("tai.taxCode.T"))
      }

      "provide taxCodeExplanation when only single digit proceeds letter N" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "2N", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "2" -> Messages(s"tai.taxCode.amount", 20),
          "N" -> Messages("tai.taxCode.N"))
      }
    }

    "be able to create table contents for emergency tax codes" when {
      "taxCode has emergency tax code with X suffix" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(1),
          1111,
          "employer",
          "1150L",
          "employer",
          Week1Month1BasisOfOperation,
          Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "1150L")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(1),
          1111,
          "employer",
          "1150L",
          "employer",
          Week1Month1BasisOfOperation,
          Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "L"    -> Messages("tai.taxCode.L"),
          "X"    -> Messages("tai.taxCode.X"))
      }
    }

    "be able to create table contents for different types of income tax code" when {
      "provide taxCodeExplanation for scottish and untaxed code" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "SK100", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "S" -> Messages(
            s"tai.taxCode.S",
            Link
              .toExternalPage(
                url = ApplicationConfig.scottishRateIncomeTaxUrl,
                value = Some(Messages("tai.taxCode.scottishIncomeText.link")))
              .toHtml
          ),
          "K"   -> Messages("tai.taxCode.K"),
          "100" -> Messages(s"tai.taxCode.untaxedAmount", 1000)
        )
      }

      "provide taxCodeExplanation for scottish and 0 personal allowance code" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S0T", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "S" -> Messages(
            s"tai.taxCode.S",
            Link
              .toExternalPage(
                url = ApplicationConfig.scottishRateIncomeTaxUrl,
                value = Some(Messages("tai.taxCode.scottishIncomeText.link")))
              .toHtml
          ),
          "0T" -> Messages("tai.taxCode.0T")
        )
      }

      "provide taxCodeExplanation for scottish and suffix code" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150M", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "S" -> Messages(
            s"tai.taxCode.S",
            Link
              .toExternalPage(
                url = ApplicationConfig.scottishRateIncomeTaxUrl,
                value = Some(Messages("tai.taxCode.scottishIncomeText.link")))
              .toHtml
          ),
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "M"    -> Messages("tai.taxCode.M")
        )
      }

      "provide taxCodeExplanation for scottish and suffix code with 0T to clash with stand-alone tax code" in {
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150T", "employer", OtherBasisOfOperation, Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "S" -> Messages(
            s"tai.taxCode.S",
            Link
              .toExternalPage(
                url = ApplicationConfig.scottishRateIncomeTaxUrl,
                value = Some(Messages("tai.taxCode.scottishIncomeText.link")))
              .toHtml
          ),
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "T"    -> Messages("tai.taxCode.T")
        )
      }

      "provide taxCodeExplanation for scottish, suffix code with emergency tax code" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(1),
          1111,
          "employer",
          "S1150T",
          "employer",
          Week1Month1BasisOfOperation,
          Live)
        val result = testViewModel(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap(
          "S" -> Messages(
            s"tai.taxCode.S",
            Link
              .toExternalPage(
                url = ApplicationConfig.scottishRateIncomeTaxUrl,
                value = Some(Messages("tai.taxCode.scottishIncomeText.link")))
              .toHtml
          ),
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "T"    -> Messages("tai.taxCode.T"),
          "X"    -> Messages("tai.taxCode.X")
        )
      }
    }
  }

  val taxYearStartNonBreak = taxYear.start.toString("d MMMM yyyy").replaceAll(" ", "\u00A0")
  val taxYearEndNonBreak = taxYear.end.toString("d MMMM yyyy").replaceAll(" ", "\u00A0")

  val previousTaxYearStartNonBreak = taxYear.prev.start.toString("d MMMM yyyy").replaceAll(" ", "\u00A0")
  val previousTaxYearEndNonBreak = taxYear.prev.end.toString("d MMMM yyyy").replaceAll(" ", "\u00A0")

  private val taxCodeIncomes1 =
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
  private val taxCodeIncomes2 =
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "BR", "employer2", Week1Month1BasisOfOperation, Live)
  private val nino = new Generator().nextNino
  private val scottishTaxRateBands = Map.empty[String, BigDecimal]

  def testViewModel(taxCodeIncomes: Seq[TaxCodeIncome]) = TaxCodeViewModel(taxCodeIncomes, scottishTaxRateBands)
}
