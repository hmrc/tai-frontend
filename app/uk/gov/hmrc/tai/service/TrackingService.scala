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

package uk.gov.hmrc.tai.service

import javax.inject.{Inject, Named}
import play.api.Logger
import uk.gov.hmrc.tai.connectors.TrackingConnector
import uk.gov.hmrc.tai.model.domain.tracking.{TrackedForm, TrackedFormDone}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

import scala.util.control.NonFatal

sealed trait TimeToProcess

case object ThreeWeeks extends TimeToProcess

case object SevenDays extends TimeToProcess

case object NoTimeToProcess extends TimeToProcess

class TrackingService @Inject()(trackingConnector: TrackingConnector,
                                @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService
                               ) extends JourneyCacheConstants {

  def isAnyIFormInProgress(nino: String)(implicit hc: HeaderCarrier): Future[TimeToProcess] = {

    val longTESProcesses: Future[Seq[TrackedForm]] = unfinishedTrackingForms(nino, "TES[1|7]")
    val shortTESProcesses: Future[Seq[TrackedForm]] = unfinishedTrackingForms(nino, "TES[2-6]")

    for {
      haveAnyLongProcesses <- longTESProcesses map (_.nonEmpty)
      haveAnyShortProcesses <- shortTESProcesses map (_.nonEmpty)
      successfulJournies: Map[String, String] <- successfulJourneyCacheService.currentCache
    } yield {

      val filteredJournies = successfulJournies.keySet.filterNot(
        key => key.contains(TrackSuccessfulJourney_EstimatedPayKey) || key.contains(UpdateNextYearsIncomeConstants.SUCCESSFUL)
          || key.contains(TrackSuccessfulJourney_UpdatePreviousYearsIncomeKey)
      )

      (haveAnyShortProcesses, haveAnyLongProcesses, filteredJournies.isEmpty, isA3WeeksJourney(successfulJournies)) match {
        case(true, false, _, _) | (_ , _, false, false) => SevenDays
        case(_, true, _, _ ) | (_ , _, false, true) => ThreeWeeks
        case _ => NoTimeToProcess
      }
    }
  }

  private def isA3WeeksJourney(journies: Map[String, String]): Boolean = {
    journies exists { _ == TrackSuccessfulJourney_EndEmploymentBenefitKey -> "true" }
  }

  private def unfinishedTrackingForms(nino: String, regex: String)(implicit hc: HeaderCarrier): Future[Seq[TrackedForm]] = {
    val futureTrackedForms = trackingConnector.getUserTracking(nino).map(_.filter(_.id.matches(regex)))

    futureTrackedForms map { trackedForms =>
      trackedForms.filter(form => form.status != TrackedFormDone)
    } recover {
      case NonFatal(x) => {
        Logger.warn(s"Tracking service returned error, therefore returning an empty response. Error: ${x.getMessage}")
        Seq.empty[TrackedForm]
      }
    }
  }

}
