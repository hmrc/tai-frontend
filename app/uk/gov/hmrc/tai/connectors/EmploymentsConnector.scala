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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{AddEmployment, EndEmployment, IncorrectIncome}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentsConnector @Inject() (
  httpClient: HttpClient,
  httpClientResponse: HttpClientResponse,
  servicesConfig: ServicesConfig
)(implicit
  ec: ExecutionContext
) {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def employments(nino: Nino, year: TaxYear)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    httpClientResponse.read(
      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](employmentsUrl(nino, year))
    )
//    httpClientResponse
//      .getFromApiV2(employmentServiceUrl(nino, year))
//      .map { json =>
//        if ((json \ "data" \ "employments").validate[Seq[Employment]].isSuccess) {
//          (json \ "data" \ "employments").as[Seq[Employment]]
//        } else {
//          throw new RuntimeException("Invalid employment json")
//        }
//      }
//      .getOrElse(Seq.empty) // TODO - To remove one at a time to avoid an overextended change

  def ceasedEmployments(nino: Nino, year: TaxYear)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    httpClientResponse.read(
      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](ceasedEmploymentServiceUrl(nino, year))
    )

  def employment(nino: Nino, id: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] = // TODO - Merge this with employments()
    httpClientResponse.read(httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](employmentUrl(nino, id)))

  def endEmployment(nino: Nino, id: Int, endEmploymentData: EndEmployment)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    httpClientResponse.read(
      httpClient.PUT[EndEmployment, Either[UpstreamErrorResponse, HttpResponse]](
        endEmploymentServiceUrl(nino, id),
        endEmploymentData
      )
    )

  def addEmployment(nino: Nino, employment: AddEmployment)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    httpClientResponse.read(
      httpClient
        .POST[AddEmployment, Either[UpstreamErrorResponse, HttpResponse]](addEmploymentServiceUrl(nino), employment)
    )

  def incorrectEmployment(nino: Nino, id: Int, incorrectEmployment: IncorrectIncome)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    httpClientResponse.read(
      httpClient.POST[IncorrectIncome, Either[UpstreamErrorResponse, HttpResponse]](
        incorrectEmploymentServiceUrl(nino, id),
        incorrectEmployment
      )
    )

  def employmentUrl(nino: Nino, id: String): String = s"$serviceUrl/tai/$nino/employments/$id"

  private def endEmploymentServiceUrl(nino: Nino, id: Int): String = s"$serviceUrl/tai/$nino/employments/$id/end-date"

  private def addEmploymentServiceUrl(nino: Nino): String = s"$serviceUrl/tai/$nino/employments"

  private def employmentsUrl(nino: Nino, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/employments/years/${year.year}"

  private def ceasedEmploymentServiceUrl(nino: Nino, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/employments/year/${year.year}/status/ceased"

  private def incorrectEmploymentServiceUrl(nino: Nino, id: Int): String =
    s"$serviceUrl/tai/$nino/employments/$id/reason"
}
