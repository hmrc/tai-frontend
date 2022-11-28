/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.Logging
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UnauthorizedException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.connectors.responses._
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.formatters.CodingComponentFormatters
import uk.gov.hmrc.tai.model.domain.income.{Incomes, TaxCodeIncome, TaxCodeIncomeSourceStatus}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.domain.{TaxAccountSummary, TaxCodeIncomeComponentType, TaxedIncome, UpdateTaxCodeIncomeRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class TaxAccountConnector @Inject()(httpHandler: HttpHandler, servicesConfig: ServicesConfig)(
  implicit ec: ExecutionContext)
    extends CodingComponentFormatters with Logging {

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
    status: TaxCodeIncomeSourceStatus)(implicit hc: HeaderCarrier): Future[Seq[TaxedIncome]] =
    httpHandler
      .getFromApiV2(
        incomeSourceUrl(nino = nino.nino, year = year, incomeType = incomeType.toString, status = status.toString))
      .map(json => (json \ "data").as[Seq[TaxedIncome]])

  def taxCodeIncomes(nino: Nino, year: TaxYear)(
    implicit hc: HeaderCarrier): Future[Either[String, Seq[TaxCodeIncome]]] =
    httpHandler.getFromApiV2(taxAccountUrl(nino.nino, year)) map (
      json => Right((json \ "data").as[Seq[TaxCodeIncome]](Reads.seq(taxCodeIncomeSourceReads)))
    ) recover {
      case e: Exception =>
        logger.warn(s"Couldn't retrieve tax code for $nino with exception:${e.getMessage}")
        Left(e.getMessage)
    }

  def nonTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    httpHandler.getFromApiV2(nonTaxCodeIncomeUrl(nino.nino, year)) map (
      json => TaiSuccessResponseWithPayload((json \ "data").as[Incomes].nonTaxCodeIncomes)
    ) recover {
      case e: UnauthorizedException => TaiUnauthorisedResponse(e.getMessage)
      case e: Exception =>
        logger.warn(s"Couldn't retrieve non tax code incomes for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }

  def codingComponents(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[CodingComponent]] =
    httpHandler.getFromApiV2(codingComponentsUrl(nino.nino, year)) map (
      json => (json \ "data").as[Seq[CodingComponent]](Reads.seq(codingComponentReads))
    ) recover {
      case e: NotFoundException =>
        logger.warn(s"Coding Components - No tax account information found: ${e.getMessage}")
        Seq.empty[CodingComponent]
    }

  def taxAccountSummary(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    httpHandler.getFromApiV2(taxAccountSummaryUrl(nino.nino, year)) map (
      json => TaiSuccessResponseWithPayload((json \ "data").as[TaxAccountSummary])
    ) recover {
      case e: NotFoundException =>
        logger.warn(s"No tax account information found: ${e.getMessage}")
        TaiNotFoundResponse(e.getMessage)
      case e: UnauthorizedException =>
        TaiUnauthorisedResponse(e.getMessage)
      case NonFatal(e) =>
        logger.warn(s"Couldn't retrieve tax summary for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }

  def updateEstimatedIncome(nino: Nino, year: TaxYear, newAmount: Int, id: Int)(
    implicit hc: HeaderCarrier): Future[Done] =
    httpHandler
      .putToApi(updateTaxCodeIncome(nino.nino, year, id), UpdateTaxCodeIncomeRequest(newAmount))
      .map(_ => Done)

  def totalTax(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    httpHandler.getFromApiV2(totalTaxUrl(nino.nino, year)) map (
      json => TaiSuccessResponseWithPayload((json \ "data").as[TotalTax])
    ) recover {
      case e: NotFoundException =>
        logger.warn(s"Total tax - No tax account information found: ${e.getMessage}")
        TaiNotFoundResponse(e.getMessage)
      case e: UnauthorizedException => TaiUnauthorisedResponse(e.getMessage)
      case e: Exception =>
        logger.warn(s"Couldn't retrieve total tax for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }

}
