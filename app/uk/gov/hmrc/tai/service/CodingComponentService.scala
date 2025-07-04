/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.EitherT
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.{TaxAccountConnector, TaxFreeAmountComparisonConnector}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxFreeAmountComparison
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CodingComponentService @Inject() (
  taxAccountConnector: TaxAccountConnector,
  taxFreeAmountComparisonConnector: TaxFreeAmountComparisonConnector,
  implicit val executionContext: ExecutionContext
) {

  def taxFreeAmountComponents(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): Future[Seq[CodingComponent]] =
    taxAccountConnector.codingComponents(nino, year).map(filterOutZeroAmountsComponents)

  def taxFreeAmountComparison(
    nino: Nino
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, TaxFreeAmountComparison] =
    taxFreeAmountComparisonConnector.taxFreeAmountComparison(nino).map(filterOutZeroAmountsComponents)

  private def filterOutZeroAmountsComponents(
    taxFreeAmountComparison: TaxFreeAmountComparison
  ): TaxFreeAmountComparison =
    TaxFreeAmountComparison(
      filterOutZeroAmountsComponents(taxFreeAmountComparison.previous),
      filterOutZeroAmountsComponents(taxFreeAmountComparison.current)
    )

  private def filterOutZeroAmountsComponents(codingComponents: Seq[CodingComponent]): Seq[CodingComponent] =
    codingComponents.filter {
      case component if component.amount == 0 => false
      case _                                  => true
    }
}
