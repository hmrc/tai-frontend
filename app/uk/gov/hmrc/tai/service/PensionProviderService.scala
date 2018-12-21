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

package uk.gov.hmrc.tai.service

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.PensionProviderConnector
import uk.gov.hmrc.tai.model.domain.{AddPensionProvider, IncorrectPensionProvider}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait PensionProviderService {

  def connector: PensionProviderConnector

  def addPensionProvider(nino: Nino, pensionProvider: AddPensionProvider)(implicit hc:HeaderCarrier): Future[String] = {
    connector.addPensionProvider(nino, pensionProvider) map {
      case Some(envId) => envId
      case _ => throw new RuntimeException(s"No envelope id was generated when adding the new pension provider for ${nino.nino}")
    }
  }

  def incorrectPensionProvider(nino: Nino, id: Int, pensionProvider: IncorrectPensionProvider)(implicit hc:HeaderCarrier):
  Future[String] = {connector.incorrectPensionProvider(nino, id, pensionProvider) map {
    case Some(envId) => envId
    case _ => throw new RuntimeException(s"No envelope id was generated when submitting incorrect pension for ${nino.nino}")
    }
  }
}
// $COVERAGE-OFF$
object PensionProviderService extends PensionProviderService {
  override lazy val connector: PensionProviderConnector = PensionProviderConnector
}
// $COVERAGE-ON$
