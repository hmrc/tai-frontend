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

import controllers.auth.TaiUser
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

import scala.concurrent.Future

final case class UpdateNextYearsIncomeCacheModel(employmentName: String, employmentId: Int, currentValue: BigDecimal, newValue: Option[Int] = None) {
  def toCacheMap: Map[String, String] = {
    if (newValue.isDefined) {
      Map(
        UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME -> employmentName,
        UpdateNextYearsIncomeConstants.EMPLOYMENT_ID -> employmentId.toString,
        UpdateNextYearsIncomeConstants.CURRENT_AMOUNT -> currentValue.toString,
        UpdateNextYearsIncomeConstants.NEW_AMOUNT -> newValue.toString
      )
    } else {
      Map(
        UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME -> employmentName,
        UpdateNextYearsIncomeConstants.EMPLOYMENT_ID -> employmentId.toString,
        UpdateNextYearsIncomeConstants.CURRENT_AMOUNT -> currentValue.toString
      )
    }
  }
}

class UpdateNextYearsIncomeService {

  lazy val journeyCacheService: JourneyCacheService = JourneyCacheService(UpdateNextYearsIncomeConstants.JOURNEY_KEY)
  lazy val employmentService: EmploymentService = EmploymentService
  lazy val taxAccountService: TaxAccountService = TaxAccountService

  def setup(employmentId: Int)(implicit hc: HeaderCarrier, user: TaiUser): Future[UpdateNextYearsIncomeCacheModel] = {

    val nino: Nino = Nino(user.getNino)
    val taxCodeIncomeFuture = taxAccountService.taxCodeIncomeForEmployment(nino, TaxYear(), employmentId)
    val employmentFuture = employmentService.employment(nino, employmentId)

    for {
      taxCodeIncomeOption <- taxCodeIncomeFuture
      employmentOption <- employmentFuture
    } yield (taxCodeIncomeOption, employmentOption) match {
      case (Some(taxCodeIncome), Some(employment)) => {
        val model = UpdateNextYearsIncomeCacheModel(employment.name, employmentId, taxCodeIncome.amount)

        journeyCacheService.cache(model.toCacheMap)

        model
      }
    }
  }

  def get(employmentId: Int)(implicit hc: HeaderCarrier, user: TaiUser): Future[UpdateNextYearsIncomeCacheModel] = {
    journeyCacheService.currentCache flatMap {
      case cache: Map[String, String] if cache.isEmpty => setup(employmentId)
      case cache: Map[String, String] => {
          if (cache.contains(UpdateNextYearsIncomeConstants.NEW_AMOUNT)) {
            Future.successful(UpdateNextYearsIncomeCacheModel(cache(UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME),
            cache(UpdateNextYearsIncomeConstants.EMPLOYMENT_ID).toInt,
            cache(UpdateNextYearsIncomeConstants.CURRENT_AMOUNT).toInt,
            Some(cache(UpdateNextYearsIncomeConstants.NEW_AMOUNT).toInt)))
        } else {
            Future.successful(UpdateNextYearsIncomeCacheModel(cache(UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME),
            cache(UpdateNextYearsIncomeConstants.EMPLOYMENT_ID).toInt,
            cache(UpdateNextYearsIncomeConstants.CURRENT_AMOUNT).toInt))
        }
      }
    }
  }

  def setNewAmount(newValue: Int, employmentId: Int)(implicit hc: HeaderCarrier, user: TaiUser): Future[UpdateNextYearsIncomeCacheModel] = {
    get(employmentId) map { model =>
      val updatedValue = model.copy(newValue = Some(newValue))

      journeyCacheService.cache(updatedValue.toCacheMap)

      updatedValue
    }
  }

}
