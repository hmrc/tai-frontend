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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{AddEmployment, Employment, EndEmployment, IncorrectIncome}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentsConnector @Inject() (
  http: HttpClientV2,
  httpHandler: HttpHandler,
  applicationConfig: ApplicationConfig
)(implicit
  ec: ExecutionContext
) {

  val serviceUrl: String = applicationConfig.taiServiceUrl

  def employmentUrl(nino: Nino, id: String): String = s"$serviceUrl/tai/$nino/employments/$id"

  private def filterDate(dateOption: Option[LocalDate]): Option[LocalDate] =
    dateOption.filter(_.isAfter(applicationConfig.startEmploymentDateFilteredBefore))

  def employments(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[Employment]] =
    httpHandler
      .read(
        http
          .get(url"${employmentServiceUrl(nino, year)}")
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .map { response =>
        (response.json \ "data" \ "employments").as[Seq[Employment]].map { employment =>
          employment.copy(startDate = filterDate(employment.startDate))
        }
      }

  def ceasedEmployments(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    httpHandler.getFromApiV2(ceasedEmploymentServiceUrl(nino, year)).map { json =>
      (json \ "data").as[Seq[Employment]].map { employment =>
        employment.copy(startDate = filterDate(employment.startDate))
      }
    }

  def employment(nino: Nino, id: String)(implicit hc: HeaderCarrier): Future[Option[Employment]] =
    httpHandler
      .getFromApiV2(employmentUrl(nino, id))
      .map(json =>
        (json \ "data").asOpt[Employment].map { employment =>
          employment.copy(startDate = filterDate(employment.startDate))
        }
      )

  def endEmployment(nino: Nino, id: Int, endEmploymentData: EndEmployment)(implicit hc: HeaderCarrier): Future[String] =
    httpHandler.putToApi[EndEmployment](endEmploymentServiceUrl(nino, id), endEmploymentData).map { response =>
      if ((response.json \ "data").validate[String].isSuccess) {
        (response.json \ "data").as[String]
      } else {
        throw new RuntimeException("Invalid json")
      }
    }

  def addEmployment(nino: Nino, employment: AddEmployment)(implicit hc: HeaderCarrier): Future[Option[String]] =
    httpHandler.postToApi[AddEmployment](addEmploymentServiceUrl(nino), employment).map { response =>
      (response.json \ "data").asOpt[String]
    }

  def incorrectEmployment(nino: Nino, id: Int, incorrectEmployment: IncorrectIncome)(implicit
    hc: HeaderCarrier
  ): Future[Option[String]] =
    httpHandler.postToApi[IncorrectIncome](incorrectEmploymentServiceUrl(nino, id), incorrectEmployment).map {
      response =>
        (response.json \ "data").asOpt[String]
    }

  def endEmploymentServiceUrl(nino: Nino, id: Int): String = s"$serviceUrl/tai/$nino/employments/$id/end-date"

  def addEmploymentServiceUrl(nino: Nino): String = s"$serviceUrl/tai/$nino/employments"

  def employmentServiceUrl(nino: Nino, year: TaxYear): String = s"$serviceUrl/tai/$nino/employments/years/${year.year}"

  def ceasedEmploymentServiceUrl(nino: Nino, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/employments/year/${year.year}/status/ceased"

  def incorrectEmploymentServiceUrl(nino: Nino, id: Int): String = s"$serviceUrl/tai/$nino/employments/$id/reason"
}
