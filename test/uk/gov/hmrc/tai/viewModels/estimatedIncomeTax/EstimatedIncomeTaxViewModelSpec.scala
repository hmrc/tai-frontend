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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util.{BandTypesConstants, TaxRegionConstants}

import scala.language.postfixOps

class EstimatedIncomeTaxViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with TaxRegionConstants with BandTypesConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]


  "Estimated Income Tax View Model" must {
    "return a valid view model" in {

      val taxAccountSummary = TaxAccountSummary(700, 11500, 0, 0, 0, 16500, 11500)

      val codingComponents = Seq(
        CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance", Some(11500))
      )

      val taxBands = List(
        TaxBand("B", "", 3500, 700, Some(0), Some(33500), 20),
        TaxBand("SR", "", 1500, 0, Some(0), Some(5000), 45)
      )

      val bandedGraph = BandedGraph(TaxGraph,
        List(
          Band(TaxFree, 69.69, 11500, 0, ZeroBand),
          Band("Band", 30.30, 5000, 700, NonZeroBand))
        , 0, 16500, 16500, 69.69, 11500, 99.99, 700,
        None,
        Some(Swatch(4.24, 700)))

      val expectedViewModel = EstimatedIncomeTaxViewModel(700, 16500, 11500, bandedGraph, UkTaxRegion)

      EstimatedIncomeTaxViewModel(codingComponents, taxAccountSummary, ukTaxCodeIncome, taxBands) mustBe expectedViewModel

    }
  }

  "createPABand must return a Personal Allowance Taxband for a given Tax Free Allowance" in {
    EstimatedIncomeTaxViewModel.createPABand(11500) mustBe TaxBand(TaxFreeAllowanceBand, "", 11500, 0, Some(0), None, 0)
  }

  "findTaxRegion" must {
    "return UK when a UK TaxCode is present" in {
       EstimatedIncomeTaxViewModel.findTaxRegion(ukTaxCodeIncome) mustBe UkTaxRegion
    }
    "return Scottish when a Scottish TaxCode is Present" in {
      EstimatedIncomeTaxViewModel.findTaxRegion(ScottishTaxCodeIncome) mustBe ScottishTaxRegion
    }
  }

  "retrieve tax bands" must {

    "return tax bands" in {
      val taxBand = List(
        TaxBand("pa", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )

      val taxBands = EstimatedIncomeTaxViewModel.retrieveTaxBands(taxBand)

      taxBands mustBe taxBand

    }

    "return sorted tax bands" in {
      val taxBand = List(
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40)
      )

      val taxBands = EstimatedIncomeTaxViewModel.retrieveTaxBands(taxBand)

      taxBands mustBe List(
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )
    }

    "return merged tax bands for multiple PSR bands" in {
      val bankIntTaxBand = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

      val untaxedTaxBand = List(TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0),
        upperBand = Some(11000), rate = 0))


      val taxBands = EstimatedIncomeTaxViewModel.retrieveTaxBands(bankIntTaxBand ::: untaxedTaxBand)

      taxBands mustBe List(TaxBand("PSR", "", 10000, 0, Some(0), Some(11000), 0),
        TaxBand("B", "", 15000, 3000, Some(11000), Some(32000), 20))
    }

    "return ordered tax bands for multiple PSR SR SDR bands" in {
      val bankIntTaxBand = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

      val untaxedTaxBand = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SDR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0))


      val taxBands = EstimatedIncomeTaxViewModel.retrieveTaxBands(bankIntTaxBand ::: untaxedTaxBand)

      val resBands = List(TaxBand("SR", "", 5000, 0, Some(0), Some(11000), 0),
        TaxBand("PSR", "", 10000, 0, Some(0), Some(11000), 0),
        TaxBand("SDR", "", 5000, 0, Some(0), Some(11000), 0),
        TaxBand("B", "", 15000, 3000, Some(11000), Some(32000), 20))

      taxBands mustBe resBands
    }
  }

  val ukTaxCodeIncome = Seq(
    TaxCodeIncome(EmploymentIncome,Some(1),BigDecimal(15000),"EmploymentIncome","1150L","TestName",
      OtherBasisOperation,Live,None,Some(new LocalDate(2015,11,26)),Some(new LocalDate(2015,11,26)))
  )

  val ScottishTaxCodeIncome = Seq(
    TaxCodeIncome(EmploymentIncome,Some(1),BigDecimal(99999),"EmploymentIncome","SK723","TestName",
      OtherBasisOperation,Live,None,Some(new LocalDate(2015,11,26)),Some(new LocalDate(2015,11,26)))
  )
}
