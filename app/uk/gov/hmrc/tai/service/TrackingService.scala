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

import cats.implicits._
import controllers.auth.DataRequest
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TrackingConnector
import uk.gov.hmrc.tai.model.domain.tracking.{TrackedForm, TrackedFormDone}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.journeyCache._

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

sealed trait TimeToProcess

case object ThreeWeeks extends TimeToProcess

case object FifteenDays extends TimeToProcess

case object NoTimeToProcess extends TimeToProcess

class TrackingService @Inject() (
  trackingConnector: TrackingConnector,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService
) {

  def isAnyIFormInProgress(
    nino: String
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[TimeToProcess] =
    (
      trackingConnector.getUserTracking(nino),
      successfulJourneyCacheService.currentCache
    ) mapN { case (trackedForms, successfulJournies) =>
      val haveAnyLongProcesses = hasIncompleteTrackingForms(trackedForms, "TES[1|7]")
      val haveAnyShortProcesses = hasIncompleteTrackingForms(trackedForms, "TES[2-6]")

      val filteredJournies = successfulJournies.keySet.filterNot(key =>
        key.contains(TrackSuccessfulJourneyConstants.EstimatedPayKey) || key.contains(
          UpdateNextYearsIncomeConstants.Successful
        )
          || key.contains(TrackSuccessfulJourneyConstants.UpdatePreviousYearsIncomeKey)
      )

      (
        haveAnyShortProcesses,
        haveAnyLongProcesses,
        filteredJournies.isEmpty,
        isA3WeeksJourney(successfulJournies)
      ) match {
        case (true, false, _, _) | (_, _, false, false) => FifteenDays
        case (_, true, _, _) | (_, _, false, true)      => ThreeWeeks
        case _                                          => NoTimeToProcess
      }
    }

  private def isA3WeeksJourney(journies: Map[String, String]): Boolean =
    journies exists {
      _ == TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey -> "true"
    }

  private def hasIncompleteTrackingForms(trackedForms: Seq[TrackedForm], regex: String): Boolean =
    trackedForms
      .filter(_.id.matches(regex))
      .filter(form => form.status != TrackedFormDone)
      .nonEmpty

}
