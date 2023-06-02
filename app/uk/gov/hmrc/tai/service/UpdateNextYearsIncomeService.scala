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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
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

  def isEstimatedPayJourneyCompleteForEmployer(
    id: Int
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Boolean] = {
    val key = s"${UpdateNextYearsIncomeConstants.Successful}-$id"
    successfulJourneyCacheService.currentCache.map(_ contains key)
  }

  def isEstimatedPayJourneyComplete(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Boolean] =
    successfulJourneyCacheService.currentCache.map(_ contains UpdateNextYearsIncomeConstants.Successful)

  private def setup(employmentId: Int, nino: Nino)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, UpdateNextYearsIncomeCacheModel] = ///// TODO - POC for MapN to for/yield
    for {
      taxCodeIncome <- taxAccountService.taxCodeIncomeForEmployment(nino, TaxYear().next, employmentId)
      employment    <- employmentService.employment(nino, employmentId) // TODO - Check if correct behaviour
    } yield (taxCodeIncome, employment) match {
      case (Some(taxCodeIncome), Some(employment)) =>
        val isPension = taxCodeIncome.componentType == PensionIncome
        UpdateNextYearsIncomeCacheModel(employment.name, employmentId, isPension, taxCodeIncome.amount.toInt)
    }

  def get(employmentId: Int, nino: Nino)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, UpdateNextYearsIncomeCacheModel] =
    journeyCacheService.currentCache.map(_ => setup(employmentId, nino)).flatten

  def amountKey(employmentId: Int): String =
    s"${UpdateNextYearsIncomeConstants.NewAmount}-$employmentId"

  def setNewAmount(newValue: String, employmentId: Int, nino: Nino)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Map[String, String]] =
    journeyCacheService.cache(amountKey(employmentId), convertCurrencyToInt(Some(newValue)).toString)

  def getNewAmount(employmentId: Int)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Int] =
    journeyCacheService.mandatoryJourneyValueAsInt(amountKey(employmentId))

  def submit(employmentId: Int, nino: Nino)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Done] =
    for {
      _         <- get(employmentId, nino)
      newAmount <- getNewAmount(employmentId)
      _ <- successfulJourneyCacheService
             .cache(Map(UpdateNextYearsIncomeConstants.Successful -> "true"))
      _ <- successfulJourneyCacheService
             .cache(Map(s"${UpdateNextYearsIncomeConstants.Successful}-$employmentId" -> "true"))
      _ <- taxAccountService.updateEstimatedIncome(nino, newAmount, TaxYear().next, employmentId)
    } yield Done
}
