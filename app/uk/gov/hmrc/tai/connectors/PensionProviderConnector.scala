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

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.domain.{AddPensionProvider, IncorrectPensionProvider}

import scala.concurrent.{ExecutionContext, Future}

class PensionProviderConnector @Inject() (httpHandler: HttpHandler, servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def addPensionProvider(nino: Nino, pensionProvider: AddPensionProvider)(implicit
    hc: HeaderCarrier
  ): Future[Option[String]] =
    httpHandler.postToApi[AddPensionProvider](addPensionProviderServiceUrl(nino), pensionProvider).map { response =>
      (response.json \ "data").asOpt[String]
    }

  def addPensionProviderServiceUrl(nino: Nino): String = s"$serviceUrl/tai/$nino/pensionProvider"

  def incorrectPensionProvider(nino: Nino, id: Int, pensionProvider: IncorrectPensionProvider)(implicit
    hc: HeaderCarrier
  ): Future[Option[String]] =
    httpHandler.postToApi[IncorrectPensionProvider](incorrectPensionProviderServiceUrl(nino, id), pensionProvider).map {
      response =>
        (response.json \ "data").asOpt[String]
    }

  def incorrectPensionProviderServiceUrl(nino: Nino, id: Int): String =
    s"$serviceUrl/tai/$nino/pensionProvider/$id/reason"
}
