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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util.constants.{BandTypesConstants, TaxRegionConstants}

import scala.language.postfixOps

class ZeroTaxEstimatedIncomeTaxViewModelSpec
    extends PlaySpec with FakeTaiPlayApplication with I18nSupport with TaxRegionConstants with BandTypesConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "ZeroTaxEstimatedIncomeTaxViewModel" must {
    "return a valid view model" in {

      val taxAccountSummary = TaxAccountSummary(0, 10500, 0, 0, 0, 9000, 11500)

      val codingComponents = Seq(
        CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance", Some(11500))
      )

      val bandedGraph = BandedGraph(
        TaxGraph,
        List(Band(TaxFree, 78.26, 11500, 0, "pa")),
        0,
        11500,
        11500,
        78.26,
        11500,
        78.26,
        0,
        None,
        None)

      val expectedViewModel = ZeroTaxEstimatedIncomeTaxViewModel(0, 9000, 11500, bandedGraph, UkTaxRegion)

      ZeroTaxEstimatedIncomeTaxViewModel(
        codingComponents,
        taxAccountSummary,
        Seq.empty[TaxCodeIncome],
        List.empty[TaxBand]) mustBe expectedViewModel

    }
  }
}
