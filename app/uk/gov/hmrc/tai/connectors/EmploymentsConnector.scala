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

package uk.gov.hmrc.tai.connectors

import cats.data.EitherT
import cats.implicits._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{AddEmployment, Employment, EndEmployment, IncorrectIncome}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmploymentsConnector @Inject() (httpHandler: HttpHandler, applicationConfig: ApplicationConfig)(implicit
  ec: ExecutionContext
) {

  val serviceUrl: String = applicationConfig.taiServiceUrl

  private val startDateCutoff: LocalDate = applicationConfig.startEmploymentDateFilteredBefore

  def employmentUrl(nino: Nino, id: String): String = s"$serviceUrl/tai/$nino/employments/$id"

  private def employmentOnlyUrl(nino: Nino, id: Int, taxYear: TaxYear): String =
    s"$serviceUrl/tai/$nino/employment-only/$id/years/${taxYear.year}"

  private def employmentsOnlyUrl(nino: Nino, taxYear: TaxYear): String =
    s"$serviceUrl/tai/$nino/employments-only/years/${taxYear.year}"

  private def endEmploymentServiceUrl(nino: Nino, id: Int): String =
    s"$serviceUrl/tai/$nino/employments/$id/end-date"

  def addEmploymentServiceUrl(nino: Nino): String =
    s"$serviceUrl/tai/$nino/employments"

  def employmentServiceUrl(nino: Nino, year: TaxYear): String =
    s"$serviceUrl/tai/$nino/employments/years/${year.year}"

  private def incorrectEmploymentServiceUrl(nino: Nino, id: Int): String =
    s"$serviceUrl/tai/$nino/employments/$id/reason"

  private def filterDate(dateOption: Option[LocalDate]): Option[LocalDate] =
    dateOption.filter(_.isAfter(startDateCutoff))

  private def sanitize(e: Employment): Employment =
    e.copy(startDate = filterDate(e.startDate))

  private def sanitizeAll(es: Seq[Employment]): Seq[Employment] =
    es.iterator.map(sanitize).toSeq

  def employments(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    httpHandler.getFromApiV2(employmentServiceUrl(nino, year)).map { json =>
      sanitizeAll((json \ "data" \ "employments").as[Seq[Employment]])
    }

  def employmentOnly(nino: Nino, id: Int, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Option[Employment]] =
    httpHandler.getFromApiV2(employmentOnlyUrl(nino, id, taxYear)).map { json =>
      (json \ "data").asOpt[Employment].map(sanitize)
    }

  def employmentsOnly(nino: Nino, taxYear: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[Employment]] = {
    val url = employmentsOnlyUrl(nino, taxYear)
    httpHandler
      .read(
        httpHandler.httpClient
          .get(url"$url")
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .map { httpResponse =>
        sanitizeAll((httpResponse.json \ "data" \ "employments").as[Seq[Employment]])
      }
  }

  def employment(nino: Nino, id: String)(implicit hc: HeaderCarrier): Future[Option[Employment]] =
    httpHandler.getFromApiV2(employmentUrl(nino, id)).map { json =>
      (json \ "data").asOpt[Employment].map(sanitize)
    }

  def endEmployment(nino: Nino, id: Int, endEmploymentData: EndEmployment)(implicit hc: HeaderCarrier): Future[String] =
    httpHandler.putToApi[EndEmployment](endEmploymentServiceUrl(nino, id), endEmploymentData).flatMap { response =>
      (response.json \ "data").validate[String].asEither match {
        case Right(envId) => Future.successful(envId)
        case Left(_)      => Future.failed(new RuntimeException("Invalid json"))
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
}
