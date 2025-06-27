/*
 * Copyright 2025 HM Revenue & Customs
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

package testOnly.connectors

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaiConnector @Inject() (httpClientV2: HttpClientV2, servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def url(path: String): String = s"$serviceUrl$path"

  def employmentDetails(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val urlGET = url(s"/test-only/v1/api/employment/employee/$nino/tax-year/$taxYear/employment-details")
    httpClientV2
      .get(url"$urlGET")
      .execute[HttpResponse]
  }

  def taxAccount(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val urlGET = url(s"/test-only/v1/api/person/$nino/tax-account/$taxYear")
    httpClientV2
      .get(url"$urlGET")
      .execute[HttpResponse]
  }
  def iabds(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[HttpResponse]      = {
    val urlGET = url(s"/test-only/v1/api/iabd/taxpayer/$nino/tax-year/$taxYear")
    httpClientV2
      .get(url"$urlGET")
      .execute[HttpResponse]
  }
}
