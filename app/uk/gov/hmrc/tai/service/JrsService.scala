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

import cats.data.{EitherT, OptionT}
import cats.implicits.catsStdInstancesForFuture
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.JrsConnector
import uk.gov.hmrc.tai.model.JrsClaims

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JrsService @Inject() (jrsConnector: JrsConnector, appConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  def getJrsClaims(nino: Nino)(implicit hc: HeaderCarrier): OptionT[Future, JrsClaims] = OptionT {
    jrsConnector
      .getJrsClaimsForIndividual(nino)(hc)
      .map(JrsClaims(appConfig, _))
      .getOrElse(None)
  }

  def checkIfJrsClaimsDataExist(
    nino: Nino
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Boolean] =
    if (appConfig.jrsClaimsEnabled) {
      jrsConnector
        .getJrsClaimsForIndividual(nino)(hc)
        .map(_.employers.nonEmpty)
    } else EitherT.rightT(false)
}
