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

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.{TaxAccountConnector, TaxFreeAmountComparisonConnector}
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.tai.model.domain.TaxFreeAmountComparison

class CodingComponentService @Inject()(taxAccountConnector: TaxAccountConnector, taxFreeAmountComparisonConnector: TaxFreeAmountComparisonConnector) {

  def taxFreeAmountComponents(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[CodingComponent]] = {
    taxAccountConnector.codingComponents(nino, year) map {
      case TaiSuccessResponseWithPayload(codingComponents: Seq[CodingComponent]) => filterOutZeroAmountsComponents(codingComponents)
      case TaiTaxAccountFailureResponse(e) => throw new RuntimeException(e)
      case _ => throw new RuntimeException("could not fetch coding components")
    }
  }

  def taxFreeAmountComparison(nino: Nino)(implicit hc: HeaderCarrier): Future[TaxFreeAmountComparison] = {
    taxFreeAmountComparisonConnector.taxFreeAmountComparison(nino) map {
      case TaiSuccessResponseWithPayload(taxFreeAmountComparison: TaxFreeAmountComparison) => filterOutZeroAmountsComponents(taxFreeAmountComparison)
      case TaiTaxAccountFailureResponse(e) => throw new RuntimeException(e)
      case _ => throw new RuntimeException("Could not fetch tax free amount comparison")
    }
  }

  private def filterOutZeroAmountsComponents(taxFreeAmountComparison: TaxFreeAmountComparison): TaxFreeAmountComparison = {
    TaxFreeAmountComparison(
      filterOutZeroAmountsComponents(taxFreeAmountComparison.previous),
      filterOutZeroAmountsComponents(taxFreeAmountComparison.current)
    )
  }

  private def filterOutZeroAmountsComponents(codingComponents: Seq[CodingComponent]): Seq[CodingComponent] = {
    codingComponents.filter {
      case component if component.amount == 0 => false
      case _ => true
    }
  }
}
