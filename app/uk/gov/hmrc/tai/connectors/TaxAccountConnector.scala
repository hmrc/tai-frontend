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

package uk.gov.hmrc.tai.connectors

import cats.data.EitherT
import org.apache.pekko.Done
import play.api.Logging
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.formatters.CodingComponentFormatters
import uk.gov.hmrc.tai.model.domain.income.{Incomes, NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.domain.{TaxAccountSummary, UpdateTaxCodeIncomeRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxAccountConnector @Inject() (
  httpHandler: HttpHandler,
  servicesConfig: ServicesConfig
)(implicit
  ec: ExecutionContext
) extends CodingComponentFormatters
    with Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def taxAccountUrl(nino: String, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/tax-account/${year.year}/income/tax-code-incomes"

  def nonTaxCodeIncomeUrl(nino: String, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/tax-account/${year.year}/income"

  def codingComponentsUrl(nino: String, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/tax-account/${year.year}/tax-components"

  def taxAccountSummaryUrl(nino: String, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/tax-account/${year.year}/summary"

  def updateTaxCodeIncome(nino: String, year: TaxYear, id: Int): String =
    s"$serviceUrl/tai/$nino/tax-account/snapshots/${year.year}/incomes/tax-code-incomes/$id/estimated-pay"

  def totalTaxUrl(nino: String, year: TaxYear): String = s"$serviceUrl/tai/$nino/tax-account/${year.year}/total-tax"

  def taxCodeIncomes(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Seq[TaxCodeIncome]]] =
    httpHandler.getFromApiV2(taxAccountUrl(nino.nino, year)) map (json =>
      Right((json \ "data").as[Seq[TaxCodeIncome]](Reads.seq(taxCodeIncomeSourceReads)))
    ) recover { case e: Exception =>
      logger.warn(s"Couldn't retrieve tax code for $nino with exception:${e.getMessage}")
      Left(e.getMessage)
    }

  def newTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[TaxCodeIncome]] = {
    val url = taxAccountUrl(nino.nino, year)
    httpHandler
      .read(
        httpHandler.httpClient
          .get(url"$url")
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .map(httpResponse => (httpResponse.json \ "data").as[Seq[TaxCodeIncome]](Reads.seq(taxCodeIncomeSourceReads)))
  }

  def nonTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[NonTaxCodeIncome] =
    httpHandler.getFromApiV2(nonTaxCodeIncomeUrl(nino.nino, year)).map { json =>
      (json \ "data").as[Incomes].nonTaxCodeIncomes
    } recover { case e: Exception =>
      logger.warn(s"Couldn't retrieve non tax code incomes for $nino with exception:${e.getMessage}")
      throw e
    }

  def newNonTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, NonTaxCodeIncome] = {
    val url = nonTaxCodeIncomeUrl(nino.nino, year)
    httpHandler
      .read(
        httpHandler.httpClient
          .get(url"$url")
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .map(httpResponse => (httpResponse.json \ "data").as[Incomes].nonTaxCodeIncomes)
  }

  def codingComponents(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[CodingComponent]] =
    httpHandler.getFromApiV2(codingComponentsUrl(nino.nino, year)) map (json =>
      (json \ "data").as[Seq[CodingComponent]](Reads.seq(codingComponentReads))
    ) recover { case e: NotFoundException =>
      logger.warn(s"Coding Components - No tax account information found: ${e.getMessage}")
      Seq.empty[CodingComponent]
    }

  def taxAccountSummary(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, TaxAccountSummary] = {
    val url = taxAccountSummaryUrl(nino.nino, year)
    httpHandler
      .read(
        httpHandler.httpClient
          .get(url"$url")
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .map { httpResponse =>
        (httpResponse.json \ "data").as[TaxAccountSummary]
      }
  }

  def updateEstimatedIncome(nino: Nino, year: TaxYear, newAmount: Int, id: Int)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpHandler
      .putToApi(updateTaxCodeIncome(nino.nino, year, id), UpdateTaxCodeIncomeRequest(newAmount))
      .map(_ => Done)

  def totalTax(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TotalTax] =
    httpHandler.getFromApiV2(totalTaxUrl(nino.nino, year)) map (json => (json \ "data").as[TotalTax])
}
