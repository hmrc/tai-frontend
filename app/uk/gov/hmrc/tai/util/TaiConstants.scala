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

package uk.gov.hmrc.tai.util

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
  val UpdateIncome_BonusPaymentsThisYearKey = "updateIncomeBonusPaymentsThisYearKey"
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

trait AuditConstants {
  val AddEmployment_CantAddEmployer = "cantAddEmployer"
  val EndEmployment_WithinSixWeeksError = "sixWeeksErrorEmployment"
  val EndEmployment_IrregularPayment = "irregularPaidEmployment"
  val PotentialUnderpayment_InYearAdjustment = "inYearAdjustment"
  val TaxAccountSummary_UserEntersSummaryPage = "userEntersSummaryPage"
  val AddPension_CantAddPensionProvider = "cantAddPensionProvider"
}

trait AddEmploymentPayrollNumberConstants {
  val PayrollNumberChoice = "payrollNumberChoice"
  val PayrollNumberEntry = "payrollNumberEntry"
}

trait AddPensionNumberConstants {
  val PayrollNumberChoice = "payrollNumberChoice"
  val PayrollNumberEntry = "payrollNumberEntry"
}

trait AddEmploymentFirstPayChoiceConstants {
  val FirstPayChoice: String = "firstPayChoice"
}

trait AddPensionFirstPayChoiceConstants {
  val FirstPayChoice: String = "firstPayChoice"
}

trait EmploymentDecisionConstants {
  val EmploymentDecision = "employmentDecision"
}

trait IncorrectPensionDecisionConstants {
  val IncorrectPensionDecision = "incorrectPensionDecision"
}

trait IrregularPayConstants {
  val IrregularPayDecision = "irregularPayDecision"
  val ContactEmployer = "contactEmployer"
  val UpdateDetails = "updateDetails"
}

trait FormValuesConstants {
  val YesValue = "Yes"
  val NoValue = "No"
  val YesNoChoice = "yesNoChoice"
  val YesNoTextEntry = "yesNoTextEntry"
}

trait BankAccountDecisionConstants {
  val BankAccountDecision = "bankAccountDecision"
  val UpdateInterest = "updateInterest"
  val CloseAccount = "closeAccount"
  val RemoveAccount = "removeAccount"

  val UpdateBankAccountChoiceJourneyKey = "update-or-remove-bank-account-decision"
  val UpdateBankAccountUserChoiceKey = "userAccountActionDecision"
}

trait BankAccountClosingInterestConstants {
  val ClosingInterestChoice = "closingInterestChoice"
  val ClosingInterestEntry = "closingInterestEntry"
}

trait UpdateHistoricIncomeChoiceConstants {
  val UpdateIncomeChoice = "updateIncomeChoice"
}

trait BandTypesConstants {
  val UkBands = "uk.bandtype"
  val ScottishBands = "scottish.bandtype"
  val TaxFreeAllowanceBand = "pa"
  val StarterSavingsRate = "SR"
  val PersonalSavingsRate = "PSR"
  val SavingsBasicRate = "LSR"
  val SavingsHigherRate = "HSR1"
  val SavingsAdditionalRate = "HSR2"
  val DividendZeroRate = "SDR"
  val DividendBasicRate = "LDR"
  val DividendHigherRate = "HDR1"
  val DividendAdditionalRate = "HDR2"
  val ZeroBand = "ZeroBand"
  val NonZeroBand = "NonZeroBand"
  val TaxGraph = "taxGraph"
  val TaxFree = "TaxFree"
  val BasicRate = "B"
}

trait TaxRegionConstants {
  val UkTaxRegion = "UK"
  val ScottishTaxRegion = "SCOTTISH"
}

trait UpdateOrRemoveCompanyBenefitDecisionConstants{
  val DecisionChoice = "decisionChoice"
  val YesIGetThisBenefit = "yesIGetThisBenefit"
  val NoIDontGetThisBenefit = "noIDontGetThisBenefit"
}

trait RemoveCompanyBenefitStopDateConstants{
  val StopDateChoice = "stopDateChoice"
  val BeforeTaxYearEnd = "beforeTaxYearEnd"
  val OnOrAfterTaxYearEnd = "onOrAfterTaxYearEnd"
}

object TaxRegionConstants extends TaxRegionConstants

object GoogleAnalyticsConstants {
  val taxCodeChangeEdgeCase = "taxCodeChangeEdgeCase"
  val yes = "Yes"
  val no = "No"
  val valueOfIycdcPayment = "valueOfIycdcPayment"
  val iycdcReconciliationStatus = "iycdcReconciliationStatus"
  val currentYear = "Current Year"
  val nextYear = "Next Year"
  val currentAndNextYear = "Current and Next Year"
}

object TaiConstants {

  val SERVICE_IDENTIFIER = "TES"

  val incomeTaxPage = 1
  val incomePage = 2
  val taxFreeAmountPage = 3
  val taxCodePage = 4
  val maxCompareTaxCodes = 4
  val maxTaxCodes = 4
  val maxVisibleTaxCodes = 3
  val nextTaxYearDetails = 1
  val currentTaxYearDetails = 2
  val lastTaxYearDetails = 3
  val claimATaxRefund  = 4

  val notApplicable = "Not applicable"

  val IABD_TYPE_UKDIVIDENDS = 76
  val DividendZeroBandType = "SDR"

  val MCI_GATEKEEPER_TYPE = 6
  val MCI_GATEKEEPER_ID = 6
  val MCI_GATEKEEPER_DESCR = "Manual Correspondence Indicator"

  val defaultPrimaryPay = 15000
  val defaultSecondaryPay = 5000

  val TOTAL_TAX_DELTA = "TotalTaxDelta"
  val TAXABLE_PAY_DELTA = "TaxablePayDelta"
  val EMPEE_CONTRIBNS_DELTA = "EmpeeContribnsDelta"
  val EYU_DATE_FORMAT = "dd/MM/yyyy"

  val CEASED_MINUS_ONE = "CY-1"
  val CEASED_MINUS_TWO = "CY-2"
  val CeasedMinusThree = "CY-3"

  val EmploymentLive = 1
  val EmploymentPotentiallyCeased = 2
  val EmploymentCeased = 3

  val AuthProviderGG = "GovernmentGateway"
  val AuthProviderVerify = "Verify"

  val SessionPostLogoutPage = "postLogoutPage"

  val TaxableIncomeCurrentYearPage = 1
  val TaxableIncomeCurrentYearMinusOnePage = 2

  val Origin = "origin"
  val ConfidenceLevel = "confidenceLevel"
  val CompletionUrl = "completionURL"
  val FailureUrl = "failureURL"

  val Continue = "continue"
  val Failure = "failure"

  val NinoLength = 9
  val NinoWithoutSuffixLength = NinoLength-1
  val TaxAmountFactor = 10
  val EmergencyTaxCode = "X"

  val IncomeTypeDummy = "99"
  val IncomeTypeEmployment = "0"
  val IncomeTypePension = "1"

  val EmployeePensionIForm = "employment-pension"
  val CompanyBenefitsIform = "company-benefits"
  val CompanyCarsIform = "company-cars"
  val MedicalBenefitsIform = "medical-benefits"
  val OtherIncomeIform = "other-income"
  val InvestIncomeIform = "invest-income"
  val StateBenefitsIform = "state-benefits"
  val MarriageAllowanceService= "marriage-allowance"

  val NpsTaxAccountDeceasedMsg = "deceased"
  val NpsTaxAccountCYDataAbsentMsg = "no tax account information found"
  val NpsTaxAccountDataAbsentMsg = "cannot complete a coding calculation without a primary employment"
  val RtiPaymentDataAbsentMsg = "no data"
  val NpsNoEmploymentsRecorded = "no employments recorded for this individual"
  val NpsNoEmploymentForCurrentTaxYear = "no employments recorded for current tax year"
  val NpsAppStatusMsg = "appstatusmessage"

  val EmergencyTaxCodeSuffix = "X"
  val ScottishTaxCodePrefix = "S"

  val encodedMinusSign = "\u2212"

  val CurrentTaxYear = "currentTaxYear"
  val CurrentTaxYearPlusOne = "currentTaxYearPlusOne"

  val HigherRateBandIncome = 150000
}
