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

import org.apache.pekko.Done
import cats.data.EitherT
import cats.implicits._
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.TaxAccountConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome, TaxCodeIncomeSourceStatus}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.domain.{TaxAccountSummary, TaxCodeIncomeComponentType, TaxedIncome}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.tai.util.EitherTExtensions._

class TaxAccountService @Inject() (taxAccountConnector: TaxAccountConnector)(implicit ec: ExecutionContext) {

  def incomeSources(
    nino: Nino,
    year: TaxYear,
    incomeType: TaxCodeIncomeComponentType,
    status: TaxCodeIncomeSourceStatus
  )(implicit hc: HeaderCarrier): Future[Seq[TaxedIncome]] =
    taxAccountConnector.incomeSources(nino, year, incomeType, status)

  def taxCodeIncomes(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Seq[TaxCodeIncome]]] =
    taxAccountConnector.taxCodeIncomes(nino, year)

  def newTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[TaxCodeIncome]] =
    taxAccountConnector.newTaxCodeIncomes(nino, year).transform {
      case Right(taxCodeIncomes)                        => Right(taxCodeIncomes)
      case Left(error) if error.statusCode == NOT_FOUND => Right(Seq.empty)
      case Left(error)                                  => Left(error)
    }

  def taxCodeIncomeForEmployment(nino: Nino, year: TaxYear, employmentId: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Option[TaxCodeIncome]]] =
    EitherT(taxAccountConnector.taxCodeIncomes(nino, year)).map(_.find(_.employmentId.contains(employmentId))).value

  def taxAccountSummary(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, TaxAccountSummary] =
    taxAccountConnector.taxAccountSummary(nino, year)

  def nonTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[NonTaxCodeIncome] =
    taxAccountConnector.nonTaxCodeIncomes(nino, year)

  def newNonTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Option[NonTaxCodeIncome]] =
    taxAccountConnector.newNonTaxCodeIncomes(nino, year).transform {
      case Right(response)                                                     => Right(Some(response))
      case Left(UpstreamErrorResponse(_, status, _, _)) if status == NOT_FOUND => Right(None)
      case Left(error)                                                         => Left(error)
    }

  def updateEstimatedIncome(nino: Nino, newAmount: Int, year: TaxYear, id: Int)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    taxAccountConnector.updateEstimatedIncome(nino, year, newAmount, id)

  def totalTax(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TotalTax] =
    taxAccountConnector.totalTax(nino, year).toFutureOrThrow

  def scottishBandRates(nino: Nino, year: TaxYear, taxCodes: Seq[String])(implicit
    hc: HeaderCarrier
  ): Future[Map[String, BigDecimal]] = {
    def isScottishStandAloneTaxcode(taxCode: String) = "D0|D1|D2|D3|D4|D5|D6|D7|D8".r.findFirstIn(taxCode).isDefined

    if (taxCodes.exists(isScottishStandAloneTaxcode)) {
      taxAccountConnector
        .totalTax(nino, year)
        .fold(
          _ => Map.empty[String, BigDecimal],
          totalTax => totalTax.incomeCategories.flatMap(_.taxBands.map(band => band.bandType -> band.rate)).toMap
        )
    } else {
      Future.successful(Map.empty[String, BigDecimal])
    }
  }
}
