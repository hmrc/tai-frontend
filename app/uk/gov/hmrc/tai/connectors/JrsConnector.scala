/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.tai.config.ApplicationConfig
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.tai.metrics.HasMetrics
import uk.gov.hmrc.tai.model.JrsClaims

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class JrsConnector @Inject()(httpClient: HttpClient, val metrics: Metrics, applicationConfig: ApplicationConfig)(
  implicit ec: ExecutionContext)
    extends HasMetrics with Logging {

  def getJrsClaimsForIndividual(nino: Nino)(hc: HeaderCarrier): OptionT[Future, JrsClaims] = {

    def jrsClaimsUrl(nino: String): String =
      s"${applicationConfig.jrsClaimsServiceUrl}/coronavirus-jrs-published-employees/employee/$nino"

    implicit val jrsHeaderCarrier: HeaderCarrier = hc
      .withExtraHeaders(
        "CorrelationId" -> randomUUID.toString
      )

    OptionT {
      withMetricsTimerAsync("jrs-claim-data") { _ =>
        httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](jrsClaimsUrl(nino.value)) map {
          case Right(response) if response.status == OK         => response.json.asOpt[JrsClaims]
          case Right(response) if response.status == NO_CONTENT => Some(JrsClaims(List.empty))
          case Right(_)                                         => None
          case Left(error) if error.statusCode == NOT_FOUND     => None
          case Left(error) if error.statusCode == FORBIDDEN => {
            logger.warn(error.message)
            None
          }
          case Left(error) if error.statusCode >= INTERNAL_SERVER_ERROR => {
            logger.error(error.message)
            None
          }
          case Left(error) => {
            logger.error(error.message, error)
            None
          }
        } recover {
          case exception: HttpException => {
            logger.error(exception.message)
            None
          }
        }
      }
    }
  }
}
