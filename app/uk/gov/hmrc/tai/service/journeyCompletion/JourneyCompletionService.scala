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

package uk.gov.hmrc.tai.service.journeyCompletion

import controllers.auth.DataRequest
import play.api.Logging
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.journeyCache._

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

abstract class JourneyCompletionService(successfulJourneyCacheService: JourneyCacheService) extends Logging {

  protected def cache(
    key: String
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Map[String, String]] =
    successfulJourneyCacheService.cache(key, "true") recover { case NonFatal(exception) =>
      logger.warn(s"Failed to update Journey Completion service for key:$key caused by ${exception.getStackTrace}")
      Map.empty[String, String]
    }

  protected def currentValue(
    key: String
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext, request: DataRequest[AnyContent]): Future[Boolean] =
    successfulJourneyCacheService.currentValue(key) map (_.isDefined) recover { case NonFatal(exception) =>
      logger.warn(
        s"Failed to retrieve Journey Completion service value for key:$key caused by ${exception.getStackTrace}"
      )
      false
    }

  def journeyCompleted(
    incomeId: String
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Map[String, String]]

  def hasJourneyCompleted(
    id: String
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext, request: DataRequest[AnyContent]): Future[Boolean]

}

class EstimatedPayJourneyCompletionService @Inject() (
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService
) extends JourneyCompletionService(successfulJourneyCacheService) {

  override def journeyCompleted(
    incomeId: String
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Map[String, String]] =
    cache(s"${TrackSuccessfulJourneyConstants.EstimatedPayKey}-$incomeId")

  override def hasJourneyCompleted(
    id: String
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext, request: DataRequest[AnyContent]): Future[Boolean] =
    currentValue(s"${TrackSuccessfulJourneyConstants.EstimatedPayKey}-$id")
}
