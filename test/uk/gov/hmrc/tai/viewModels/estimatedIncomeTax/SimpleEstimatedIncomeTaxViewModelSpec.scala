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
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util.{BandTypesConstants, TaxRegionConstants}

import scala.language.postfixOps

class SimpleEstimatedIncomeTaxViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with TaxRegionConstants with BandTypesConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]


  "Simple Estimated Income Tax View Model" must {
    "return a valid view model for valid input" in {

      val taxAccountSummary = TaxAccountSummary(7834,2772,0,0,0,47835,11500)

       val codingComponents = Seq(
         CodingComponent(PersonalAllowancePA,None,11500,"Personal Allowance",Some(11500)),
         CodingComponent(CarBenefit,Some(1),8026,"Car Benefit",None),
         CodingComponent(MedicalInsurance,Some(1),637,"Medical Insurance",None),
         CodingComponent(OtherItems,Some(1),65,"Other Items",None)
       )

       val basicRateTaxBand = TaxBand("B","",33500,6700,Some(0),Some(33500),20)
       val higherRateTaxBand = TaxBand("D0","",2835,1134,Some(33500),Some(150000),40)

       val taxBands = List(
         basicRateTaxBand,
         higherRateTaxBand
       )

       val mergedTaxBands = List(
         TaxBand("pa","",11500,0,Some(0),None,0),
         basicRateTaxBand,
         higherRateTaxBand
       )

       val bandedGraph = BandedGraph(TaxGraph,
         List(
           Band(TaxFree,24.04,11500,0,ZeroBand),
           Band("Band",75.95,36335,7834,NonZeroBand))
         ,0,150000,47835,24.04,11500,99.99,7834,
         Some("You can earn £102,165 more before your income reaches the next tax band."),
         Some(Swatch(16.37,7834)))

       val expectedViewModel = SimpleEstimatedIncomeTaxViewModel(7834,47835,11500,bandedGraph,UkTaxRegion,mergedTaxBands)

       SimpleEstimatedIncomeTaxViewModel(codingComponents,taxAccountSummary,ukTaxCodeIncome,taxBands) mustBe expectedViewModel

    }
  }

  "createPABand must return a Personal Allowance Taxband for a given Tax Free Allowance" in {
    SimpleEstimatedIncomeTaxViewModel.createPABand(11500) mustBe TaxBand(TaxFreeAllowanceBand, "", 11500, 0, Some(0), None, 0)
  }

  "findTaxRegion" must {
    "return UK when a UK TaxCode is present" in {
      SimpleEstimatedIncomeTaxViewModel.findTaxRegion(ukTaxCodeIncome) mustBe UkTaxRegion
    }
    "return Scottish when a Scottish TaxCode is Present" in {
      SimpleEstimatedIncomeTaxViewModel.findTaxRegion(ScottishTaxCodeIncome) mustBe ScottishTaxRegion
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

      val taxBands = SimpleEstimatedIncomeTaxViewModel.retrieveTaxBands(taxBand)

      taxBands mustBe taxBand

    }

    "return sorted tax bands" in {
      val taxBand = List(
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40)
      )

      val taxBands = SimpleEstimatedIncomeTaxViewModel.retrieveTaxBands(taxBand)

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


      val taxBands = SimpleEstimatedIncomeTaxViewModel.retrieveTaxBands(bankIntTaxBand ::: untaxedTaxBand)

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


      val taxBands = SimpleEstimatedIncomeTaxViewModel.retrieveTaxBands(bankIntTaxBand ::: untaxedTaxBand)

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
