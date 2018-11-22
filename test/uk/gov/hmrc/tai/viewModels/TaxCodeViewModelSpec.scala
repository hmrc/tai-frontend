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
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.urls.Link

import scala.collection.immutable.ListMap
import scala.concurrent.Future

class TaxCodeViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "TaxCodeViewModel applyTaxCodeIncomes method" must {
    "be able to form view model object with single TaxCodeIncome" when {
      "provided with valid input" in {
        val result = testIncomeViewModel(Seq(taxIncome1))
        result.mainHeading mustBe Messages("tai.taxCode.single.code.title",taxYearStartNonBreak,taxYearEndNonBreak)
        result.ledeMessage mustBe Messages("tai.taxCode.single.info")
        result.title mustBe Messages("tai.taxCode.single.code.title",taxYearStartNonBreak,taxYearEndNonBreak)
      }
    }

    "be able to form view model object with multiple TaxCodeIncome" when {
      "provided with valid input" in {
        val result = testIncomeViewModel(Seq(taxIncome1, taxIncome2))
        result.mainHeading mustBe Messages("tai.taxCode.multiple.code.title",taxYearStartNonBreak,taxYearEndNonBreak)
        result.ledeMessage mustBe Messages("tai.taxCode.multiple.info")
        result.title mustBe Messages("tai.taxCode.multiple.code.title",taxYearStartNonBreak,taxYearEndNonBreak)
      }
    }
  }
  
  "TaxCodeViewModel apply method" must {
    "be able to form view model object with single TaxCodeRecord" when {
      "provided with valid input" in {
        val result = testRecordViewModel(Seq(taxCodeRecord1))
        result.mainHeading mustBe Messages("tai.taxCode.single.code.title",taxYearStartNonBreak,taxYearEndNonBreak)
        result.ledeMessage mustBe Messages("tai.taxCode.single.info")
        result.title mustBe Messages("tai.taxCode.single.code.title",taxYearStartNonBreak,taxYearEndNonBreak)
      }
    }

    "be able to form view model object with multiple TaxCodeRecord" when {
      "provided with valid input" in {
        val result = testRecordViewModel(Seq(taxCodeRecord1, taxCodeRecord2))
        result.mainHeading mustBe Messages("tai.taxCode.multiple.code.title",taxYearStartNonBreak,taxYearEndNonBreak)
        result.ledeMessage mustBe Messages("tai.taxCode.multiple.info")
        result.title mustBe Messages("tai.taxCode.multiple.code.title",taxYearStartNonBreak,taxYearEndNonBreak)
      }

    }

    "be able to create a tax code description table heading" when {
      "BasisOperations is OtherBasisOperation" in {
        val result = testRecordViewModel(Seq(taxCodeRecord1))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "1150L")
      }


      "BasisOperations is Week1Month1BasisOperation" in {
        val result = testRecordViewModel(Seq(taxCodeRecord2))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer2", "BRX")
      }
    }

    val taxCodeRecordS1150L = makeTestTaxCodeRecord("S1150L", OtherBasisOfOperation)

    "be able to create table contents for scottish tax code" when {
      "taxCode has scottish indicator" in {
        val result = testRecordViewModel(Seq(taxCodeRecordS1150L))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "S1150L")
      }

      "provide taxCodeExplanation" in {
        val result = testRecordViewModel(Seq(taxCodeRecordS1150L))
        result.taxCodeDetails.head.descriptionItems.head mustBe("S", Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml))
      }
    }

    val taxCodeRecordK100 = makeTestTaxCodeRecord("K100", OtherBasisOfOperation)

    "be able to create table contents for untaxed income tax code" when {
      "taxCode has untaxed indicator" in {
        val result = testRecordViewModel(Seq(taxCodeRecordK100))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "K100")
      }

      "provide taxCodeExplanation" in {
        val result = testRecordViewModel(Seq(taxCodeRecordK100))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("K" -> Messages("tai.taxCode.K"),
          "100" -> Messages(s"tai.taxCode.untaxedAmount", 1000))
      }
    }

    val taxCodeRecord0T = makeTestTaxCodeRecord("0T", OtherBasisOfOperation)

    "be able to create table contents for 0 personal allowance income tax code" when {
      "taxCode has untaxed indicator" in {
        val result = testRecordViewModel(Seq(taxCodeRecord0T))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "0T")
      }

      "provide taxCodeExplanation" in {
        val result = testRecordViewModel(Seq(taxCodeRecord0T))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("0T" -> Messages("tai.taxCode.0T"))
      }
    }

    val taxCodeRecord1150L = makeTestTaxCodeRecord("1150L", OtherBasisOfOperation)
    val taxCodeRecord2T = makeTestTaxCodeRecord("2T", OtherBasisOfOperation)
    val taxCodeRecord2N = makeTestTaxCodeRecord("2N", OtherBasisOfOperation)

    "be able to create table contents for suffixes" when {
      "taxCode has basic tax free personal allowance" in {
        val result = testRecordViewModel(Seq(taxCodeRecord1150L))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "1150L")
      }

      "provide taxCodeExplanation" in {
        val result = testRecordViewModel(Seq(taxCodeRecord1150L))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("1150" -> Messages(s"tai.taxCode.amount", 11500),
          "L" -> Messages("tai.taxCode.L"))
      }

      "provide taxCodeExplanation when only single digit proceeds letter T" in {
        val result = testRecordViewModel(Seq(taxCodeRecord2T))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("2" -> Messages(s"tai.taxCode.amount", 20),
          "T" -> Messages("tai.taxCode.T"))
      }

      "provide taxCodeExplanation when only single digit proceeds letter N" in {
        val result = testRecordViewModel(Seq(taxCodeRecord2N))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("2" -> Messages(s"tai.taxCode.amount", 20),
          "N" -> Messages("tai.taxCode.N"))
      }
    }


    "be able to create table contents for emergency tax codes" when {
      "taxCode has emergency tax code with X suffix" in {
        val taxCodeRecord1150LWeek1Month1 = makeTestTaxCodeRecord("1150LX", Week1Month1BasisOfOperation)
        val result = testRecordViewModel(Seq(taxCodeRecord1150LWeek1Month1))
        result.taxCodeDetails.head.heading mustBe Messages("tai.taxCode.subheading", "employer", "1150LX")
      }

      "provide taxCodeExplanation" in {
        val taxCodeRecord1150LWeek1Month1 = makeTestTaxCodeRecord("1150L", Week1Month1BasisOfOperation)
        val result = testRecordViewModel(Seq(taxCodeRecord1150LWeek1Month1))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("1150" -> Messages(s"tai.taxCode.amount", 11500),
          "L" -> Messages("tai.taxCode.L"),
          "X" -> Messages("tai.taxCode.X"))
      }

    }

    val taxCodeRecordSK100 = makeTestTaxCodeRecord("SK100", OtherBasisOfOperation)
    val taxCodeRecordS0T = makeTestTaxCodeRecord("S0T", OtherBasisOfOperation)
    val taxCodeRecordS1150M = makeTestTaxCodeRecord("S1150M", OtherBasisOfOperation)
    val taxCodeRecordS1150T = makeTestTaxCodeRecord("S1150T", OtherBasisOfOperation)

    "be able to create table contents for different types of income tax code" when {
      "asked for taxCodeExplanation for scottish and untaxed code" in {
        val result = testRecordViewModel(Seq(taxCodeRecordSK100))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "K" -> Messages("tai.taxCode.K"),
          "100" -> Messages(s"tai.taxCode.untaxedAmount", 1000))
      }

      "asked for taxCodeExplanation for scottish and 0 personal allowance code" in {
        val result = testRecordViewModel(Seq(taxCodeRecordS0T))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "0T" -> Messages("tai.taxCode.0T"))
      }

      "asked for taxCodeExplanation for scottish and suffix code" in {
        val result = testRecordViewModel(Seq(taxCodeRecordS1150M))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "M" -> Messages("tai.taxCode.M"))
      }

      "asked for taxCodeExplanation for scottish and suffix code with 0T to clash with stand-alone tax code" in {
        val result = testRecordViewModel(Seq(taxCodeRecordS1150T))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "T" -> Messages("tai.taxCode.T"))
      }

      "asked for taxCodeExplanation for scottish, suffix code with emergency tax code" in {
        val taxCodeRecord = makeTestTaxCodeRecord("S1150T", Week1Month1BasisOfOperation)
        val result = testRecordViewModel(Seq(taxCodeRecord))
        result.taxCodeDetails.head.descriptionItems mustBe ListMap("S" -> Messages(s"tai.taxCode.S",
          Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value = Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml),
          "1150" -> Messages(s"tai.taxCode.amount", 11500),
          "T" -> Messages("tai.taxCode.T"),
          "X" -> Messages("tai.taxCode.X"))
      }
    }
  }

  val taxYear = uk.gov.hmrc.tai.model.TaxYear()

  val taxYearStartNonBreak = taxYear.start.toString("d MMMM yyyy").replaceAll(" ", "\u00A0")
  val taxYearEndNonBreak = taxYear.end.toString("d MMMM yyyy").replaceAll(" ", "\u00A0")

  private def makeTestTaxCodeRecord(taxCode: String, basisOfOperation: BasisOfOperation, isPrimary: Boolean = true, employerName: String = "employer") = {
    TaxCodeRecord(taxCode,LocalDate.now,LocalDate.now,basisOfOperation,employerName,false,Some("payrollnumber"),isPrimary)
  }
  
  private def makeTestTaxCodeIncome(taxCode: String, employmentName: String): TaxCodeIncome = {
    TaxCodeIncome(EmploymentIncome, None, 100, "", taxCode, employmentName, OtherBasisOfOperation, Live)
  }

  private val taxIncome1 = makeTestTaxCodeIncome("1185L", "Employer")
  private val taxIncome2 = makeTestTaxCodeIncome("DR", "Other Employer")
  
  private val taxCodeRecord1 = makeTestTaxCodeRecord("1150L", OtherBasisOfOperation)
  private val taxCodeRecord2 = makeTestTaxCodeRecord("BRX", OtherBasisOfOperation, false, "employer2")

  private val scottishTaxRateBands = Map.empty[String, BigDecimal]

  def testRecordViewModel(taxCodeRecords: Seq[TaxCodeRecord]) = TaxCodeViewModel(taxCodeRecords, scottishTaxRateBands)
  def testIncomeViewModel(taxCodeIncomes: Seq[TaxCodeIncome]) = TaxCodeViewModel.applyTaxCodeIncomes(taxCodeIncomes, scottishTaxRateBands)

}
