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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import uk.gov.hmrc.tai.model.domain._

trait IabdMessageGroups {
  val youHaveClaimedBenefits: Seq[TaxComponentType] = {
    Seq(
      JobExpenses,
      FlatRateJobExpenses,
      ProfessionalSubscriptions,
      HotelAndMealExpenses,
      VehicleExpenses,
      MileageAllowanceRelief
    )
  }

  val youNowGetBenefits: Seq[TaxComponentType] = {
    Seq(
      BlindPersonsAllowance,
      GiftAidPayments,
      GiftAidAdjustment,
      Commission,
      BeneficialLoan,
      CarBenefit,
      CarFuelBenefit,
      MedicalInsurance,
      VanBenefit,
      VanFuelBenefit,
      EmployerProvidedProfessionalSubscription,
      QualifyingRelocationExpenses,
      TravelAndSubsistence,
      VouchersAndCreditCards,
      StatePension,
      OccupationalPension,
      PublicServicesPension,
      ForcesPension,
      PersonalPensionAnnuity
    )
  }

  val haveBeenAllowances: Seq[TaxComponentType] = {
    Seq(
      JobExpenses,
      FlatRateJobExpenses,
      ProfessionalSubscriptions,
      HotelAndMealExpenses,
      VehicleExpenses
    )
  }

  val hasBeenAllowances: Seq[TaxComponentType] = {
    Seq(
      BlindPersonsAllowance,
      GiftAidPayments,
      GiftAidAdjustment,
      MileageAllowanceRelief,
      Commission,
      BeneficialLoan,
      CarBenefit,
      CarFuelBenefit,
      MedicalInsurance,
      VanBenefit,
      VanFuelBenefit,
      EmployerProvidedProfessionalSubscription,
      QualifyingRelocationExpenses,
      TravelAndSubsistence,
      VouchersAndCreditCards,
      StatePension,
      OccupationalPension,
      PublicServicesPension,
      ForcesPension,
      PersonalPensionAnnuity
    )
  }
}
