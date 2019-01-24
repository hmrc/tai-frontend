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

package uk.gov.hmrc.tai.connectors

import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.formatters.CodingComponentFormatters
import uk.gov.hmrc.tai.model.domain.income.{Incomes, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.domain.{IncomeSource, TaxAccountSummary, UpdateTaxCodeIncomeRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxAccountConnector @Inject() (httpHandler: HttpHandler) extends CodingComponentFormatters with ServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def taxAccountUrl(nino: String, year: TaxYear): String = s"$serviceUrl/tai/$nino/tax-account/${year.year}/income/tax-code-incomes"

  def incomeSourceUrl(nino: String, year: TaxYear, incomeType: String, status: String): String =
    s"$serviceUrl/tai/$nino/tax-account/year/${year.year}/income/$incomeType/status/$status"

  def nonTaxCodeIncomeUrl(nino: String, year: TaxYear): String = s"$serviceUrl/tai/$nino/tax-account/${year.year}/income"

  def codingComponentsUrl(nino: String, year: TaxYear): String = s"$serviceUrl/tai/$nino/tax-account/${year.year}/tax-components"

  def taxAccountSummaryUrl(nino: String, year: TaxYear): String = s"$serviceUrl/tai/$nino/tax-account/${year.year}/summary"

  def updateTaxCodeIncome(nino: String, year: TaxYear, id: Int): String =
    s"$serviceUrl/tai/$nino/tax-account/snapshots/${year.year}/incomes/tax-code-incomes/$id/estimated-pay"

  def totalTaxUrl(nino: String, year: TaxYear): String = s"$serviceUrl/tai/$nino/tax-account/${year.year}/total-tax"

  def incomeSources(nino: Nino, year: TaxYear, incomeType: String, status: String)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(incomeSourceUrl(nino.nino, year, incomeType, status)).map (
      json =>
        TaiSuccessResponseWithPayload((json \ "data").as[Seq[IncomeSource]])
      ) recover {
      case _: NotFoundException =>
        TaiSuccessResponseWithPayload(Seq.empty[IncomeSource])
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve $status $incomeType income sources for $nino with exception:${e.getMessage}",e)

        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

  def taxCodeIncomes(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(taxAccountUrl(nino.nino, year)) map (
      json =>
        TaiSuccessResponseWithPayload((json \ "data").as[Seq[TaxCodeIncome]](Reads.seq(taxCodeIncomeSourceReads)))
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve tax code for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

  def nonTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(nonTaxCodeIncomeUrl(nino.nino, year)) map (
      json =>
        TaiSuccessResponseWithPayload((json \ "data").as[Incomes].nonTaxCodeIncomes)
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve non tax code incomes for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

  def codingComponents(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(codingComponentsUrl(nino.nino, year)) map (
      json =>
        TaiSuccessResponseWithPayload((json \ "data").as[Seq[CodingComponent]](Reads.seq(codingComponentReads)))
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve coding components for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

  def taxAccountSummary(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(taxAccountSummaryUrl(nino.nino, year)) map (
      json =>
        TaiSuccessResponseWithPayload((json \ "data").as[TaxAccountSummary])
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve tax summary for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

  def updateEstimatedIncome(nino: Nino, year: TaxYear, newAmount: Int, id: Int)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.putToApi(updateTaxCodeIncome(nino.nino, year, id), UpdateTaxCodeIncomeRequest(newAmount)) map (_ =>
      TaiSuccessResponse
      ) recover {
      case e: Exception =>
        Logger.warn(s"Error while updating estimated income for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

  def totalTax(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(totalTaxUrl(nino.nino, year)) map (
      json =>
        TaiSuccessResponseWithPayload((json \ "data").as[TotalTax])
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve total tax for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

}
