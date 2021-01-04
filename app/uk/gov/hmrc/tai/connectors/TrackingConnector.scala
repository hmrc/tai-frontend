/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.connectors

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.domain.tracking.TrackedForm
import uk.gov.hmrc.tai.model.domain.tracking.formatter.TrackedFormFormatters

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class TrackingConnector @Inject()(httpHandler: HttpHandler, servicesConfig: ServicesConfig)(
  implicit ec: ExecutionContext)
    extends TrackedFormFormatters {

  lazy val serviceUrl: String = servicesConfig.baseUrl("tracking")

  private val IdType = "nino"

  def trackingUrl(id: String) = s"$serviceUrl/tracking-data/user/$IdType/$id"

  def getUserTracking(nino: String)(implicit hc: HeaderCarrier): Future[Seq[TrackedForm]] =
    (httpHandler.getFromApi(trackingUrl(nino)) map (_.as[Seq[TrackedForm]](trackedFormSeqReads))).recover {
      case NonFatal(x) => {
        Logger.warn(s"Tracking service returned error, therefore returning an empty response. Error: ${x.getMessage}")
        Seq.empty[TrackedForm]
      }
    }
}
