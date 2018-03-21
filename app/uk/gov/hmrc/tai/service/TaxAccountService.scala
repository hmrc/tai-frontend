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

package uk.gov.hmrc.tai.service

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.connectors.TaxAccountConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.tax.TotalTax

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TaxAccountService {

  def taxAccountConnector: TaxAccountConnector

  def taxCodeIncomes(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    taxAccountConnector.taxCodeIncomes(nino, year)
  }

  def taxAccountSummary(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    taxAccountConnector.taxAccountSummary(nino, year)
  }

  def nonTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    taxAccountConnector.nonTaxCodeIncomes(nino, year)
  }

  def updateEstimatedIncome(nino: Nino, newAmount: Int, year: TaxYear, id: Int)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    taxAccountConnector.updateEstimatedIncome(nino, year, newAmount, id)
  }

  def totalTax(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    taxAccountConnector.totalTax(nino, year)
  }

  def scottishBandRates(nino: Nino, year: TaxYear, taxCodeIncomes: Seq[TaxCodeIncome])(implicit hc: HeaderCarrier): Future[Map[String, BigDecimal]] = {

    def isScottishStandAloneTaxcode(income: TaxCodeIncome) = "D0|D1|D2|D3|D4|D5|D6|D7|D8".r.findFirstIn(income.taxCode).isDefined

    if (taxCodeIncomes.exists(isScottishStandAloneTaxcode)) {
      taxAccountConnector.totalTax(nino, year) map {
        case TaiSuccessResponseWithPayload(totalTax: TotalTax) =>
          totalTax.incomeCategories.flatMap(_.taxBands.map(band => band.code -> band.rate)).toMap
        case _ => Map.empty[String, BigDecimal]
      }
    } else {
      Future.successful(Map.empty[String, BigDecimal])
    }
  }
}

object TaxAccountService extends TaxAccountService {
  override val taxAccountConnector: TaxAccountConnector = TaxAccountConnector
}
