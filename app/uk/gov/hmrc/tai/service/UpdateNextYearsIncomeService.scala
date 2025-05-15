/*
 * Copyright 2024 HM Revenue & Customs
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

import org.apache.pekko.Done
import pages.income.{UpdateNextYearsIncomeNewAmountPage, UpdateNextYearsIncomeSuccessPage, UpdateNextYearsIncomeSuccessPageForEmployment}
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.util.FormHelper.convertCurrencyToInt

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateNextYearsIncomeService @Inject() (
  journeyCacheRepository: JourneyCacheRepository,
  employmentService: EmploymentService,
  taxAccountService: TaxAccountService
)(implicit ec: ExecutionContext) {

  def isEstimatedPayJourneyCompleteForEmployer(id: Int, userAnswers: UserAnswers): Future[Boolean] =
    Future.successful(userAnswers.get(UpdateNextYearsIncomeSuccessPageForEmployment(id)).getOrElse(false))

  def isEstimatedPayJourneyComplete(userAnswers: UserAnswers): Future[Boolean] =
    Future.successful(userAnswers.get(UpdateNextYearsIncomeSuccessPage).getOrElse(false))

  private def setup(employmentId: Int, nino: Nino)(implicit
    hc: HeaderCarrier
  ): Future[UpdateNextYearsIncomeCacheModel] =
    for {
      taxCodeIncomeResult <- taxAccountService.taxCodeIncomeForEmployment(nino, TaxYear().next, employmentId)
      employmentOpt       <- employmentService.employment(nino, employmentId)
    } yield employmentOpt match {
      case Some(employment) =>
        val currentValue = taxCodeIncomeResult match {
          case Right(Some(tci)) => Some(tci.amount.toInt)
          case _                => None
        }

        UpdateNextYearsIncomeCacheModel(
          employmentName = employment.name,
          employmentId = employmentId,
          isPension = employment.receivingOccupationalPension,
          currentValue = currentValue
        )

      case None =>
        throw new RuntimeException(
          "[UpdateNextYearsIncomeService] Could not set up next years estimated income journey"
        )
    }

  def get(employmentId: Int, nino: Nino, userAnswers: UserAnswers)(implicit
    hc: HeaderCarrier
  ): Future[UpdateNextYearsIncomeCacheModel] =
    journeyCacheRepository.get(userAnswers.sessionId, userAnswers.nino).flatMap(_ => setup(employmentId, nino))

  def setNewAmount(newValue: String, employmentId: Int, userAnswers: UserAnswers): Future[Map[String, String]] = {
    val value = convertCurrencyToInt(Some(newValue)).toString
    val amountKey = UpdateNextYearsIncomeNewAmountPage(employmentId).toString
    val updatedAnswers = userAnswers.setOrException(UpdateNextYearsIncomeNewAmountPage(employmentId), value)

    journeyCacheRepository.set(updatedAnswers).map { _ =>
      Map(amountKey -> value)
    }
  }

  def getNewAmount(employmentId: Int, userAnswers: UserAnswers): Future[Either[String, Int]] = {
    val key = UpdateNextYearsIncomeNewAmountPage(employmentId).toString

    Future.successful {
      userAnswers
        .get(UpdateNextYearsIncomeNewAmountPage(employmentId))
        .map(value => Right(value.toInt))
        .getOrElse(Left(s"Value for $key not found"))
    }
  }

  def submit(employmentId: Int, nino: Nino, userAnswers: UserAnswers)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Done] =
    for {
      _ <- get(employmentId, nino, userAnswers)
      newAmount <- getNewAmount(employmentId, userAnswers).flatMap {
                     case Right(amount) => Future.successful(amount)
                     case Left(error)   => Future.failed(new Exception(error))
                   }
      _ <- {
        val updatedUserAnswers = userAnswers
          .setOrException(UpdateNextYearsIncomeSuccessPage, true)
          .setOrException(UpdateNextYearsIncomeSuccessPageForEmployment(employmentId), true)

        journeyCacheRepository.set(updatedUserAnswers)
      }
      _ <- taxAccountService.updateEstimatedIncome(nino, newAmount, TaxYear().next, employmentId)
    } yield Done
}
