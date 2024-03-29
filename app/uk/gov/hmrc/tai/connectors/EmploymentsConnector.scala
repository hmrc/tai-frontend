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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{AddEmployment, Employment, EndEmployment, IncorrectIncome}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentsConnector @Inject() (httpHandler: HttpHandler, servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def employmentUrl(nino: Nino, id: String): String = s"$serviceUrl/tai/$nino/employments/$id"

  def employments(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    httpHandler.getFromApiV2(employmentServiceUrl(nino, year)) map { json =>
      if ((json \ "data" \ "employments").validate[Seq[Employment]].isSuccess) {
        (json \ "data" \ "employments").as[Seq[Employment]]
      } else {
        throw new RuntimeException("Invalid employment json")
      }
    }

  def ceasedEmployments(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    httpHandler.getFromApiV2(ceasedEmploymentServiceUrl(nino, year)).map { json =>
      (json \ "data").validate[Seq[Employment]].recoverTotal(_ => throw new RuntimeException("Invalid employment json"))
    }

  def employment(nino: Nino, id: String)(implicit hc: HeaderCarrier): Future[Option[Employment]] =
    httpHandler
      .getFromApiV2(employmentUrl(nino, id))
      .map(json => (json \ "data").asOpt[Employment])

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
