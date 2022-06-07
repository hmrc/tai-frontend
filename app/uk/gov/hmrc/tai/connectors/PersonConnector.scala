/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.connectors.responses._
import uk.gov.hmrc.tai.model.domain.{Address, Person}

import scala.concurrent.{ExecutionContext, Future}

class PersonConnector @Inject()(httpHandler: HttpHandler, servicesConfig: ServicesConfig)(implicit ec: ExecutionContext)
    extends Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def personUrl(nino: String): String = s"$serviceUrl/tai/$nino/person"

  def person(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    httpHandler.getFromApiV2(personUrl(nino.nino)) map (
      json => TaiSuccessResponseWithPayload((json \ "data").as[Person])
    ) recover {
      case e: Exception =>
        logger.warn(s"Couldn't retrieve person details for $nino with exception:${e.getMessage}", e)
        TaiNotFoundResponse(e.getMessage)
    }
}
