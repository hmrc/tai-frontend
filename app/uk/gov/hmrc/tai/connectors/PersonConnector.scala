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
import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.connectors.responses._
import uk.gov.hmrc.tai.model.domain.Person

import scala.concurrent.Future

class PersonConnector @Inject()(httpHandler: HttpHandler) extends DefaultServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def personUrl(nino: String): String = s"$serviceUrl/tai/$nino/person"

  def person(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] = {

    httpHandler.getFromApi(personUrl(nino.nino)) map (
      json =>
        TaiSuccessResponseWithPayload((json \ "data").as[Person])
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve person details for $nino with exception:${e.getMessage}", e)
        TaiNotFoundResponse(e.getMessage)
    }
  }
}
