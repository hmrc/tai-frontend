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

package uk.gov.hmrc.tai.model.domain

import play.api.i18n.Messages
import play.api.libs.json._

sealed trait TaxComponentType {
  def toMessage()(implicit messages: Messages): String = {
    s"${messages(s"tai.taxFreeAmount.table.taxComponent.${this.toString}")}"
  }
}

sealed trait AllowanceComponentType extends TaxComponentType
sealed trait BenefitComponentType  extends TaxComponentType
sealed trait DeductionComponentType extends TaxComponentType
sealed trait IncomeComponentType extends TaxComponentType

sealed trait EmploymentPensions
sealed trait TaxableStateBenefits
sealed trait OtherIncomes
sealed trait SavingAndInvestments

sealed trait TaxCodeIncomeComponentType extends IncomeComponentType
sealed trait NonTaxCodeIncomeComponentType extends IncomeComponentType

//Coding components
case object GiftAidPayments extends AllowanceComponentType
case object PersonalPensionPayments extends AllowanceComponentType
case object MaintenancePayments extends AllowanceComponentType
case object LoanInterestAmount extends AllowanceComponentType
case object BlindPersonsAllowance extends AllowanceComponentType
case object BpaReceivedFromSpouseOrCivilPartner extends AllowanceComponentType
case object CommunityInvestmentTaxCredit extends AllowanceComponentType
case object GiftsSharesCharity extends AllowanceComponentType
case object RetirementAnnuityPayments extends AllowanceComponentType
case object JobExpenses extends AllowanceComponentType
case object FlatRateJobExpenses extends AllowanceComponentType
case object ProfessionalSubscriptions extends AllowanceComponentType
case object HotelAndMealExpenses extends AllowanceComponentType
case object OtherExpenses extends AllowanceComponentType
case object VehicleExpenses extends AllowanceComponentType
case object MileageAllowanceRelief extends AllowanceComponentType
case object DoubleTaxationRelief extends AllowanceComponentType
case object ConcessionRelief extends AllowanceComponentType
case object EnterpriseInvestmentScheme extends AllowanceComponentType
case object EarlyYearsAdjustment extends AllowanceComponentType
case object LossRelief extends AllowanceComponentType
case object ForeignPensionAllowance extends AllowanceComponentType
case object MarriedCouplesAllowanceMAE extends AllowanceComponentType
case object MarriedCouplesAllowanceMCCP extends AllowanceComponentType
case object VentureCapitalTrust extends AllowanceComponentType
case object SurplusMarriedCouplesAllowanceToWifeWAE extends AllowanceComponentType
case object MarriedCouplesAllowanceToWifeWMA extends AllowanceComponentType
case object PersonalAllowancePA extends AllowanceComponentType
case object PersonalAllowanceAgedPAA extends AllowanceComponentType
case object PersonalAllowanceElderlyPAE extends AllowanceComponentType
case object MarriageAllowanceReceived extends AllowanceComponentType

case object MarriedCouplesAllowanceToWifeMAW extends DeductionComponentType
case object BalancingCharge extends DeductionComponentType
case object GiftAidAdjustment extends DeductionComponentType
case object ChildBenefit extends DeductionComponentType
case object MarriageAllowanceTransferred extends DeductionComponentType
case object DividendTax extends DeductionComponentType
case object UnderPaymentFromPreviousYear extends DeductionComponentType
case object OutstandingDebt extends DeductionComponentType
case object EstimatedTaxYouOweThisYear extends DeductionComponentType
case object UnderpaymentRestriction extends DeductionComponentType
case object HigherPersonalAllowanceRestriction extends DeductionComponentType
case object AdjustmentToRateBand extends DeductionComponentType

case object BenefitInKind extends BenefitComponentType { val name = "BenefitInKind" }
case object CarFuelBenefit extends BenefitComponentType { val name = "CarFuelBenefit" }
case object MedicalInsurance extends BenefitComponentType { val name = "MedicalInsurance" }
case object CarBenefit extends BenefitComponentType { val name = "CarBenefit" }
case object Telephone extends BenefitComponentType { val name = "Telephone" }
case object ServiceBenefit extends BenefitComponentType { val name = "ServiceBenefit" }
case object TaxableExpensesBenefit extends BenefitComponentType { val name = "TaxableExpensesBenefit" }
case object VanBenefit extends BenefitComponentType { val name = "VanBenefit" }
case object VanFuelBenefit extends BenefitComponentType { val name = "VanFuelBenefit" }
case object BeneficialLoan extends BenefitComponentType { val name = "BeneficialLoan" }
case object Accommodation extends BenefitComponentType { val name = "Accommodation" }
case object Assets extends BenefitComponentType { val name = "Assets" }
case object AssetTransfer extends BenefitComponentType { val name = "AssetTransfer" }
case object EducationalServices extends BenefitComponentType { val name = "EducationalServices" }
case object Entertaining extends BenefitComponentType { val name = "Entertaining" }
case object Expenses extends BenefitComponentType { val name = "Expenses" }
case object Mileage extends BenefitComponentType { val name = "Mileage" }
case object NonQualifyingRelocationExpenses extends BenefitComponentType { val name = "NonQualifyingRelocationExpenses" }
case object NurseryPlaces extends BenefitComponentType { val name = "NurseryPlaces" }
case object OtherItems extends BenefitComponentType { val name = "OtherItems" }
case object PaymentsOnEmployeesBehalf extends BenefitComponentType { val name = "PaymentsOnEmployeesBehalf" }
case object PersonalIncidentalExpenses extends BenefitComponentType { val name = "PersonalIncidentalExpenses" }
case object QualifyingRelocationExpenses extends BenefitComponentType { val name = "QualifyingRelocationExpenses" }
case object EmployerProvidedProfessionalSubscription extends BenefitComponentType { val name = "EmployerProvidedProfessionalSubscription" }
case object IncomeTaxPaidButNotDeductedFromDirectorsRemuneration extends BenefitComponentType { val name = "IncomeTaxPaidButNotDeductedFromDirectorsRemuneration" }
case object TravelAndSubsistence extends BenefitComponentType { val name = "TravelAndSubsistence" }
case object VouchersAndCreditCards extends BenefitComponentType { val name = "VouchersAndCreditCards" }
case object NonCashBenefit extends BenefitComponentType { val name = "NonCashBenefit" }
case object EmployerProvidedServices extends BenefitComponentType { val name = "EmployerProvidedServices" }

object BenefitComponentType {
  def apply(name: String): Option[BenefitComponentType] = name match {
    case BenefitInKind.name => Some(BenefitInKind)
    case CarFuelBenefit.name => Some(CarFuelBenefit)
    case MedicalInsurance.name => Some(MedicalInsurance)
    case CarBenefit.name => Some(CarBenefit)
    case Telephone.name => Some(Telephone)
    case ServiceBenefit.name => Some(ServiceBenefit)
    case TaxableExpensesBenefit.name => Some(TaxableExpensesBenefit)
    case VanBenefit.name => Some(VanBenefit)
    case VanFuelBenefit.name => Some(VanFuelBenefit)
    case BeneficialLoan.name => Some(BeneficialLoan)
    case Accommodation.name => Some(Accommodation)
    case Assets.name => Some(Assets)
    case AssetTransfer.name => Some(AssetTransfer)
    case EducationalServices.name => Some(EducationalServices)
    case Entertaining.name => Some(Entertaining)
    case Expenses.name => Some(Expenses)
    case Mileage.name => Some(Mileage)
    case NonQualifyingRelocationExpenses.name => Some(NonQualifyingRelocationExpenses)
    case NurseryPlaces.name => Some(NurseryPlaces)
    case OtherItems.name => Some(OtherItems)
    case PaymentsOnEmployeesBehalf.name => Some(PaymentsOnEmployeesBehalf)
    case PersonalIncidentalExpenses.name => Some(PersonalIncidentalExpenses)
    case QualifyingRelocationExpenses.name => Some(QualifyingRelocationExpenses)
    case EmployerProvidedProfessionalSubscription.name => Some(EmployerProvidedProfessionalSubscription)
    case IncomeTaxPaidButNotDeductedFromDirectorsRemuneration.name => Some(IncomeTaxPaidButNotDeductedFromDirectorsRemuneration)
    case TravelAndSubsistence.name => Some(TravelAndSubsistence)
    case VouchersAndCreditCards.name => Some(VouchersAndCreditCards)
    case NonCashBenefit.name => Some(NonCashBenefit)
    case EmployerProvidedServices.name => Some(EmployerProvidedServices)
    case _ => None
  }
}

case object NonCodedIncome extends NonTaxCodeIncomeComponentType with OtherIncomes
case object Commission extends NonTaxCodeIncomeComponentType with OtherIncomes
case object OtherIncomeEarned extends NonTaxCodeIncomeComponentType with OtherIncomes
case object OtherIncomeNotEarned extends NonTaxCodeIncomeComponentType with OtherIncomes
case object PartTimeEarnings extends NonTaxCodeIncomeComponentType with OtherIncomes
case object Tips extends NonTaxCodeIncomeComponentType with OtherIncomes
case object OtherEarnings extends NonTaxCodeIncomeComponentType with OtherIncomes
case object CasualEarnings extends NonTaxCodeIncomeComponentType with OtherIncomes
case object PersonalPensionAnnuity extends NonTaxCodeIncomeComponentType with OtherIncomes
case object ForeignPropertyIncome extends NonTaxCodeIncomeComponentType with OtherIncomes
case object Profit extends NonTaxCodeIncomeComponentType with OtherIncomes
case object OccupationalPension extends NonTaxCodeIncomeComponentType with EmploymentPensions
case object PublicServicesPension extends NonTaxCodeIncomeComponentType with EmploymentPensions
case object ForcesPension extends NonTaxCodeIncomeComponentType with EmploymentPensions
case object UntaxedInterestIncome extends NonTaxCodeIncomeComponentType
case object ForeignDividendIncome extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object ForeignInterestAndOtherSavings extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object ForeignPensionsAndOtherIncome extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object BankOrBuildingSocietyInterest extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object UkDividend extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object UnitTrust extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object StockDividend extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object NationalSavings extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object SavingsBond extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object PurchasedLifeAnnuities extends NonTaxCodeIncomeComponentType with SavingAndInvestments
case object StatePension extends NonTaxCodeIncomeComponentType with TaxableStateBenefits
case object EmploymentAndSupportAllowance extends NonTaxCodeIncomeComponentType with TaxableStateBenefits
case object JobSeekersAllowance extends NonTaxCodeIncomeComponentType with TaxableStateBenefits
case object IncapacityBenefit extends NonTaxCodeIncomeComponentType with TaxableStateBenefits

//Tax-code Incomes
case object EmploymentIncome extends TaxCodeIncomeComponentType
case object PensionIncome  extends TaxCodeIncomeComponentType
case object JobSeekerAllowanceIncome extends TaxCodeIncomeComponentType
case object OtherIncome extends TaxCodeIncomeComponentType

object TaxComponentType{
  implicit val formatTaxComponentType: Format[TaxComponentType] = new Format[TaxComponentType] {
    override def reads(json: JsValue): JsResult[TaxComponentType] =  json.as[String] match {
      case "EmploymentIncome" => JsSuccess(EmploymentIncome)
      case "PensionIncome" => JsSuccess(PensionIncome)
      case "JobSeekerAllowanceIncome" => JsSuccess(JobSeekerAllowanceIncome)
      case "OtherIncome" => JsSuccess(OtherIncome)
      case _ => JsError("Invalid Tax component type")
    }
    override def writes(taxComponentType: TaxComponentType) = JsString(taxComponentType.toString)
  }
}

object NonTaxCodeIncomeComponentType {
  implicit val formatTaxComponentType: Format[NonTaxCodeIncomeComponentType] = new Format[NonTaxCodeIncomeComponentType] {
    override def reads(json: JsValue): JsResult[NonTaxCodeIncomeComponentType] = json.as[String] match {
      case "NonCodedIncome" => JsSuccess(NonCodedIncome)
      case "Commission" => JsSuccess(Commission)
      case "OtherIncomeEarned" => JsSuccess(OtherIncomeEarned)
      case "OtherIncomeNotEarned" => JsSuccess(OtherIncomeNotEarned)
      case "PartTimeEarnings" => JsSuccess(PartTimeEarnings)
      case "Tips" => JsSuccess(Tips)
      case "OtherEarnings" => JsSuccess(OtherEarnings)
      case "CasualEarnings" => JsSuccess(CasualEarnings)
      case "ForeignDividendIncome" => JsSuccess(ForeignDividendIncome)
      case "ForeignPropertyIncome" => JsSuccess(ForeignPropertyIncome)
      case "ForeignInterestAndOtherSavings" => JsSuccess(ForeignInterestAndOtherSavings)
      case "ForeignPensionsAndOtherIncome" => JsSuccess(ForeignPensionsAndOtherIncome)
      case "StatePension" => JsSuccess(StatePension)
      case "OccupationalPension" => JsSuccess(OccupationalPension)
      case "PublicServicesPension" => JsSuccess(PublicServicesPension)
      case "ForcesPension" => JsSuccess(ForcesPension)
      case "PersonalPensionAnnuity" => JsSuccess(PersonalPensionAnnuity)
      case "Profit" => JsSuccess(Profit)
      case "BankOrBuildingSocietyInterest" => JsSuccess(BankOrBuildingSocietyInterest)
      case "UkDividend" => JsSuccess(UkDividend)
      case "UnitTrust" => JsSuccess(UnitTrust)
      case "StockDividend" => JsSuccess(StockDividend)
      case "NationalSavings" => JsSuccess(NationalSavings)
      case "SavingsBond" => JsSuccess(SavingsBond)
      case "PurchasedLifeAnnuities" => JsSuccess(PurchasedLifeAnnuities)
      case "UntaxedInterestIncome" => JsSuccess(UntaxedInterestIncome)
      case "IncapacityBenefit" => JsSuccess(IncapacityBenefit)
      case "JobSeekersAllowance" => JsSuccess(JobSeekersAllowance)
      case "EmploymentAndSupportAllowance" => JsSuccess(EmploymentAndSupportAllowance)
      case _ => JsError("Invalid Non tax code component type")
    }
    override def writes(nonTaxCodeIncomeComponentType: NonTaxCodeIncomeComponentType) = JsString(nonTaxCodeIncomeComponentType.toString)
  }
}
