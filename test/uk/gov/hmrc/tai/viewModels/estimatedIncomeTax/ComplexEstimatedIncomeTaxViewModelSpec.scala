/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util.constants.{BandTypesConstants, TaxRegionConstants}
import utils.BaseSpec

import scala.language.postfixOps

class ComplexEstimatedIncomeTaxViewModelSpec extends BaseSpec with TaxRegionConstants with BandTypesConstants {

  "ComplexEstimatedIncomeTaxViewModel" must {
    "return a valid view model" in {

      val taxAccountSummary = TaxAccountSummary(700, 11500, 0, 0, 0, 16500, 11500)

      val codingComponents = Seq(
        CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance", Some(11500))
      )

      val taxBands = List(
        TaxBand("B", "", 3500, 700, Some(0), Some(33500), 20),
        TaxBand("SR", "", 1500, 0, Some(0), Some(5000), 45)
      )

      val bandedGraph = BandedGraph(
        TaxGraph,
        List(Band(TaxFree, 69.69, 11500, 0, ZeroBand), Band("Band", 30.30, 5000, 700, NonZeroBand)),
        0,
        16500,
        16500,
        69.69,
        11500,
        99.99,
        700,
        None,
        Some(Swatch(4.24, 700))
      )

      val expectedViewModel = ComplexEstimatedIncomeTaxViewModel(700, 16500, 11500, bandedGraph, UkTaxRegion)

      ComplexEstimatedIncomeTaxViewModel(codingComponents, taxAccountSummary, taxCodeIncome, taxBands) mustBe expectedViewModel

    }
  }

  val taxCodeIncome = Seq(
    TaxCodeIncome(
      EmploymentIncome,
      Some(1),
      BigDecimal(15000),
      "EmploymentIncome",
      "1150L",
      "TestName",
      OtherBasisOfOperation,
      Live,
      None,
      Some(new LocalDate(2015, 11, 26)),
      Some(new LocalDate(2015, 11, 26))
    )
  )

}
