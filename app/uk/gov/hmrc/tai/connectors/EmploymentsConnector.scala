/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{AddEmployment, Employment, EndEmployment, IncorrectIncome}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmploymentsConnector @Inject()(httpHandler: HttpHandler) extends DefaultServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def employmentUrl(nino: Nino, id: String) = s"$serviceUrl/tai/$nino/employments/$id"

  def employments(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    httpHandler.getFromApiv2(employmentServiceUrl(nino, year)).map {
      case Right(json) =>
        (json \ "data" \ "employments").validate[Seq[Employment]] match {
          case JsSuccess(value, _) => value
          case JsError(_)          => throw new RuntimeException("Invalid employment json")
        }
      case Left(_) => throw new UnauthorizedException("Retrieve employments returned 401")
    }

  def ceasedEmployments(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    httpHandler.getFromApiv2(ceasedEmploymentServiceUrl(nino, year)).map {
      case Right(json) =>
        (json \ "data").validate[Seq[Employment]] match {
          case JsSuccess(value, _) => value
          case JsError(_)          => throw new RuntimeException("Invalid employment json")
        }
      case Left(_) => throw new UnauthorizedException("Retrieve employments returned 401")
    }

  def employment(nino: Nino, id: String)(implicit hc: HeaderCarrier): Future[Option[Employment]] =
    httpHandler.getFromApiv2(employmentUrl(nino, id)).map {
      case Right(json) => (json \ "data").asOpt[Employment]
      case Left(_)     => None
    }

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

  def incorrectEmployment(nino: Nino, id: Int, incorrectEmployment: IncorrectIncome)(
    implicit hc: HeaderCarrier): Future[Option[String]] =
    httpHandler.postToApi[IncorrectIncome](incorrectEmploymentServiceUrl(nino, id), incorrectEmployment).map {
      response =>
        (response.json \ "data").asOpt[String]
    }

  def endEmploymentServiceUrl(nino: Nino, id: Int) = s"$serviceUrl/tai/$nino/employments/$id/end-date"

  def addEmploymentServiceUrl(nino: Nino) = s"$serviceUrl/tai/$nino/employments"

  def employmentServiceUrl(nino: Nino, year: TaxYear) = s"$serviceUrl/tai/$nino/employments/years/${year.year}"

  def ceasedEmploymentServiceUrl(nino: Nino, year: TaxYear) =
    s"$serviceUrl/tai/$nino/employments/year/${year.year}/status/ceased"

  def incorrectEmploymentServiceUrl(nino: Nino, id: Int) = s"$serviceUrl/tai/$nino/employments/$id/reason"
}
