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

import akka.Done
import cats.data.EitherT
import cats.implicits._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.model.domain.PensionIncome
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper.convertCurrencyToInt
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class UpdateNextYearsIncomeService @Inject() (
  @Named("Update Next Years Income") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  employmentService: EmploymentService,
  taxAccountService: TaxAccountService
)(implicit ec: ExecutionContext) {

  def isEstimatedPayJourneyCompleteForEmployer(id: Int)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val key = s"${UpdateNextYearsIncomeConstants.Successful}-$id"
    successfulJourneyCacheService.currentCache.map(_ contains key)
  }

  def isEstimatedPayJourneyComplete(implicit hc: HeaderCarrier): Future[Boolean] =
    successfulJourneyCacheService.currentCache.map(_ contains UpdateNextYearsIncomeConstants.Successful)

  private def setup(employmentId: Int, nino: Nino)(implicit
    hc: HeaderCarrier
  ): Future[UpdateNextYearsIncomeCacheModel] =
    (
      taxAccountService.taxCodeIncomeForEmployment(nino, TaxYear().next, employmentId),
      employmentService.employment(nino, employmentId).getOrElse(None) // TODO - Check if correct behaviour
    ).mapN {
      case (Right(Some(taxCodeIncome)), Some(employment)) =>
        val isPension = taxCodeIncome.componentType == PensionIncome
        UpdateNextYearsIncomeCacheModel(employment.name, employmentId, isPension, taxCodeIncome.amount.toInt)
      case _ =>
        throw new RuntimeException(
          "[UpdateNextYearsIncomeService] Could not set up next years estimated income journey"
        )
    }

  def get(employmentId: Int, nino: Nino)(implicit hc: HeaderCarrier): Future[UpdateNextYearsIncomeCacheModel] =
    journeyCacheService.currentCache flatMap { _ =>
      setup(employmentId, nino)
    }

  def amountKey(employmentId: Int): String =
    s"${UpdateNextYearsIncomeConstants.NewAmount}-$employmentId"

  def setNewAmount(newValue: String, employmentId: Int, nino: Nino)(implicit
    hc: HeaderCarrier
  ): Future[Map[String, String]] =
    journeyCacheService.cache(amountKey(employmentId), convertCurrencyToInt(Some(newValue)).toString)

  def getNewAmount(employmentId: Int)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Int] =
    journeyCacheService.mandatoryJourneyValueAsInt(amountKey(employmentId))

  def submit(employmentId: Int, nino: Nino)(implicit hc: HeaderCarrier): Future[Done] =
    for {
      _ <- get(employmentId, nino)
      newAmount <- getNewAmount(employmentId).fold(
                     _ => 0,
                     data => data
                   ) // TODO - Hard set value until taxAccountService.updateEstimatedIncome() is refactored
      _ <- successfulJourneyCacheService
             .cache(Map(UpdateNextYearsIncomeConstants.Successful -> "true"))
      _ <- successfulJourneyCacheService
             .cache(Map(s"${UpdateNextYearsIncomeConstants.Successful}-$employmentId" -> "true"))
      _ <- taxAccountService.updateEstimatedIncome(nino, newAmount, TaxYear().next, employmentId)
    } yield Done
}
