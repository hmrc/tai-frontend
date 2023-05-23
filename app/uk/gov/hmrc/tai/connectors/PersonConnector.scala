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

package uk.gov.hmrc.tai.connectors

import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.domain.{Address, Person}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonConnector @Inject() (httpHandler: HttpClientResponse, servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) extends Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def personUrl(nino: String): String = s"$serviceUrl/tai/$nino/person"

  def person(nino: Nino)(implicit hc: HeaderCarrier): Future[Person] =
    httpHandler
      .getFromApiV2(personUrl(nino.nino))
      .map { json =>
        (json \ "data").as[Person]
      }
      .getOrElse(
        Person(Nino("AA000003A"), "", "", false, false, Address(None, None, None, None, None))
      ) // TODO - To remove one at a time to avoid an overextended change
}
