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
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.connectors.responses.TaiResponse
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.model.domain.PensionIncome
import uk.gov.hmrc.tai.util.FormHelper.convertCurrencyToInt

import scala.concurrent.Future

class UpdateNextYearsIncomeService {

  lazy val journeyCacheService: JourneyCacheService = JourneyCacheService(UpdateNextYearsIncomeConstants.JOURNEY_KEY)
  lazy val employmentService: EmploymentService = EmploymentService
  lazy val taxAccountService: TaxAccountService = TaxAccountService

  def reset(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    journeyCacheService.flush()
  }

  private def setup(employmentId: Int, nino: Nino)(implicit hc: HeaderCarrier): Future[UpdateNextYearsIncomeCacheModel] = {
    val taxCodeIncomeFuture = taxAccountService.taxCodeIncomeForEmployment(nino, TaxYear().next, employmentId)
    val employmentFuture = employmentService.employment(nino, employmentId)

    for {
      taxCodeIncomeOption <- taxCodeIncomeFuture
      employmentOption <- employmentFuture
    } yield (taxCodeIncomeOption, employmentOption) match {
      case (Some(taxCodeIncome), Some(employment)) => {
        val isPension = taxCodeIncome.componentType == PensionIncome
        val model = UpdateNextYearsIncomeCacheModel(employment.name, employmentId, isPension, taxCodeIncome.amount.toInt)

        journeyCacheService.cache(model.toCacheMap)

        model
      }
      case _ => throw new RuntimeException("[UpdateNextYearsIncomeService] Could not set up next years estimated income journey")
    }
  }

  def get(employmentId: Int, nino: Nino)(implicit hc: HeaderCarrier): Future[UpdateNextYearsIncomeCacheModel] = {
    journeyCacheService.currentCache flatMap {
      case cache: Map[String, String] if cache.isEmpty => setup(employmentId, nino)
      case cache: Map[String, String] => {
        if (cache.contains(UpdateNextYearsIncomeConstants.NEW_AMOUNT)) {
          Future.successful(UpdateNextYearsIncomeCacheModel(
            cache(UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME),
            cache(UpdateNextYearsIncomeConstants.EMPLOYMENT_ID).toInt,
            cache(UpdateNextYearsIncomeConstants.IS_PENSION).toBoolean,
            cache(UpdateNextYearsIncomeConstants.CURRENT_AMOUNT).toInt,
            Some(cache(UpdateNextYearsIncomeConstants.NEW_AMOUNT).toInt)))
        } else {
          Future.successful(UpdateNextYearsIncomeCacheModel(cache(
            UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME),
            cache(UpdateNextYearsIncomeConstants.EMPLOYMENT_ID).toInt,
            cache(UpdateNextYearsIncomeConstants.IS_PENSION).toBoolean,
            cache(UpdateNextYearsIncomeConstants.CURRENT_AMOUNT).toInt))
        }
      }
    }
  }

  def setNewAmount(newValue: String, employmentId: Int, nino: Nino)(implicit hc: HeaderCarrier): Future[UpdateNextYearsIncomeCacheModel] = {
    get(employmentId, nino) map { model =>
      val updatedValue = model.copy(newValue = Some(convertCurrencyToInt(Some(newValue))))
      journeyCacheService.cache(updatedValue.toCacheMap)

      updatedValue
    }
  }
}
