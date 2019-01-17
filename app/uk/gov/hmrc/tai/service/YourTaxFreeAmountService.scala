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

package uk.gov.hmrc.tai.service

import com.google.inject.Inject
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.util.yourTaxFreeAmount._
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class YourTaxFreeAmountService @Inject()(employmentService: EmploymentService,
                                         taxCodeChangeService: TaxCodeChangeService,
                                         codingComponentService: CodingComponentService,
                                         companyCarService: CompanyCarService) extends YourTaxFreeAmount {

  def taxFreeAmountComparison(nino: Nino)(implicit hc: HeaderCarrier, messages: Messages): Future[YourTaxFreeAmountViewModel] = {

    val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())
    val taxCodeChangeFuture = taxCodeChangeService.taxCodeChange(nino)
    val taxFreeAmountComparisonFuture = codingComponentService.taxFreeAmountComparison(nino)

    for {
      employmentNames <- employmentNameFuture
      taxCodeChange <- taxCodeChangeFuture
      taxFreeAmountComparison <- taxFreeAmountComparisonFuture
      currentCompanyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, taxFreeAmountComparison.current)
      previousCompanyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, taxFreeAmountComparison.previous)
    } yield {
      val yourTaxFreeAmount = buildTaxFreeAmount(
        Some(CodingComponentsWithCarBenefits(
          taxCodeChange.mostRecentPreviousTaxCodeChangeDate,
          taxFreeAmountComparison.previous,
          previousCompanyCarBenefits
        )),
        CodingComponentsWithCarBenefits(
          taxCodeChange.mostRecentTaxCodeChangeDate,
          taxFreeAmountComparison.current,
          currentCompanyCarBenefits
        )
      )

      val describedPairs = describeIabdPairs(yourTaxFreeAmount.iabdPairs, currentCompanyCarBenefits, previousCompanyCarBenefits, employmentNames)

      YourTaxFreeAmountViewModel(
        yourTaxFreeAmount.previousTaxFreeInfo,
        yourTaxFreeAmount.currentTaxFreeInfo,
        describedPairs.allowances,
        describedPairs.deductions
      )
    }
  }

  def taxFreeAmountComparison2(nino: Nino)(implicit hc: HeaderCarrier, messages: Messages): Future[YourTaxFreeAmountComparison] = {

    // TODO: Now we are moving the describe part to DescribedYourTaxFreeAmount we can move the company car and employment calls to there
    // So build will be called with just CodingComponents ( i think )
    val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())
    val taxCodeChangeFuture = taxCodeChangeService.taxCodeChange(nino)
    val taxFreeAmountComparisonFuture = codingComponentService.taxFreeAmountComparison(nino)

    for {
      employmentNames <- employmentNameFuture
      taxCodeChange <- taxCodeChangeFuture
      taxFreeAmountComparison <- taxFreeAmountComparisonFuture
      currentCompanyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, taxFreeAmountComparison.current)
      previousCompanyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, taxFreeAmountComparison.previous)
    } yield {
      buildTaxFreeAmount(
        Some(CodingComponentsWithCarBenefits(
          taxCodeChange.mostRecentPreviousTaxCodeChangeDate,
          taxFreeAmountComparison.previous,
          previousCompanyCarBenefits
        )),
        CodingComponentsWithCarBenefits(
          taxCodeChange.mostRecentTaxCodeChangeDate,
          taxFreeAmountComparison.current,
          currentCompanyCarBenefits
        )
      )
    }
  }

  def taxFreeAmount(nino: Nino)(implicit hc: HeaderCarrier, messages: Messages): Future[YourTaxFreeAmountViewModel] = {

    val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())
    val taxCodeChangeFuture = taxCodeChangeService.taxCodeChange(nino)
    val codingComponentsFuture = codingComponentService.taxFreeAmountComponents(nino, TaxYear())

    for {
      employmentNames <- employmentNameFuture
      taxCodeChange <- taxCodeChangeFuture
      currentCodingComponents <- codingComponentsFuture
      currentCompanyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, currentCodingComponents)
    } yield {
      val yourTaxFreeAmount = buildTaxFreeAmount(
        None,
        CodingComponentsWithCarBenefits(
          taxCodeChange.mostRecentTaxCodeChangeDate,
          currentCodingComponents,
          currentCompanyCarBenefits
        )
      )

      val describedPairs = describeIabdPairs(yourTaxFreeAmount.iabdPairs, currentCompanyCarBenefits, Seq.empty, employmentNames)

      YourTaxFreeAmountViewModel(
        yourTaxFreeAmount.previousTaxFreeInfo,
        yourTaxFreeAmount.currentTaxFreeInfo,
        describedPairs.allowances,
        describedPairs.deductions
      )
    }
  }

  case class describedIabdPairs(allowances: Seq[CodingComponentPairDescription], deductions: Seq[CodingComponentPairDescription])

  private def describeIabdPairs(allowancesAndDeductions: AllowancesAndDeductionPairs,
                                currentCompanyCarBenefit: Seq[CompanyCarBenefit],
                                previousCompanyCarBenefit: Seq[CompanyCarBenefit],
                                employmentIds: Map[Int, String])
                               (implicit hc: HeaderCarrier, messages: Messages) = {

    val allowancesDescription = for (
      allowance <- allowancesAndDeductions.allowances
    ) yield CodingComponentPairDescription(allowance, employmentIds, previousCompanyCarBenefit ++ currentCompanyCarBenefit)

    val deductionsDescription = for (
      deduction <- allowancesAndDeductions.deductions
    ) yield CodingComponentPairDescription(deduction, employmentIds, previousCompanyCarBenefit ++ currentCompanyCarBenefit)

    describedIabdPairs(allowancesDescription, deductionsDescription)
  }
}
