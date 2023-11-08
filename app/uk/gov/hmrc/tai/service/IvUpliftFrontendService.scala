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
import com.google.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.IvUpliftFrontendConnector
import uk.gov.hmrc.tai.model.IdentityVerificationResponse

import scala.concurrent.{ExecutionContext, Future}

class IvUpliftFrontendService @Inject() (
  ivUpliftFrontendConnector: IvUpliftFrontendConnector
) {
  def getIVJourneyStatus(journeyId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse] =
    ivUpliftFrontendConnector.getIVJourneyStatus(journeyId)(hc, ec).map(_.as[IdentityVerificationResponse])
}
