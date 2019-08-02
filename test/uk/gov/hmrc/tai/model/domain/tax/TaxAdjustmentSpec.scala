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

package uk.gov.hmrc.tai.model.domain.tax

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class TaxAdjustmentSpec extends PlaySpec {

  "TaxAdjustmentType" must {
    "be able to parse json" when {
      "given a valid json with EnterpriseInvestmentSchemeRelief" in {
        taxAdjustmentTypeJson("EnterpriseInvestmentSchemeRelief")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(EnterpriseInvestmentSchemeRelief, 10)))
      }

      "given a valid json with ConcessionalRelief" in {
        taxAdjustmentTypeJson("ConcessionalRelief")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(ConcessionalRelief, 10)))
      }

      "given a valid json with MaintenancePayments" in {
        taxAdjustmentTypeJson("MaintenancePayments")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(MaintenancePayments, 10)))
      }

      "given a valid json with MarriedCouplesAllowance" in {
        taxAdjustmentTypeJson("MarriedCouplesAllowance")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(MarriedCouplesAllowance, 10)))
      }

      "given a valid json with DoubleTaxationRelief" in {
        taxAdjustmentTypeJson("DoubleTaxationRelief")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(DoubleTaxationRelief, 10)))
      }

      "given a valid json with ExcessGiftAidTax" in {
        taxAdjustmentTypeJson("ExcessGiftAidTax")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(ExcessGiftAidTax, 10)))
      }

      "given a valid json with ExcessWidowsAndOrphans" in {
        taxAdjustmentTypeJson("ExcessWidowsAndOrphans")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(ExcessWidowsAndOrphans, 10)))
      }

      "given a valid json with PensionPaymentsAdjustment" in {
        taxAdjustmentTypeJson("PensionPaymentsAdjustment")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(PensionPaymentsAdjustment, 10)))
      }

      "given a valid json with ChildBenefit" in {
        taxAdjustmentTypeJson("ChildBenefit")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(ChildBenefit, 10)))
      }

      "given a valid json with TaxOnBankBSInterest" in {
        taxAdjustmentTypeJson("TaxOnBankBSInterest")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(TaxOnBankBSInterest, 10)))
      }

      "given a valid json with TaxCreditOnUKDividends" in {
        taxAdjustmentTypeJson("TaxCreditOnUKDividends")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(TaxCreditOnUKDividends, 10)))
      }

      "given a valid json with TaxCreditOnForeignInterest" in {
        taxAdjustmentTypeJson("TaxCreditOnForeignInterest")
          .as[TaxAdjustment] mustBe TaxAdjustment(10, Seq(TaxAdjustmentComponent(TaxCreditOnForeignInterest, 10)))
      }

      "given a valid json with TaxCreditOnForeignIncomeDividends" in {
        taxAdjustmentTypeJson("TaxCreditOnForeignIncomeDividends").as[TaxAdjustment] mustBe TaxAdjustment(
          10,
          Seq(TaxAdjustmentComponent(TaxCreditOnForeignIncomeDividends, 10)))
      }
    }

    "throw an exception" when {
      "given an invalid json value" in {
        val exception = the[IllegalArgumentException] thrownBy taxAdjustmentTypeJson("invalid").as[TaxAdjustment]
        exception.getMessage mustBe "Invalid income category type"
      }
    }
  }

  private def taxAdjustmentTypeJson(taxAdjustmentType: String) =
    Json.obj(
      "amount" -> 10,
      "taxAdjustmentComponents" -> Json.arr(
        Json.obj(
          "taxAdjustmentType"   -> taxAdjustmentType,
          "taxAdjustmentAmount" -> 10
        )
      )
    )

}
