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

import com.google.inject.Inject
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.{CodingComponentPairModel, TaxYear}
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{EmploymentService, YourTaxFreeAmountComparison, YourTaxFreeAmountService}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount._
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DescribedYourTaxFreeAmountService @Inject()(yourTaxFreeAmountService: YourTaxFreeAmountService,
                                                  companyCarService: CompanyCarService,
                                                  employmentService: EmploymentService) {

  def taxFreeAmountComparison(nino: Nino)(implicit hc: HeaderCarrier, messages: Messages): Future[YourTaxFreeAmountViewModel] = {
    taxFreeAmount(nino, yourTaxFreeAmountService.taxFreeAmountComparison)
  }


  private def taxFreeAmount(nino: Nino, getTaxFreeAmount: Nino => Future[YourTaxFreeAmountComparison])(implicit hc: HeaderCarrier, messages: Messages): Future[YourTaxFreeAmountViewModel] = {
    val taxFreeAmountComparisonFuture: Future[YourTaxFreeAmountComparison] = getTaxFreeAmount(nino)
    val companyCarFuture = companyCarService.companyCars(nino)
    val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())

    for {
      employmentNames <- employmentNameFuture
      taxFreeAmountComparison <- taxFreeAmountComparisonFuture
      companyCarBenefit <- companyCarFuture
    } yield {

      val describedPairs = describeIabdPairs(taxFreeAmountComparison.iabdPairs, companyCarBenefit, employmentNames)

      YourTaxFreeAmountViewModel(
        taxFreeAmountComparison.previousTaxFreeInfo,
        taxFreeAmountComparison.currentTaxFreeInfo,
        describedPairs.allowances,
        describedPairs.deductions
      )
    }
  }

  case class describedIabdPairs(allowances: Seq[CodingComponentPairModel], deductions: Seq[CodingComponentPairModel])

  private def describeIabdPairs(allowancesAndDeductions: AllowancesAndDeductionPairs,
                                companyCarBenefit: Seq[CompanyCarBenefit],
                                employmentIds: Map[Int, String])
                               (implicit hc: HeaderCarrier, messages: Messages) = {

    val allowancesDescription = for (
      allowance <- allowancesAndDeductions.allowances
    ) yield CodingComponentPairModel(allowance, employmentIds, companyCarBenefit)

    val deductionsDescription = for (
      deduction <- allowancesAndDeductions.deductions
    ) yield CodingComponentPairModel(deduction, employmentIds, companyCarBenefit)

    describedIabdPairs(allowancesDescription, deductionsDescription)
  }
}
