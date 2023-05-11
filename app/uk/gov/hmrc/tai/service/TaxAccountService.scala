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

import akka.Done
import cats.data.EitherT
import cats.implicits._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaxAccountConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome, TaxCodeIncomeSourceStatus}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.domain.{TaxAccountSummary, TaxCodeIncomeComponentType, TaxedIncome}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxAccountService @Inject()(taxAccountConnector: TaxAccountConnector) {

  def incomeSources(
    nino: Nino,
    year: TaxYear,
    incomeType: TaxCodeIncomeComponentType,
    status: TaxCodeIncomeSourceStatus)(implicit hc: HeaderCarrier): Future[Seq[TaxedIncome]] =
    taxAccountConnector.incomeSources(nino, year, incomeType, status)

  def taxCodeIncomes(nino: Nino, year: TaxYear)(
    implicit hc: HeaderCarrier): Future[Either[String, Seq[TaxCodeIncome]]] =
    taxAccountConnector.taxCodeIncomes(nino, year)

  def taxCodeIncomeForEmployment(nino: Nino, year: TaxYear, employmentId: Int)(
    implicit hc: HeaderCarrier,
    executionContext: ExecutionContext): Future[Either[String, Option[TaxCodeIncome]]] =
    EitherT(taxAccountConnector.taxCodeIncomes(nino, year)).map(_.find(_.employmentId.contains(employmentId))).value

  def taxAccountSummary(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaxAccountSummary] =
    taxAccountConnector.taxAccountSummary(nino, year)

  def nonTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[NonTaxCodeIncome] =
    taxAccountConnector.nonTaxCodeIncomes(nino, year)

  def updateEstimatedIncome(nino: Nino, newAmount: Int, year: TaxYear, id: Int)(
    implicit hc: HeaderCarrier): Future[Done] =
    taxAccountConnector.updateEstimatedIncome(nino, year, newAmount, id)

  def totalTax(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TotalTax] =
    taxAccountConnector.totalTax(nino, year)

  def scottishBandRates(nino: Nino, year: TaxYear, taxCodes: Seq[String])(
    implicit hc: HeaderCarrier,
    executionContext: ExecutionContext): Future[Map[String, BigDecimal]] = {
    def isScottishStandAloneTaxcode(taxCode: String) = "D0|D1|D2|D3|D4|D5|D6|D7|D8".r.findFirstIn(taxCode).isDefined

    if (taxCodes.exists(isScottishStandAloneTaxcode)) {
      taxAccountConnector.totalTax(nino, year).map {
        _.incomeCategories.flatMap(_.taxBands.map(band => band.bandType -> band.rate)).toMap
      } recover {
        case _: Exception => Map.empty[String, BigDecimal]
      }
    } else {
      Future.successful(Map.empty[String, BigDecimal])
    }
  }
}
