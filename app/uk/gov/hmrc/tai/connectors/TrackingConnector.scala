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

package uk.gov.hmrc.tai.connectors

import uk.gov.hmrc.tai.model.domain.tracking.TrackedForm
import uk.gov.hmrc.tai.model.domain.tracking.formatter.TrackedFormFormatters
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

trait TrackingConnector extends TrackedFormFormatters{

  val serviceUrl: String
  def httpHandler: HttpHandler
  private val IdType = "nino"

  def trackingUrl(id: String) = s"$serviceUrl/tracking-data/user/$IdType/$id"

  def getUserTracking(nino: String)(implicit hc: HeaderCarrier): Future[Seq[TrackedForm]] = {
    httpHandler.getFromApi(trackingUrl(nino)) map (
      _.as[Seq[TrackedForm]](trackedFormSeqReads))
  }

}

object TrackingConnector extends TrackingConnector with ServicesConfig {
  override val serviceUrl = baseUrl("tracking")
  override def httpHandler: HttpHandler = HttpHandler

}
