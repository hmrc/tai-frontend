/*
 * Copyright 2019 HM Revenue & Customs
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

import com.google.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain.{AddEmployment, Employment, EndEmployment, IncorrectIncome}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.tai.model.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmploymentsConnector @Inject() (val httpHandler: HttpHandler) extends ServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def employmentUrl(nino: Nino, id: String) = s"$serviceUrl/tai/$nino/employments/$id"

  def employments(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[Employment]] = {
    httpHandler.getFromApi(employmentServiceUrl(nino, year)) map {
      json =>
        if ((json \ "data" \ "employments").validate[Seq[Employment]].isSuccess) {
          (json \ "data" \ "employments").as[Seq[Employment]]
        } else {
          throw new RuntimeException("Invalid employment json")
        }
    }
  }

  def employment(nino: Nino, id: String)(implicit hc: HeaderCarrier): Future[Option[Employment]] =
    httpHandler.getFromApi(employmentUrl(nino, id)).map(
      json => ((json \ "data").asOpt[Employment])
    )

  def endEmployment(nino: Nino, id: Int, endEmploymentData: EndEmployment)(implicit hc: HeaderCarrier): Future[String] = {
    httpHandler.putToApi[EndEmployment](endEmploymentServiceUrl(nino, id), endEmploymentData).map { response =>
        if((response.json \ "data").validate[String].isSuccess){
          (response.json \ "data").as[String]
        } else {
          throw new RuntimeException("Invalid json")
        }
    }
  }

  def addEmployment(nino: Nino, employment: AddEmployment)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    httpHandler.postToApi[AddEmployment](addEmploymentServiceUrl(nino), employment).map { response =>
      (response.json \ "data").asOpt[String]
    }
  }

  def incorrectEmployment(nino: Nino, id: Int, incorrectEmployment: IncorrectIncome)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    httpHandler.postToApi[IncorrectIncome](incorrectEmploymentServiceUrl(nino, id), incorrectEmployment).map { response =>
      (response.json \ "data").asOpt[String]
    }
  }


  def endEmploymentServiceUrl(nino: Nino, id: Int) = s"$serviceUrl/tai/$nino/employments/$id/end-date"

  def addEmploymentServiceUrl(nino: Nino) = s"$serviceUrl/tai/$nino/employments"

  def employmentServiceUrl(nino: Nino, year: TaxYear) = s"$serviceUrl/tai/$nino/employments/years/${year.year}"

  def incorrectEmploymentServiceUrl(nino: Nino, id: Int) = s"$serviceUrl/tai/$nino/employments/$id/reason"
}
