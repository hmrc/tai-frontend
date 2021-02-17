/*
 * Copyright 2021 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits.catsStdInstancesForFuture
import controllers.Assets.Ok
import controllers.auth.DataRequest
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.{DataCacheConnector, JrsConnector}
import uk.gov.hmrc.tai.identifiers.JrsClaimsId
import uk.gov.hmrc.tai.model.JrsClaims

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JrsService @Inject()(
                            jrsConnector: JrsConnector,
                            appConfig: ApplicationConfig,
                            dataCacheConnector: DataCacheConnector
                          )(implicit ec: ExecutionContext) {

  private def checkCache()(implicit request: DataRequest[_]): Option[JrsClaims] = {
    request.cachedData.getJrsClaims
  }

  def getJrsClaims(nino: Nino)(implicit hc: HeaderCarrier, request: DataRequest[_]): OptionT[Future, JrsClaims] = {
    checkCache() match {
      case Some(jrsData) => OptionT { Future.successful(Some(jrsData)) }
      case None => val x =
        jrsConnector.getJrsClaimsForIndividual(nino)(hc).flatMap { dataMap =>
          val jrsClaimsOpt = JrsClaims(appConfig, dataMap)
          jrsClaimsOpt.map { claims =>
            val dataWithJrs = request.cachedData.set(JrsClaimsId, claims)(JrsClaims.formats)
            dataCacheConnector.save(dataWithJrs) map { _ =>
              claims
            }
          }
        x
      }
    }
  }

  def checkIfJrsClaimsDataExist(nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] =
    if (appConfig.jrsClaimsEnabled) {
      jrsConnector
        .getJrsClaimsForIndividual(nino)(hc)
        .map(_.employers.nonEmpty)
        .getOrElse(false)
    } else Future.successful(false)
}
