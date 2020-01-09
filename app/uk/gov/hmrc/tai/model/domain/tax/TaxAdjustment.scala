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

import play.api.libs.json._

sealed trait TaxAdjustmentType

trait ReliefsGivingBackTax extends TaxAdjustmentType
trait OtherTaxDue extends TaxAdjustmentType
trait AlreadyTaxedAtSource extends TaxAdjustmentType
trait TaxReliefComponent extends TaxAdjustmentType

case object EnterpriseInvestmentSchemeRelief extends ReliefsGivingBackTax
case object ConcessionalRelief extends ReliefsGivingBackTax
case object MaintenancePayments extends ReliefsGivingBackTax
case object MarriedCouplesAllowance extends ReliefsGivingBackTax
case object DoubleTaxationRelief extends ReliefsGivingBackTax

case object ExcessGiftAidTax extends OtherTaxDue
case object ExcessWidowsAndOrphans extends OtherTaxDue
case object PensionPaymentsAdjustment extends OtherTaxDue
case object ChildBenefit extends OtherTaxDue

case object TaxOnBankBSInterest extends AlreadyTaxedAtSource
case object TaxCreditOnUKDividends extends AlreadyTaxedAtSource
case object TaxCreditOnForeignInterest extends AlreadyTaxedAtSource
case object TaxCreditOnForeignIncomeDividends extends AlreadyTaxedAtSource

case object PersonalPensionPayment extends TaxReliefComponent
case object PersonalPensionPaymentRelief extends TaxReliefComponent
case object GiftAidPayments extends TaxReliefComponent
case object GiftAidPaymentsRelief extends TaxReliefComponent

object TaxAdjustmentType {
  implicit val formatTaxAdjustmentType = new Format[TaxAdjustmentType] {
    override def writes(taxAdjustmentType: TaxAdjustmentType): JsValue = ???

    override def reads(json: JsValue): JsResult[TaxAdjustmentType] =
      json.as[String] match {
        case "EnterpriseInvestmentSchemeRelief"  => JsSuccess(EnterpriseInvestmentSchemeRelief)
        case "ConcessionalRelief"                => JsSuccess(ConcessionalRelief)
        case "MaintenancePayments"               => JsSuccess(MaintenancePayments)
        case "MarriedCouplesAllowance"           => JsSuccess(MarriedCouplesAllowance)
        case "DoubleTaxationRelief"              => JsSuccess(DoubleTaxationRelief)
        case "ExcessGiftAidTax"                  => JsSuccess(ExcessGiftAidTax)
        case "ExcessWidowsAndOrphans"            => JsSuccess(ExcessWidowsAndOrphans)
        case "PensionPaymentsAdjustment"         => JsSuccess(PensionPaymentsAdjustment)
        case "ChildBenefit"                      => JsSuccess(ChildBenefit)
        case "TaxOnBankBSInterest"               => JsSuccess(TaxOnBankBSInterest)
        case "TaxCreditOnUKDividends"            => JsSuccess(TaxCreditOnUKDividends)
        case "TaxCreditOnForeignInterest"        => JsSuccess(TaxCreditOnForeignInterest)
        case "TaxCreditOnForeignIncomeDividends" => JsSuccess(TaxCreditOnForeignIncomeDividends)
        case "PersonalPensionPayment"            => JsSuccess(PersonalPensionPayment)
        case "PersonalPensionPaymentRelief"      => JsSuccess(PersonalPensionPaymentRelief)
        case "GiftAidPayments"                   => JsSuccess(GiftAidPayments)
        case "GiftAidPaymentsRelief"             => JsSuccess(GiftAidPaymentsRelief)
        case _                                   => throw new IllegalArgumentException("Invalid income category type")
      }
  }
}

case class TaxAdjustmentComponent(taxAdjustmentType: TaxAdjustmentType, taxAdjustmentAmount: BigDecimal)

object TaxAdjustmentComponent {
  implicit val format: Format[TaxAdjustmentComponent] = Json.format[TaxAdjustmentComponent]
}

case class TaxAdjustment(amount: BigDecimal, taxAdjustmentComponents: Seq[TaxAdjustmentComponent])

object TaxAdjustment {
  implicit val format: Format[TaxAdjustment] = Json.format[TaxAdjustment]
}
