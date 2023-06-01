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

package uk.gov.hmrc.tai.service

import cats.data.EitherT
import play.api.Logging
import play.api.http.Status.BAD_GATEWAY

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.PensionProviderConnector
import uk.gov.hmrc.tai.model.domain.{AddPensionProvider, IncorrectPensionProvider}

import scala.concurrent.{ExecutionContext, Future}

class PensionProviderService @Inject() (pensionProviderConnector: PensionProviderConnector) extends Logging {

  def addPensionProvider(nino: Nino, pensionProvider: AddPensionProvider)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[String] =
    pensionProviderConnector.addPensionProvider(nino, pensionProvider) map {
      case Some(envId) => envId
      case _ =>
        throw new RuntimeException(
          s"No envelope id was generated when adding the new pension provider for ${nino.nino}"
        )
    }

  def incorrectPensionProvider(nino: Nino, id: Int, pensionProvider: IncorrectPensionProvider)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, String] =
    pensionProviderConnector.incorrectPensionProvider(nino, id, pensionProvider).transform {
      case Left(error) => Left(error)
      case Right(response) =>
        val data = (response.json \ "data").asOpt[String]
        data match {
          case Some(data) => Right(data)
          case None =>
            val exception = new RuntimeException(
              s"No envelope id was generated when attempting to end company benefit for ${nino.nino}"
            )
            logger.error(exception.getMessage, exception)
            Left(
              UpstreamErrorResponse(exception.getMessage, BAD_GATEWAY, BAD_GATEWAY)
            ) // TODO - Correct error status? May delete later
        }
    }
}