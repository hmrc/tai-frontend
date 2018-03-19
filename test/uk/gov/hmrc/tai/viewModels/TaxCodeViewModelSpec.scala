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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome, Week1Month1BasisOperation}
import uk.gov.hmrc.urls.Link

import scala.collection.immutable.ListMap

class TaxCodeViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val taxYear = uk.gov.hmrc.tai.model.tai.TaxYear()
  val taxYearSuffix = Messages("tai.taxCode.title.pt2",
    taxYear.start.toString("d MMMM yyyy"),
    taxYear.end.toString("d MMMM yyyy"))
  val expectedNonBreakSuffix = Messages("tai.taxCode.title.pt2",
    taxYear.start.toString("d MMMM yyyy").replaceAll(" ", "\u00A0"),
    taxYear.end.toString("d MMMM yyyy").replaceAll(" ", "\u00A0"))

  "TaxCodeViewModel apply method" must {
    "be able to form view model object with single TaxCodeIncome" when {
      "provided with valid input" in {
        val result = sut(Seq(taxCodeIncomes1))
        result.mainHeading mustBe s"${Messages("tai.taxCode.single.code.title.pt1")} ${expectedNonBreakSuffix}"
        result.ledeMessage mustBe Messages("tai.taxCode.single.info")
        result.title mustBe s"${Messages("tai.taxCode.single.code.title.pt1")} ${taxYearSuffix}"
      }
    }

    "be able to form view model object with multiple TaxCodeIncome" when {
      "provided with valid input" in {
        val result = sut(Seq(taxCodeIncomes1, taxCodeIncomes2))
        result.mainHeading mustBe s"${Messages("tai.taxCode.multiple.code.title.pt1")} ${expectedNonBreakSuffix}"
        result.ledeMessage mustBe Messages("tai.taxCode.multiple.info")
        result.title mustBe s"${Messages("tai.taxCode.multiple.code.title.pt1")} ${taxYearSuffix}"
      }
    }

    "be able to create a tax code description table heading" when {
      "BasisOperations is OtherBasisOperation" in {
        val result = sut(Seq(taxCodeIncomes1))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer1", "1150L")
      }

      "BasisOperations is Week1Month1BasisOperation" in {
        val result = sut(Seq(taxCodeIncomes2))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer2", "BRX")
      }
    }

    "be able to create table contents for scottish tax code" when {
      "taxCode has scottish indicator" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "S1150L")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems.head mustBe("S", Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml))
      }
    }

    "be able to create table contents for untaxed income tax code" when {
      "taxCode has untaxed indicator" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "K100", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "K100")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "K100", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("K" -> Messages("tai.taxCode.K"),
          "100" -> Messages(s"tai.taxCode.untaxedAmount", 1000))
      }
    }

    "be able to create table contents for 0 personal allowance income tax code" when {
      "taxCode has untaxed indicator" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "0T", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "0T")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "0T", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("0T" -> Messages("tai.taxCode.0T"))
      }
    }

    "be able to create table contents for suffixes" when {
      "taxCode has basic tax free personal allowance" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "1150L", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "1150L")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "1150L", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("1150" -> Messages(s"tai.taxCode.amount", 11500),
          "L" -> Messages("tai.taxCode.L"))
      }

      "provide taxCodeExplanation when only single digit proceeds letter T" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "2T", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("2" -> Messages(s"tai.taxCode.amount", 20),
          "T" -> Messages("tai.taxCode.T"))
      }

      "provide taxCodeExplanation when only single digit proceeds letter N" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "2N", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("2" -> Messages(s"tai.taxCode.amount", 20),
          "N" -> Messages("tai.taxCode.N"))
      }
    }

    "be able to create table contents for emergency tax codes" when {
      "taxCode has emergency tax code with X suffix" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "1150L", "employer", Week1Month1BasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "1150LX")
      }

      "provide taxCodeExplanation" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "1150L", "employer", Week1Month1BasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("1150" -> Messages(s"tai.taxCode.amount", 11500),
          "L" -> Messages("tai.taxCode.L"),
          "X" -> Messages("tai.taxCode.X"))
      }
    }

    "be able to create table contents for different types of income tax code" when {
      "provide taxCodeExplanation for scottish and untaxed code" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "SK100", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "K" -> Messages("tai.taxCode.K"),
          "100" -> Messages(s"tai.taxCode.untaxedAmount", 1000))
      }

      "provide taxCodeExplanation for scottish and 0 personal allowance code" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S0T", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "0T" -> Messages("tai.taxCode.0T"))
      }

      "provide taxCodeExplanation for scottish and suffix code" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150M", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "M" -> Messages("tai.taxCode.M"))
      }

      "provide taxCodeExplanation for scottish and suffix code with 0T to clash with stand-alone tax code" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150T", "employer", OtherBasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "T" -> Messages("tai.taxCode.T"))
      }

      "provide taxCodeExplanation for scottish, suffix code with emergency tax code" in {
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150T", "employer", Week1Month1BasisOperation, Live)
        val result = sut(Seq(taxCodeIncome))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "T" -> Messages("tai.taxCode.T"),
          "X" -> Messages("tai.taxCode.X"))
      }
    }
  }

  "standAloneTaxCodeExplanation" must {
    "return the correct message" when {
      "tax code has 0T" in {
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription("S0T", OtherBasisOperation, None)) mustBe
          ListMap("0T" -> Messages(s"tai.taxCode.0T"))
      }

      "tax code has BR" in {
        val taxCode = "BR"
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, None)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode"))
      }

      "tax code has D0" in {
        val taxCode = "D0"
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, None)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode"))
      }

      "tax code has D1" in {
        val taxCode = "D1"
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, None)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode"))
      }

      "tax code has NT" in {
        val taxCode = "NT"
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, None)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode"))
      }

      "tax code has D2" in {
        val taxCode = "D2"
        val scottishTaxRate = Some(10)
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, scottishTaxRate)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode", scottishTaxRate.get))
      }

      "tax code has D3" in {
        val taxCode = "D3"
        val scottishTaxRate = Some(10)
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, scottishTaxRate)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode", scottishTaxRate.get))
      }

      "tax code has D4" in {
        val taxCode = "D4"
        val scottishTaxRate = Some(10)
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, scottishTaxRate)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode", scottishTaxRate.get))
      }

      "tax code has D5" in {
        val taxCode = "D5"
        val scottishTaxRate = Some(10)
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, scottishTaxRate)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode", scottishTaxRate.get))
      }

      "tax code has D6" in {
        val taxCode = "D6"
        val scottishTaxRate = Some(10)
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, scottishTaxRate)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode", scottishTaxRate.get))
      }

      "tax code has D7" in {
        val taxCode = "D7"
        val scottishTaxRate = Some(10)
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, scottishTaxRate)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode", scottishTaxRate.get))
      }

      "tax code has D8" in {
        val taxCode = "D8"
        val scottishTaxRate = Some(10)
        TaxCodeDescriptor.standAloneTaxCodeExplanation(TaxCodeDescription(taxCode, OtherBasisOperation, scottishTaxRate)) mustBe
          ListMap(taxCode -> Messages(s"tai.taxCode.$taxCode", scottishTaxRate.get))
      }
    }
  }

  private val taxCodeIncomes1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOperation, Live)
  private val taxCodeIncomes2 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "BR", "employer2", Week1Month1BasisOperation, Live)
  private val nino = new Generator().nextNino

  def sut(taxCodeIncomes: Seq[TaxCodeIncome]) = TaxCodeViewModel(taxCodeIncomes, nino)

}
