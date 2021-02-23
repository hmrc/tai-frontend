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

import java.util.UUID.randomUUID

import cats.data.OptionT
import com.google.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config
import uk.gov.hmrc.tai.config.ApplicationConfig
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.tai.metrics.HasMetrics
import uk.gov.hmrc.tai.model.JrsClaims
import uk.gov.hmrc.tai.model.enums.APITypes

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JrsConnector @Inject()(httpClient: HttpClient, val metrics: Metrics, applicationConfig: ApplicationConfig)(
  implicit ec: ExecutionContext)
    extends HasMetrics {

  val logger = Logger(this.getClass)

  def getJrsClaimsForIndividual(nino: Nino)(hc: HeaderCarrier): OptionT[Future, JrsClaims] = {

    def jrsClaimsUrl(nino: String): String =
      s"${applicationConfig.jrsClaimsServiceUrl}/coronavirus-jrs-published-employees/employee/$nino"

    implicit val jrsHeaderCarrier: HeaderCarrier = hc
      .withExtraHeaders(
        "CorrelationId" -> randomUUID.toString
      )

    OptionT {
      withMetricsTimerAsync("jrs-claim-data") { _ =>
        httpClient.GET[HttpResponse](jrsClaimsUrl(nino.value)) map { response =>
          response.status match {
            case OK => {
              response.json.asOpt[JrsClaims]
            }
            case NO_CONTENT => {
              Some(JrsClaims(List.empty))
            }
            case _ => {
              None
            }
          }
        } recover {
          case e => {
            logger.warn(s"${e.getMessage}")
            None
          }
        }
      }
    }
  }
}
