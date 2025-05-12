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
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReadsInstances._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxCodeChangeConnector @Inject() (
  httpHandler: HttpHandler,
  servicesConfig: ServicesConfig,
  httpClientResponse: HttpClientResponse
)(implicit
  ec: ExecutionContext
) extends Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def baseTaxAccountUrl(nino: String): String = s"$serviceUrl/tai/$nino/tax-account/"

  def taxCodeChangeUrl(nino: String): String = baseTaxAccountUrl(nino) + "tax-code-change"

  def taxCodeChange(nino: Nino)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, TaxCodeChange] = {
    val url = taxCodeChangeUrl(nino.nino)

    httpClientResponse
      .read(
        httpHandler.httpClient
          .get(url"$url")
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .map(httpResponse => (httpResponse.json \ "data").as[TaxCodeChange])
  }

  def hasTaxCodeChangedUrl(nino: String): String = baseTaxAccountUrl(nino) + "tax-code-change/exists"

  def hasTaxCodeChanged(nino: Nino)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Boolean] = {
    val url = hasTaxCodeChangedUrl(nino.nino)

    httpClientResponse.read(
      httpHandler.httpClient
        .get(url"$url")
        .execute[Either[UpstreamErrorResponse, Boolean]]
    )
  }

  def lastTaxCodeRecordsUrl(nino: String, year: Int): String = baseTaxAccountUrl(nino) + s"$year/tax-code/latest"

  def lastTaxCodeRecords(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[List[TaxCodeRecord]] =
    httpHandler.getFromApiV2(lastTaxCodeRecordsUrl(nino.nino, year.year)).map { json =>
      (json \ "data").as[List[TaxCodeRecord]]
    } recover { case e: Exception =>
      logger.warn(s"Couldn't retrieve tax code records for $nino for year $year with exception: ${e.getMessage}")
      throw e
    }
}
