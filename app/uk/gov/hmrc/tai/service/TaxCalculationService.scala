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

import uk.gov.hmrc.tai.metrics.{GetTaxCalculationMetric, Metrics}
import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http._

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpDelete, HttpGet, HttpPost, HttpPut, NotFoundException, Upstream4xxResponse}
import uk.gov.hmrc.tai.config.WSHttp
import uk.gov.hmrc.tai.model.TaxCalculation

sealed trait TaxCalculationResponse

case class TaxCalculationSuccessResponse(taxCalculation: TaxCalculation) extends TaxCalculationResponse
case object TaxCalculationNotFoundResponse extends TaxCalculationResponse
case object TaxCalculationForbiddenResponse extends TaxCalculationResponse
case class TaxCalculationErrorResponse(cause: Exception) extends TaxCalculationResponse

trait TaxCalculationService extends uk.gov.hmrc.play.config.ServicesConfig {

  def http: HttpGet with HttpPost with HttpPut with HttpDelete
  def metrics: Metrics
  def taxCalcUrl: String

  def getTaxCalculation(nino: Nino, year: Int)(implicit hc: HeaderCarrier):
    Future[Option[TaxCalculation]] = {

      getTaxCalc(nino, year) map {
          case TaxCalculationSuccessResponse(taxCalc) => Some(taxCalc)
          case _ => None
      }

  }

  /**
   * Gets tax calc summary
   */
  private[service] def getTaxCalc(nino: Nino, year: Int)(implicit hc: HeaderCarrier): Future[TaxCalculationResponse] = {

    val timer = metrics.startTimer(GetTaxCalculationMetric)

    http.GET[TaxCalculation](s"$taxCalcUrl/taxcalc/$nino/taxSummary/$year") map { taxCalculation =>
      timer.stop()
      metrics.incrementSuccessCounter(GetTaxCalculationMetric)

      TaxCalculationSuccessResponse(taxCalculation)
    } recover {
      case e: NotFoundException =>
        timer.stop()
        metrics.incrementFailedCounter(GetTaxCalculationMetric)

        TaxCalculationNotFoundResponse
      case Upstream4xxResponse(_, FORBIDDEN, _, _) =>
        timer.stop()
        metrics.incrementFailedCounter(GetTaxCalculationMetric)

        TaxCalculationForbiddenResponse
      case e: Exception =>
        timer.stop()
        metrics.incrementFailedCounter(GetTaxCalculationMetric)

        TaxCalculationErrorResponse(e)
    }
  }
}
// $COVERAGE-OFF$
object TaxCalculationService extends TaxCalculationService {
  override val http = WSHttp
  override val metrics = Metrics
  override val taxCalcUrl: String = baseUrl("taxcalc")
}
// $COVERAGE-ON$
