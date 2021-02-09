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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.JrsConnector
import uk.gov.hmrc.tai.model.JrsClaims

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JrsService @Inject()(jrsConnector: JrsConnector, appConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  def getJrsClaims(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[JrsClaims]] =
    jrsConnector.getJrsClaims(nino)(hc).map { response =>
      response match {
        case Some(jrsClaimsData) if (!jrsClaimsData.employers.isEmpty) => {
          Some(JrsClaims(appConfig, jrsClaimsData.employers))
        }

        case _ => None
      }
    }

  def checkIfJrsClaimsDataExist(nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] =
    if (appConfig.jrsClaimsEnabled) {
      jrsConnector.getJrsClaims(nino)(hc).map { response =>
        response match {
          case Some(_) => true
          case _       => false
        }
      }
    } else Future.successful(false)
}
