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

import akka.Done
import cats.data.EitherT
import play.api.Logging
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.formatters.CodingComponentFormatters
import uk.gov.hmrc.tai.model.domain.income.{Incomes, NonTaxCodeIncome, OtherNonTaxCodeIncome, TaxCodeIncome, TaxCodeIncomeSourceStatus}
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, TotalTax}
import uk.gov.hmrc.tai.model.domain.{TaxAccountSummary, TaxCodeIncomeComponentType, TaxedIncome, UpdateTaxCodeIncomeRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxAccountConnector @Inject() (
  httpClient: HttpClient,
  httpClientResponse: HttpClientResponse,
  servicesConfig: ServicesConfig
)(implicit
  ec: ExecutionContext
) extends CodingComponentFormatters with Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def taxAccountUrl(nino: String, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/tax-account/${year.year}/income/tax-code-incomes"

  def incomeSourceUrl(nino: String, year: TaxYear, incomeType: String, status: String): String =
    s"$serviceUrl/tai/$nino/tax-account/year/${year.year}/income/$incomeType/status/$status"

  def nonTaxCodeIncomeUrl(nino: String, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/tax-account/${year.year}/income"

  def codingComponentsUrl(nino: String, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/tax-account/${year.year}/tax-components"

  def taxAccountSummaryUrl(nino: String, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/tax-account/${year.year}/summary"

  def updateTaxCodeIncome(nino: String, year: TaxYear, id: Int): String =
    s"$serviceUrl/tai/$nino/tax-account/snapshots/${year.year}/incomes/tax-code-incomes/$id/estimated-pay"

  def totalTaxUrl(nino: String, year: TaxYear): String = s"$serviceUrl/tai/$nino/tax-account/${year.year}/total-tax"

  def incomeSources(
    nino: Nino,
    year: TaxYear,
    incomeType: TaxCodeIncomeComponentType,
    status: TaxCodeIncomeSourceStatus
  )(implicit hc: HeaderCarrier): Future[Seq[TaxedIncome]] =
    httpClientResponse
      .getFromApiV2(
        incomeSourceUrl(nino = nino.nino, year = year, incomeType = incomeType.toString, status = status.toString)
      )
      .map(json => (json \ "data").as[Seq[TaxedIncome]])
      .getOrElse(Seq.empty) // TODO - To remove one at a time to avoid an overextended change

  def taxCodeIncomes(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] = {
    httpClientResponse.read(
      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](taxAccountUrl(nino.nino, year))
    )
  }

  def nonTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[NonTaxCodeIncome] =
    httpClientResponse
      .getFromApiV2(nonTaxCodeIncomeUrl(nino.nino, year))
      .map { json =>
        (json \ "data").as[Incomes].nonTaxCodeIncomes
      }
      .getOrElse(
        NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
      ) // TODO - To remove one at a time to avoid an overextended change

  def codingComponents(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[CodingComponent]] =
    httpClientResponse
      .getFromApiV2(codingComponentsUrl(nino.nino, year))
      .map(json => (json \ "data").as[Seq[CodingComponent]](Reads.seq(codingComponentReads)))
      .recover { case e: NotFoundException =>
        logger.warn(s"Coding Components - No tax account information found: ${e.getMessage}")
        Seq.empty[CodingComponent]
      }
      .getOrElse(Seq.empty[CodingComponent]) // TODO - To remove one at a time to avoid an overextended change

  def taxAccountSummary(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaxAccountSummary] =
    httpClientResponse
      .getFromApiV2(taxAccountSummaryUrl(nino.nino, year))
      .map(json => (json \ "data").as[TaxAccountSummary])
      .getOrElse(
        TaxAccountSummary(BigDecimal(10), BigDecimal(10), BigDecimal(10), BigDecimal(10), BigDecimal(10))
      ) // TODO - To remove one at a time to avoid an overextended change

  def updateEstimatedIncome(nino: Nino, year: TaxYear, newAmount: Int, id: Int)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    httpClientResponse.read(
      httpClient.PUT[UpdateTaxCodeIncomeRequest, Either[UpstreamErrorResponse, HttpResponse]](
        updateTaxCodeIncome(nino.nino, year, id),
        UpdateTaxCodeIncomeRequest(newAmount)
      )
    )

  def totalTax(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TotalTax] =
    httpClientResponse
      .getFromApiV2(totalTaxUrl(nino.nino, year))
      .map(json => (json \ "data").as[TotalTax])
      .getOrElse(
        TotalTax(BigDecimal(10), Seq.empty[IncomeCategory], None, None, None)
      ) // TODO - To remove one at a time to avoid an overextended change

}
