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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyCarConnector @Inject() (
  httpClient: HttpClient,
  httpClientResponse: HttpClientResponse,
  servicesConfig: ServicesConfig
)(implicit
  ec: ExecutionContext
) extends Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

//  def companyCarEmploymentUrl(nino: Nino, empId: Int): String =
//    s"$serviceUrl/tai/$nino/tax-account/tax-components/employments/$empId/benefits/company-car" // TODO - Remove from backend if not used
  def companyCarUrl(nino: Nino): String = s"$serviceUrl/tai/$nino/tax-account/tax-components/benefits/company-cars"

  def companyCarsForCurrentYearEmployments(
    nino: Nino
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    httpClientResponse
      .read(httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](companyCarUrl(nino)))
//    httpHandler
//      .getFromApiV2(companyCarUrl(nino))
//      .map(json => (json \ "data" \ "companyCarBenefits").as[Seq[CompanyCarBenefit]])
//      .getOrElse(Seq.empty[CompanyCarBenefit]) // TODO - To remove one at a time to avoid an overextended change
}
