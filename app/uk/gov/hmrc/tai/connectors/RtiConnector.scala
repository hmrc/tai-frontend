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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.AnnualAccount

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RtiConnector @Inject() (
  httpClientV2: HttpClientV2,
  httpClientResponse: HttpClientResponse,
  applicationConfig: ApplicationConfig
)(implicit ec: ExecutionContext) {

  val serviceUrl: String = applicationConfig.taiServiceUrl

  def rtiPaymentsUrl(nino: Nino, taxYear: TaxYear): String = s"$serviceUrl/tai/$nino/rti-payments/years/${taxYear.year}"

  def getPaymentsForYear(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[AnnualAccount]] =
    httpClientResponse
      .read(
        httpClientV2
          .get(url"${rtiPaymentsUrl(nino, year)}")
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .map { response =>
        println(response.body)
        (response.json \ "data").as[Seq[AnnualAccount]]
      }
}
