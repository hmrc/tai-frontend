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

package uk.gov.hmrc.tai.connectors

import cats.data.EitherT
import com.codahale.metrics.MetricRegistry
import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.metrics.HasMetrics
import uk.gov.hmrc.tai.model.JrsClaims

import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JrsConnector @Inject() (
  httpClient: HttpClient,
  val metrics: MetricRegistry,
  applicationConfig: ApplicationConfig
)(implicit
  ec: ExecutionContext
) extends HasMetrics with Logging {

  def getJrsClaimsForIndividual(nino: Nino)(hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, JrsClaims] = {

    def jrsClaimsUrl(nino: String): String =
      s"${applicationConfig.jrsClaimsServiceUrl}/coronavirus-jrs-published-employees/employee/$nino"

    implicit val jrsHeaderCarrier: HeaderCarrier = hc
      .withExtraHeaders(
        "CorrelationId" -> randomUUID.toString
      )

    EitherT {
      withMetricsTimerAsync("jrs-claim-data") { _ =>
        httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](jrsClaimsUrl(nino.value)).map {
          case Right(response) if response.status == NO_CONTENT => Right(JrsClaims(List.empty))
          case Right(response)                                  => Right(response.json.as[JrsClaims])
          case Left(error) if error.statusCode >= INTERNAL_SERVER_ERROR =>
            logger.error(error.message)
            Left(error)
          case Left(error) =>
            logger.error(error.message, error)
            Left(error)
        } recover { case exception: HttpException =>
          logger.error(exception.message)
          Left(UpstreamErrorResponse("Bad gateway or Timeout", BAD_GATEWAY))
        }
      }
    }
  }
}
