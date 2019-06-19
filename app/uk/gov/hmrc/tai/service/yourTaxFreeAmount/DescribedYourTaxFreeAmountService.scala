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

package uk.gov.hmrc.tai.service.yourTaxFreeAmount

import javax.inject.Inject
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.{CodingComponentPairModel, TaxFreeAmountDetails, TaxYear}
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{EmploymentService, TaxAccountService, YourTaxFreeAmountComparison, YourTaxFreeAmountService}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount._
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DescribedYourTaxFreeAmountService @Inject()(yourTaxFreeAmountService: YourTaxFreeAmountService,
                                                  companyCarService: CompanyCarService,
                                                  employmentService: EmploymentService,
                                                  taxAccountService: TaxAccountService) {

  def taxFreeAmountComparison(nino: Nino)(implicit hc: HeaderCarrier, messages: Messages): Future[YourTaxFreeAmountViewModel] = {
    taxFreeAmount(nino, yourTaxFreeAmountService.taxFreeAmountComparison)
  }


  private def taxFreeAmount(nino: Nino, getTaxFreeAmount: Nino => Future[YourTaxFreeAmountComparison])(implicit hc: HeaderCarrier, messages: Messages): Future[YourTaxFreeAmountViewModel] = {
    val taxFreeAmountComparisonFuture: Future[YourTaxFreeAmountComparison] = getTaxFreeAmount(nino)
    val companyCarFuture = companyCarService.companyCars(nino)
    val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())
    val totalTaxFuture = taxAccountService.totalTax(nino, TaxYear())
    for {
      employmentNames <- employmentNameFuture
      taxFreeAmountComparison <- taxFreeAmountComparisonFuture
      companyCarBenefit <- companyCarFuture
      totalTax <- totalTaxFuture
    } yield {
      totalTax match {
        case TaiSuccessResponseWithPayload(totalTax: TotalTax) =>
          val describedPairs = describeIabdPairs(taxFreeAmountComparison.iabdPairs, companyCarBenefit, employmentNames, totalTax)

          YourTaxFreeAmountViewModel(
            taxFreeAmountComparison.previousTaxFreeInfo,
            taxFreeAmountComparison.currentTaxFreeInfo,
            describedPairs.allowances,
            describedPairs.deductions
          )
        case _ => throw new RuntimeException("Failed to fetch total tax details")
      }
    }
  }

  case class describedIabdPairs(allowances: Seq[CodingComponentPairModel], deductions: Seq[CodingComponentPairModel])

  private def describeIabdPairs(allowancesAndDeductions: AllowancesAndDeductionPairs,
                                companyCarBenefit: Seq[CompanyCarBenefit],
                                employmentIds: Map[Int, String],
                                totalTax: TotalTax)
                               (implicit hc: HeaderCarrier, messages: Messages) = {

    val allowancesDescription = for (
      allowance <- allowancesAndDeductions.allowances
    ) yield CodingComponentPairModel(allowance, TaxFreeAmountDetails(employmentIds, companyCarBenefit, totalTax))

    val deductionsDescription = for (
      deduction <- allowancesAndDeductions.deductions
    ) yield CodingComponentPairModel(deduction, TaxFreeAmountDetails(employmentIds, companyCarBenefit, totalTax))

    describedIabdPairs(allowancesDescription, deductionsDescription)
  }
}
