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
import controllers.auth.{DataRequest, OptionalDataRequest}
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.{DataCacheConnector, JrsConnector}
import uk.gov.hmrc.tai.identifiers.JrsClaimsId
import uk.gov.hmrc.tai.model.JrsClaims
import cats.implicits.catsStdInstancesForFuture
import uk.gov.hmrc.tai.util.CachedData

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JrsService @Inject()(
  jrsConnector: JrsConnector,
  appConfig: ApplicationConfig,
  dataCacheConnector: DataCacheConnector
)(implicit ec: ExecutionContext) {

  private def checkCache()(implicit request: OptionalDataRequest[_]): Option[JrsClaims] =
    request.cachedData.flatMap(_.getJrsClaims)

  private def getClaims(nino: Nino, hc: HeaderCarrier): OptionT[Future, JrsClaims] =
    jrsConnector.getJrsClaimsForIndividual(nino)(hc)

  def getJrsClaims(
    nino: Nino)(implicit hc: HeaderCarrier, request: OptionalDataRequest[_]): OptionT[Future, JrsClaims] =
    checkCache() match {
      case Some(jrsData) => OptionT[Future, JrsClaims](Future.successful(JrsClaims(appConfig, jrsData)))
      case None =>
        getClaims(nino, hc).flatMap { dataMap =>
          JrsClaims(appConfig, dataMap)
            .map { claims =>
              OptionT.liftF {
                val dataWithJrs = request.cachedData
                  .getOrElse(CachedData.empty(request.cacheId))
                  .set(JrsClaimsId, claims)(JrsClaims.formats)
                dataCacheConnector.save(dataWithJrs) map { _ =>
                  claims
                }
              }
            }
            .getOrElse(OptionT.none[Future, JrsClaims])
        }
    }

  def checkIfJrsClaimsDataExist(
    nino: Nino)(implicit hc: HeaderCarrier, request: OptionalDataRequest[_]): Future[Boolean] =
    if (appConfig.jrsClaimsEnabled) {

      checkCache() match {
        case Some(_) => Future.successful(true)
        case None =>
          getClaims(nino, hc)
            .flatMap { dataMap =>
              OptionT.liftF {
                JrsClaims(appConfig, dataMap).fold(Future.successful(false)) { claims =>
                  val dataWithJrs = request.cachedData
                    .getOrElse(CachedData.empty(request.cacheId))
                    .set(JrsClaimsId, claims)(JrsClaims.formats)
                  dataCacheConnector.save(dataWithJrs) map { _ =>
                    true
                  }
                }
              }
            }
            .getOrElseF(Future.successful(false))
      }
    } else {
      Future.successful(false)
    }
}
