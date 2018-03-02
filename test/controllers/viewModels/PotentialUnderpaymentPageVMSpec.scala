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

package controllers.viewModels

import builders.UserBuilder
import controllers.{FakeTaiPlayApplication, routes}
import data.TaiData
import uk.gov.hmrc.tai.viewModels.EstimatedIncomeViewModel
import uk.gov.hmrc.tai.model.TaxSummaryDetails
import org.mockito.Matchers.any
import org.mockito.Mockito.verify
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.service.TaiService
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.urls.Link
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.TaxSummaryDetails

class PotentialUnderpaymentPageVMSpec
extends UnitSpec
with FakeTaiPlayApplication {



  "PotentialUnderpaymentPageVM total value extraction functions " should {

    val nino = new Generator().nextNino

    val taxSummaryDetails: TaxSummaryDetails =  TaiData.getInYearAdjustmentJsonTaxSummary
    val taxSummaryDetailsWithoutIYA : TaxSummaryDetails =TaiData.getAdditionalTaxWithoutUnderpayment
    val taxSummaryDetailsWithNegativeCYPlusOneIYA: TaxSummaryDetails = TaiData.getInYearAdjustmentJsonTaxSummaryCYandNegativeCYPlusOne
    val pageViewModelWithIYA = PotentialUnderpaymentPageVM.createObject(nino,taxSummaryDetails)(UserBuilder.apply(),HeaderCarrier())
    val pageViewModelWithoutIYA = PotentialUnderpaymentPageVM.createObject(nino,taxSummaryDetailsWithoutIYA)(UserBuilder.apply(),HeaderCarrier())
    val pageViewModelWithNegativeCYPlusOneIYA = PotentialUnderpaymentPageVM.createObject(nino,taxSummaryDetailsWithNegativeCYPlusOneIYA)(UserBuilder.apply(),HeaderCarrier())

    "return the sum of all the in year adjustment totals " in {
      val total:BigDecimal = pageViewModelWithIYA.iyaTotalAmount
      total shouldBe BigDecimal(123)
    }
    "return the sum of all the in year adjustments for current year " in {
      val total:BigDecimal = pageViewModelWithIYA.iyaCYAmount
      total shouldBe BigDecimal(100)
    }
    "return the sum of all the in year adjustments for current year plus one " in {
      val total:BigDecimal = pageViewModelWithIYA.iyaCYPlusOneAmount
      total shouldBe BigDecimal(23)
    }
    "return the in year adjustment tax code adjustment amount for current year " in {
      val total:BigDecimal = pageViewModelWithIYA.iyaTaxCodeChangeAmount
      total shouldBe BigDecimal(500)
    }
    "return the display flag to show iya cy and cy plus one " in {
      pageViewModelWithIYA.displayCYOnly shouldBe false
      pageViewModelWithIYA.displayCYPlusOneOnly shouldBe false
      pageViewModelWithIYA.displayNoValues shouldBe false
      pageViewModelWithIYA.displayCYAndCYPlusOneOnly shouldBe true
    }

    "not return the sum of all the in year adjustment totals if not there " in {
      val total:BigDecimal = pageViewModelWithoutIYA.iyaTotalAmount
      total shouldBe BigDecimal(0)
    }
    "not return the sum of all the in year adjustments for current year if not there " in {
      val total:BigDecimal = pageViewModelWithoutIYA.iyaCYAmount
      total shouldBe BigDecimal(0)
    }
    "not return the sum of all the in year adjustments for current year plus one if not there " in {
      val total:BigDecimal = pageViewModelWithoutIYA.iyaCYPlusOneAmount
      total shouldBe BigDecimal(0)
    }
    "not return the in year adjustment tax code adjustment amount for current year if not there " in {
      val total:BigDecimal = pageViewModelWithoutIYA.iyaTaxCodeChangeAmount
      total shouldBe BigDecimal(0)
    }
    "return the display flag for display no values " in {
      pageViewModelWithoutIYA.displayCYOnly shouldBe false
      pageViewModelWithoutIYA.displayCYPlusOneOnly shouldBe false
      pageViewModelWithoutIYA.displayNoValues shouldBe true
      pageViewModelWithoutIYA.displayCYAndCYPlusOneOnly shouldBe false
    }

    "return flags to display adjustment into current year only" when {
      "both CY and CYPlusOne values are present within NPS Tax Account JSON, but the CYPlusOne value is negative" in {
        pageViewModelWithNegativeCYPlusOneIYA.displayCYOnly shouldBe true
        pageViewModelWithNegativeCYPlusOneIYA.displayCYPlusOneOnly shouldBe false
        pageViewModelWithNegativeCYPlusOneIYA.displayNoValues shouldBe false
        pageViewModelWithNegativeCYPlusOneIYA.displayCYAndCYPlusOneOnly shouldBe false
      }
    }

  }
}
