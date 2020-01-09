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

package uk.gov.hmrc.tai.model.domain.formatters

import org.joda.time.LocalDate
import play.api.libs.json.{JsResult, JsSuccess, JsValue, Reads}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._

trait CodingComponentFormatters {

  val taxComponentTypeReads = new Reads[TaxComponentType] {
    override def reads(json: JsValue): JsResult[TaxComponentType] = {
      val taxComponentType = json.as[String]
      val component = taxComponentTypeMap(taxComponentType)
      JsSuccess(component)
    }
  }

  val codingComponentReads = new Reads[CodingComponent] {
    override def reads(json: JsValue): JsResult[CodingComponent] = {
      val componentType = (json \ "componentType").as[TaxComponentType](taxComponentTypeReads)
      val employmentId = (json \ "employmentId").asOpt[Int]
      val amount = (json \ "amount").as[BigDecimal]
      val description = (json \ "description").as[String]
      val inputAmount = (json \ "inputAmount").asOpt[BigDecimal]
      JsSuccess(CodingComponent(componentType, employmentId, amount, description, inputAmount))
    }
  }

  private val taxComponentTypeMap: Map[String, TaxComponentType] = Map(
    "GiftAidPayments"                                      -> GiftAidPayments,
    "PersonalPensionPayments"                              -> PersonalPensionPayments,
    "MaintenancePayments"                                  -> MaintenancePayments,
    "EmployerProvidedServices"                             -> EmployerProvidedServices,
    "BalancingCharge"                                      -> BalancingCharge,
    "LoanInterestAmount"                                   -> LoanInterestAmount,
    "BlindPersonsAllowance"                                -> BlindPersonsAllowance,
    "BpaReceivedFromSpouseOrCivilPartner"                  -> BpaReceivedFromSpouseOrCivilPartner,
    "CommunityInvestmentTaxCredit"                         -> CommunityInvestmentTaxCredit,
    "GiftsSharesCharity"                                   -> GiftsSharesCharity,
    "RetirementAnnuityPayments"                            -> RetirementAnnuityPayments,
    "NonCodedIncome"                                       -> NonCodedIncome,
    "Commission"                                           -> Commission,
    "OtherIncomeEarned"                                    -> OtherIncomeEarned,
    "OtherIncomeNotEarned"                                 -> OtherIncomeNotEarned,
    "PartTimeEarnings"                                     -> PartTimeEarnings,
    "Tips"                                                 -> Tips,
    "OtherEarnings"                                        -> OtherEarnings,
    "CasualEarnings"                                       -> CasualEarnings,
    "BenefitInKind"                                        -> BenefitInKind,
    "CarFuelBenefit"                                       -> CarFuelBenefit,
    "MedicalInsurance"                                     -> MedicalInsurance,
    "CarBenefit"                                           -> CarBenefit,
    "Telephone"                                            -> Telephone,
    "ServiceBenefit"                                       -> ServiceBenefit,
    "TaxableExpensesBenefit"                               -> TaxableExpensesBenefit,
    "VanBenefit"                                           -> VanBenefit,
    "VanFuelBenefit"                                       -> VanFuelBenefit,
    "BeneficialLoan"                                       -> BeneficialLoan,
    "Accommodation"                                        -> Accommodation,
    "Assets"                                               -> Assets,
    "AssetTransfer"                                        -> AssetTransfer,
    "EducationalServices"                                  -> EducationalServices,
    "Entertaining"                                         -> Entertaining,
    "Expenses"                                             -> Expenses,
    "Mileage"                                              -> Mileage,
    "NonQualifyingRelocationExpenses"                      -> NonQualifyingRelocationExpenses,
    "NurseryPlaces"                                        -> NurseryPlaces,
    "OtherItems"                                           -> OtherItems,
    "PaymentsOnEmployeesBehalf"                            -> PaymentsOnEmployeesBehalf,
    "PersonalIncidentalExpenses"                           -> PersonalIncidentalExpenses,
    "QualifyingRelocationExpenses"                         -> QualifyingRelocationExpenses,
    "EmployerProvidedProfessionalSubscription"             -> EmployerProvidedProfessionalSubscription,
    "IncomeTaxPaidButNotDeductedFromDirectorsRemuneration" -> IncomeTaxPaidButNotDeductedFromDirectorsRemuneration,
    "TravelAndSubsistence"                                 -> TravelAndSubsistence,
    "VouchersAndCreditCards"                               -> VouchersAndCreditCards,
    "JobExpenses"                                          -> JobExpenses,
    "FlatRateJobExpenses"                                  -> FlatRateJobExpenses,
    "ProfessionalSubscriptions"                            -> ProfessionalSubscriptions,
    "HotelAndMealExpenses"                                 -> HotelAndMealExpenses,
    "OtherExpenses"                                        -> OtherExpenses,
    "VehicleExpenses"                                      -> VehicleExpenses,
    "MileageAllowanceRelief"                               -> MileageAllowanceRelief,
    "ForeignDividendIncome"                                -> ForeignDividendIncome,
    "ForeignPropertyIncome"                                -> ForeignPropertyIncome,
    "ForeignInterestAndOtherSavings"                       -> ForeignInterestAndOtherSavings,
    "ForeignPensionsAndOtherIncome"                        -> ForeignPensionsAndOtherIncome,
    "StatePension"                                         -> StatePension,
    "OccupationalPension"                                  -> OccupationalPension,
    "PublicServicesPension"                                -> PublicServicesPension,
    "ForcesPension"                                        -> ForcesPension,
    "PersonalPensionAnnuity"                               -> PersonalPensionAnnuity,
    "Profit"                                               -> Profit,
    "BankOrBuildingSocietyInterest"                        -> BankOrBuildingSocietyInterest,
    "UkDividend"                                           -> UkDividend,
    "UnitTrust"                                            -> UnitTrust,
    "StockDividend"                                        -> StockDividend,
    "NationalSavings"                                      -> NationalSavings,
    "SavingsBond"                                          -> SavingsBond,
    "PurchasedLifeAnnuities"                               -> PurchasedLifeAnnuities,
    "UntaxedInterestIncome"                                -> UntaxedInterestIncome,
    "IncapacityBenefit"                                    -> IncapacityBenefit,
    "JobSeekersAllowance"                                  -> JobSeekersAllowance,
    "VentureCapitalTrust"                                  -> VentureCapitalTrust,
    "GiftAidAdjustment"                                    -> GiftAidAdjustment,
    "MarriedCouplesAllowanceToWifeMAW"                     -> MarriedCouplesAllowanceToWifeMAW,
    "DoubleTaxationRelief"                                 -> DoubleTaxationRelief,
    "ConcessionRelief"                                     -> ConcessionRelief,
    "EnterpriseInvestmentScheme"                           -> EnterpriseInvestmentScheme,
    "EarlyYearsAdjustment"                                 -> EarlyYearsAdjustment,
    "LossRelief"                                           -> LossRelief,
    "ForeignPensionAllowance"                              -> ForeignPensionAllowance,
    "MarriedCouplesAllowanceMAE"                           -> MarriedCouplesAllowanceMAE,
    "MarriedCouplesAllowanceMCCP"                          -> MarriedCouplesAllowanceMCCP,
    "SurplusMarriedCouplesAllowanceToWifeWAE"              -> SurplusMarriedCouplesAllowanceToWifeWAE,
    "MarriedCouplesAllowanceToWifeWMA"                     -> MarriedCouplesAllowanceToWifeWMA,
    "NonCashBenefit"                                       -> NonCashBenefit,
    "PersonalAllowancePA"                                  -> PersonalAllowancePA,
    "PersonalAllowanceAgedPAA"                             -> PersonalAllowanceAgedPAA,
    "PersonalAllowanceElderlyPAE"                          -> PersonalAllowanceElderlyPAE,
    "EmploymentAndSupportAllowance"                        -> EmploymentAndSupportAllowance,
    "ChildBenefit"                                         -> ChildBenefit,
    "MarriageAllowanceTransferred"                         -> MarriageAllowanceTransferred,
    "MarriageAllowanceReceived"                            -> MarriageAllowanceReceived,
    "DividendTax"                                          -> DividendTax,
    "UnderPaymentFromPreviousYear"                         -> UnderPaymentFromPreviousYear,
    "OutstandingDebt"                                      -> OutstandingDebt,
    "EstimatedTaxYouOweThisYear"                           -> EstimatedTaxYouOweThisYear,
    "UnderpaymentRestriction"                              -> UnderpaymentRestriction,
    "HigherPersonalAllowanceRestriction"                   -> HigherPersonalAllowanceRestriction,
    "AdjustmentToRateBand"                                 -> AdjustmentToRateBand,
    "EmploymentIncome"                                     -> EmploymentIncome,
    "PensionIncome"                                        -> PensionIncome,
    "JobSeekerAllowanceIncome"                             -> JobSeekerAllowanceIncome,
    "OtherIncome"                                          -> OtherIncome
  )

  val taxCodeIncomeSourceReads = new Reads[TaxCodeIncome] {
    override def reads(json: JsValue): JsResult[TaxCodeIncome] = {
      val componentType = (json \ "componentType").as[TaxComponentType](taxComponentTypeReads)
      val employmentId = (json \ "employmentId").asOpt[Int]
      val amount = (json \ "amount").as[BigDecimal]
      val description = (json \ "description").as[String]
      val taxCode = (json \ "taxCode").as[String]
      val name = (json \ "name").as[String]
      val basisOperation = (json \ "basisOperation").as[BasisOfOperation]
      val status = (json \ "status").as[TaxCodeIncomeSourceStatus](taxCodeIncomeSourceStatusReads)
      val iabdUpdateSource = (json \ "iabdUpdateSource").asOpt[IabdUpdateSource]
      val updateNotificationDate = (json \ "updateNotificationDate").asOpt[LocalDate]
      val updateActionDate = (json \ "updateActionDate").asOpt[LocalDate]

      JsSuccess(
        TaxCodeIncome(
          componentType,
          employmentId,
          amount,
          description,
          taxCode,
          name,
          basisOperation,
          status,
          iabdUpdateSource,
          updateNotificationDate,
          updateActionDate))
    }
  }

  val taxCodeIncomeSourceStatusReads = new Reads[TaxCodeIncomeSourceStatus] {
    override def reads(json: JsValue): JsResult[TaxCodeIncomeSourceStatus] =
      json.as[String] match {
        case "Live"              => JsSuccess(Live)
        case "PotentiallyCeased" => JsSuccess(PotentiallyCeased)
        case "Ceased"            => JsSuccess(Ceased)
        case _                   => throw new IllegalArgumentException("Invalid TaxCodeIncomeSourceStatus type")
      }
  }
}
