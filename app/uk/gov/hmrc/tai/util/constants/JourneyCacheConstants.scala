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

package uk.gov.hmrc.tai.util.constants

trait JourneyCacheConstants {

  val AddEmployment_JourneyKey = "add-employment"
  val AddEmployment_NameKey = "employmentName"
  val AddEmployment_StartDateKey = "employmentStartDate"
  val AddEmployment_RecewivedFirstPayKey = "employmentFirstPayReceived"
  val AddEmployment_PayrollNumberQuestionKey = "employmentPayrollNumberKnown"
  val AddEmployment_PayrollNumberKey = "employmentPayrollNumber"
  val AddEmployment_TelephoneQuestionKey = "employmentTelephoneContactAllowed"
  val AddEmployment_TelephoneNumberKey = "employmentTelephoneNumber"
  val AddEmployment_StartDateWithinSixWeeks = "employmentStartDateWithinSixWeeks"

  val UpdateEmployment_JourneyKey = "update-employment"
  val UpdateEmployment_EmploymentIdKey = "employmentId"
  val UpdateEmployment_NameKey = "employmentName"
  val UpdateEmployment_EmploymentDetailsKey = "employmentDetails"
  val UpdateEmployment_TelephoneQuestionKey = "employmentTelephoneContactAllowed"
  val UpdateEmployment_TelephoneNumberKey = "employmentTelephoneNumber"

  val EndEmployment_JourneyKey = "end-employment"
  val EndEmployment_EmploymentIdKey = "employmentId"
  val EndEmployment_NameKey = "employmentName"
  val EndEmployment_EndDateKey = "employmentEndDate"
  val EndEmployment_TelephoneQuestionKey = "employmentTelephoneQuestion"
  val EndEmployment_TelephoneNumberKey = "employmentTelephoneNumber"
  val EndEmployment_LatestPaymentDateKey = "employmentLatestPaymentDate"

  val AddPensionProvider_JourneyKey = "add-pension-provider"
  val AddPensionProvider_NameKey = "pensionProviderName"
  val AddPensionProvider_FirstPaymentKey = "pensionFirstPayment"
  val AddPensionProvider_StartDateKey = "pensionProviderStartDate"
  val AddPensionProvider_PayrollNumberChoice = "pensionProviderPayrollChoice"
  val AddPensionProvider_PayrollNumberKey = "pensionProviderPayrollNumber"
  val AddPensionProvider_TelephoneQuestionKey = "pensionProviderTelephoneContactAllowed"
  val AddPensionProvider_TelephoneNumberKey = "pensionProviderTelephoneNumber"
  val AddPensionProvider_StartDateWithinSixWeeks = "pensionProviderStartDateWithinSixWeeks"

  val UpdatePensionProvider_JourneyKey = "update-pension-provider"
  val UpdatePensionProvider_NameKey = "pensionProviderName"
  val UpdatePensionProvider_IdKey = "pensionProviderId"
  val UpdatePensionProvider_ReceivePensionQuestionKey = "receivePension"
  val UpdatePensionProvider_TelephoneQuestionKey = "telephoneContactAllowed"
  val UpdatePensionProvider_TelephoneNumberKey = "telephoneNumber"
  val UpdatePensionProvider_DetailsKey = "pensionDetails"

  val CompanyCar_JourneyKey = "company-car"
  val CompanyCar_EmployerIdKey = "employerId"
  val CompanyCar_Version = "version"
  val CompanyCar_CarModelKey = "carModel"
  val CompanyCar_CarProviderKey = "carProvider"
  val CompanyCar_CarSeqNoKey = "carSeqNo"
  val CompanyCar_DateGivenBackKey = "carGivenBackDate"
  val CompanyCar_DateFuelBenefitStoppedKey = "carFuelBenefitStoppedDate"
  val CompanyCar_DateStartedKey = "carStartDate"
  val CompanyCar_DateFuelBenefitStartedKey = "carFuelStartDate"
  val CompanyCar_HasActiveFuelBenefitdKey = "hasActiveFuelBenefit"
  val CompanyCar_DateWithdrawnKey = "dateWithdrawn"


  val CloseBankAccountJourneyKey = "close-bank-account"
  val CloseBankAccountDateKey = "closeBankAccountDate"
  val CloseBankAccountInterestKey = "closeBankAccountInterest"
  val CloseBankAccountInterestChoice = "closeBankAccountInterestChoice"
  val CloseBankAccountNameKey = "bankName"

  val UpdateBankAccountJourneyKey="update-bank-account"
  val UpdateBankAccountInterestKey="updateInterest"
  val UpdateBankAccountNameKey="updateBankName"

  val TrackSuccessfulJourney_JourneyKey = "successful-journey"
  val TrackSuccessfulJourney_AddEmploymentKey = "addEmployment"
  val TrackSuccessfulJourney_EndEmploymentKey = "endEmployment"
  val TrackSuccessfulJourney_UpdateEmploymentKey = "updateEmployment"
  val TrackSuccessfulJourney_UpdatePensionKey = "updatePensionProvider"
  val TrackSuccessfulJourney_UpdatePreviousYearsIncomeKey = "updatePreviousYearsIncome"
  val TrackSuccessfulJourney_AddPensionProviderKey = "addPensionProvider"
  val TrackSuccessfulJourney_EndEmploymentBenefitKey = "endEmploymentBenefit"

  val UpdatePreviousYearsIncome_JourneyKey = "update-previous-years-income"
  val UpdatePreviousYearsIncome_TaxYearKey = "taxYear"
  val UpdatePreviousYearsIncome_IncomeDetailsKey = "incomeDetails"
  val UpdatePreviousYearsIncome_TelephoneQuestionKey = "updateIncomeTelephoneContactAllowed"
  val UpdatePreviousYearsIncome_TelephoneNumberKey = "updateIncomeTelephoneNumber"

  val UpdateIncome_JourneyKey = "update-income"
  val UpdateIncome_NameKey = "updateIncomeEmploymentName"
  val UpdateIncome_IdKey = "updateIncomeEmploymentIdKey"
  val UpdateIncome_PayToDateKey = "updateIncomePayToDateKey"
  val UpdateIncome_DateKey = "updateIncomeDateKey"
  val UpdateIncome_NewAmountKey = "updateIncomeNewAmountKey"
  val UpdateIncome_PayPeriodKey = "updateIncomePayPeriodKey"
  val UpdateIncome_OtherInDaysKey = "updateIncomeOtherInDaysKey"
  val UpdateIncome_TotalSalaryKey = "updateIncomeTotalSalaryKey"
  val UpdateIncome_TaxablePayKey = "updateIncomeTaxablePayKey"
  val UpdateIncome_PayslipDeductionsKey = "updateIncomePayslipDeductionsKey"
  val UpdateIncome_BonusPaymentsKey = "updateIncomeBonusPaymentsKey"
  val UpdateIncome_BonusOvertimeAmountKey = "updateIncomeBonusOvertimeAmountKey"
  val UpdateIncome_GrossAnnualPayKey = "updateIncomeGrossAnnualPayKey"
  val UpdateIncome_IncomeTypeKey = "updateIncomeIncomeTypeKey"
  val UpdateIncome_IrregularAnnualPayKey = "updateIncomeIrregularAnnualPayKey"

  val EndCompanyBenefit_JourneyKey = "end-company-benefit"
  val EndCompanyBenefit_EmploymentIdKey = "employmentId"
  val EndCompanyBenefit_EmploymentNameKey = "employmentName"
  val EndCompanyBenefit_BenefitTypeKey = "benefitType"
  val EndCompanyBenefit_BenefitStopDateKey = "stopDate"
  val EndCompanyBenefit_BenefitValueKey = "benefitValue"
  val EndCompanyBenefit_TelephoneQuestionKey = "telephoneContactAllowed"
  val EndCompanyBenefit_TelephoneNumberKey = "telephoneNumber"
  val EndCompanyBenefit_BenefitNameKey = "benefitName"
  val EndCompanyBenefit_RefererKey = "referer"
}
