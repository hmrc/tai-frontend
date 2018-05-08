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

import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.tai.connectors.responses._
import uk.gov.hmrc.tai.model.domain.Person

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait PersonConnector {

  val serviceUrl: String

  def httpHandler: HttpHandler

  def personUrl(nino: String): String = s"$serviceUrl/$nino/person"

  def person(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] = {

    httpHandler.getFromApi(personUrl(nino.nino)) map (
      json =>
        TaiSuccessResponseWithPayload((json \ "data").as[Person])
      ) recover {
        case e: Exception =>
          Logger.warn(s"Couldn't retrieve tax payer details for $nino with exception:${e.getMessage}", e)
          TaiNotFoundResponse(e.getMessage)
      }
  }
}

object PersonConnector extends PersonConnector with ServicesConfig {

  override val serviceUrl = baseUrl("tai")

  override def httpHandler: HttpHandler = HttpHandler
}
