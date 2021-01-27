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

package uk.gov.hmrc.tai.connectors

import com.google.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.tai.metrics.Metrics
import uk.gov.hmrc.tai.model.JrsClaims
import uk.gov.hmrc.tai.model.enums.APITypes

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JrsConnector @Inject()(httpClient: HttpClient, metrics: Metrics, servicesConfig: ServicesConfig)(
  implicit ec: ExecutionContext) {

  def withoutSuffix(nino: Nino): String = {
    val BASIC_NINO_LENGTH = 8
    nino.value.take(BASIC_NINO_LENGTH)
  }

  val logger = Logger(this.getClass)

  def getJrsClaims(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[JrsClaims]] = {

    val serviceUrl: String = servicesConfig.baseUrl("coronavirus-jrs-published-employees")

    def jrsClaimsUrl(nino: String): String =
      s"$serviceUrl/coronavirus-jrs-published-employees/employee/$nino"

    lazy val bearerToken: String = "Bearer " + servicesConfig
      .getConfString("coronavirus-jrs-published-employees.authorizationToken", "local")

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = Seq("Authorization" -> bearerToken))

    val timerContext = metrics.startTimer(APITypes.JrsClaimAPI)

    httpClient.GET[HttpResponse](jrsClaimsUrl(withoutSuffix(nino))) map { response =>
      timerContext.stop()
      response.status match {
        case OK => {
          metrics.incrementSuccessCounter(APITypes.JrsClaimAPI)
          Some(response.json.as[JrsClaims])
        }
        case NO_CONTENT => {
          metrics.incrementSuccessCounter(APITypes.JrsClaimAPI)
          Some(JrsClaims(List.empty))
        }
        case _ => {
          metrics.incrementFailedCounter(APITypes.JrsClaimAPI)
          None
        }
      }
    } recover {
      case e => {
        metrics.incrementFailedCounter(APITypes.JrsClaimAPI)
        logger.warn(s"${e.getMessage}")
        timerContext.stop()
        None
      }
    }
  }
}
