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

import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.Logger
import uk.gov.hmrc.tai.connectors.TrackingConnector
import uk.gov.hmrc.tai.model.domain.tracking.{TrackedForm, TrackedFormDone}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

import scala.util.control.NonFatal

class TrackingService @Inject() (trackingConnector: TrackingConnector,
                                 @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService
                                ) extends JourneyCacheConstants{

  def isAnyIFormInProgress(nino: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val trackingStatus = trackingForTesForms(nino) map { trackedForm =>
      !trackedForm.forall(_.status == TrackedFormDone)
    } recover {
      case NonFatal(x) => Logger.warn(s"Tracking service returned error, therefore return false in response. Error: ${x.getMessage}")
        false
    }

    for {
      isStatusAvailable <- trackingStatus
      isAnyJourneySuccessful <- successfulJourneyCacheService.currentCache map (_.nonEmpty)
    } yield isStatusAvailable || isAnyJourneySuccessful
  }

  def trackingForTesForms(nino: String)(implicit hc: HeaderCarrier): Future[Seq[TrackedForm]] = {
    trackingConnector.getUserTracking(nino).map(_.filter(_.id.matches("TES[1-7]")))
  }

}
