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

package uk.gov.hmrc.tai.model.domain.benefits

import org.joda.time.LocalDate
import play.api.libs.json.{Format, JsSuccess, JsValue, Json}
import uk.gov.hmrc.tai.model.domain._

case class CompanyCar(carSeqNo: Int,
                      makeModel: String,
                      hasActiveFuelBenefit: Boolean,
                      dateMadeAvailable: Option[LocalDate],
                      dateActiveFuelBenefitMadeAvailable: Option[LocalDate],
                      dateWithdrawn: Option[LocalDate])

object CompanyCar {
  implicit val formats = Json.format[CompanyCar]
}

case class CompanyCarBenefit(employmentSeqNo: Int,
                             grossAmount: BigDecimal,
                             companyCars: Seq[CompanyCar],
                             version: Option[Int] = None)

object CompanyCarBenefit {
  implicit val formats = Json.format[CompanyCarBenefit]
}

case class GenericBenefit(benefitType: BenefitComponentType, employmentId: Option[Int], amount: BigDecimal)

object GenericBenefit {
  implicit val format = new Format[GenericBenefit] {
    override def reads(json: JsValue): JsSuccess[GenericBenefit] = {
      val benefitTypeKey = (json \ "benefitType").as[String]
      val employmentId = (json \ "employmentId").asOpt[Int]
      val amount = (json \ "amount").as[BigDecimal]
      val benefitType = benefitComponentTypeMap.getOrElse(benefitTypeKey, throw new RuntimeException("Not able to parse benefit components"))
      JsSuccess(GenericBenefit(benefitType, employmentId, amount))
    }

    override def writes(adjustmentType: GenericBenefit) = ???
  }

  private val benefitComponentTypeMap: Map[String, BenefitComponentType] = Map(
    "BenefitInKind" -> BenefitInKind,
    "CarFuelBenefit" -> CarFuelBenefit,
    "MedicalInsurance" -> MedicalInsurance,
    "CarBenefit" -> CarBenefit,
    "Telephone" -> Telephone,
    "ServiceBenefit" -> ServiceBenefit,
    "TaxableExpensesBenefit" -> TaxableExpensesBenefit,
    "VanBenefit" -> VanBenefit,
    "VanFuelBenefit" -> VanFuelBenefit,
    "BeneficialLoan" -> BeneficialLoan,
    "Accommodation" -> Accommodation,
    "Assets" -> Assets,
    "AssetTransfer" -> AssetTransfer,
    "EducationalServices" -> EducationalServices,
    "Entertaining" -> Entertaining,
    "Expenses" -> Expenses,
    "Mileage" -> Mileage,
    "NonQualifyingRelocationExpenses" -> NonQualifyingRelocationExpenses,
    "NurseryPlaces" -> NurseryPlaces,
    "OtherItems" -> OtherItems,
    "PaymentsOnEmployeesBehalf" -> PaymentsOnEmployeesBehalf,
    "PersonalIncidentalExpenses" -> PersonalIncidentalExpenses,
    "QualifyingRelocationExpenses" -> QualifyingRelocationExpenses,
    "EmployerProvidedProfessionalSubscription" -> EmployerProvidedProfessionalSubscription,
    "IncomeTaxPaidButNotDeductedFromDirectorsRemuneration" -> IncomeTaxPaidButNotDeductedFromDirectorsRemuneration,
    "TravelAndSubsistence" -> TravelAndSubsistence,
    "VouchersAndCreditCards" -> VouchersAndCreditCards,
    "NonCashBenefit" -> NonCashBenefit,
    "EmployerProvidedServices" -> EmployerProvidedServices
  )
}

case class Benefits(companyCarBenefits: Seq[CompanyCarBenefit], otherBenefits: Seq[GenericBenefit])

object Benefits {
  implicit val formats = Json.format[Benefits]
}

case class WithdrawCarAndFuel(version: Int, carWithdrawDate: LocalDate, fuelWithdrawDate: Option[LocalDate])

object WithdrawCarAndFuel {
  implicit val formats = Json.format[WithdrawCarAndFuel]
}

case class EndedCompanyBenefit(benefitType: String,
                               whatYouToldUs: String,
                               stopDate: String,
                               valueOfBenefit: Option[String],
                               contactByPhone: String,
                               phoneNumber: Option[String])

object EndedCompanyBenefit{
  implicit val formats: Format[EndedCompanyBenefit] = Json.format[EndedCompanyBenefit]
}