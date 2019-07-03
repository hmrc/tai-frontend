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

package uk.gov.hmrc.tai.connectors

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.model.domain.tracking.TrackedForm
import uk.gov.hmrc.tai.model.domain.tracking.formatter.TrackedFormFormatters

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrackingConnector @Inject() (httpHandler: HttpHandler) extends TrackedFormFormatters with DefaultServicesConfig {

  lazy val serviceUrl: String = baseUrl("tracking")

  private val IdType = "nino"

  def trackingUrl(id: String) = s"$serviceUrl/tracking-data/user/$IdType/$id"

  def getUserTracking(nino: String)(implicit hc: HeaderCarrier): Future[Seq[TrackedForm]] = {
    httpHandler.getFromApi(trackingUrl(nino)) map (
      _.as[Seq[TrackedForm]](trackedFormSeqReads))
  }

}
