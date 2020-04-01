/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Named}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.connectors.responses.{TaiCacheError, TaiResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.model.domain.PensionIncome
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper.convertCurrencyToInt
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

import scala.concurrent.Future

class UpdateNextYearsIncomeService @Inject()(
  @Named("Update Next Years Income") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  employmentService: EmploymentService,
  taxAccountService: TaxAccountService) {

  def isEstimatedPayJourneyCompleteForEmployer(id: Int)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val key = s"${UpdateNextYearsIncomeConstants.SUCCESSFUL}-$id"
    successfulJourneyCacheService.currentCache map (_.get(key).isDefined)
  }

  def isEstimatedPayJourneyComplete(implicit hc: HeaderCarrier): Future[Boolean] =
    successfulJourneyCacheService.currentCache map (_.get(UpdateNextYearsIncomeConstants.SUCCESSFUL).isDefined)

  private def setup(employmentId: Int, nino: Nino)(
    implicit hc: HeaderCarrier): Future[UpdateNextYearsIncomeCacheModel] = {
    val taxCodeIncomeFuture = taxAccountService.taxCodeIncomeForEmployment(nino, TaxYear().next, employmentId)
    val employmentFuture = employmentService.employment(nino, employmentId)

    for {
      taxCodeIncomeOption <- taxCodeIncomeFuture
      employmentOption    <- employmentFuture
    } yield
      (taxCodeIncomeOption, employmentOption) match {
        case (Some(taxCodeIncome), Some(employment)) => {
          val isPension = taxCodeIncome.componentType == PensionIncome
          UpdateNextYearsIncomeCacheModel(employment.name, employmentId, isPension, taxCodeIncome.amount.toInt)
        }
        case _ =>
          throw new RuntimeException(
            "[UpdateNextYearsIncomeService] Could not set up next years estimated income journey")
      }
  }

  def get(employmentId: Int, nino: Nino)(implicit hc: HeaderCarrier): Future[UpdateNextYearsIncomeCacheModel] =
    journeyCacheService.currentCache flatMap { _ =>
      setup(employmentId, nino)
    }

  def amountKey(employmentId: Int): String =
    s"${UpdateNextYearsIncomeConstants.NEW_AMOUNT}-$employmentId"

  def setNewAmount(newValue: String, employmentId: Int, nino: Nino)(
    implicit hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheService.cache(amountKey(employmentId), convertCurrencyToInt(Some(newValue)).toString)

  def getNewAmount(employmentId: Int)(implicit hc: HeaderCarrier): Future[Either[String, Int]] =
    journeyCacheService.mandatoryJourneyValueAsInt(amountKey(employmentId))

  def submit(employmentId: Int, nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    get(employmentId, nino) flatMap { _ =>
      getNewAmount(employmentId).flatMap {
        case Right(newAmount) =>
          successfulJourneyCacheService
            .cache(Map(UpdateNextYearsIncomeConstants.SUCCESSFUL -> "true"))
            .flatMap { _ =>
              val successfulEmploymentKey = s"${UpdateNextYearsIncomeConstants.SUCCESSFUL}-$employmentId"
              successfulJourneyCacheService
                .cache(Map(successfulEmploymentKey -> "true"))
                .flatMap(_ => taxAccountService.updateEstimatedIncome(nino, newAmount, TaxYear().next, employmentId))
            }
        case Left(error) => Future.successful(TaiCacheError(error))
      }
    }
}
