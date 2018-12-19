/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain.{AddEmployment, Employment, EndEmployment, IncorrectIncome}
import uk.gov.hmrc.tai.connectors.EmploymentsConnector
import uk.gov.hmrc.tai.model.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait EmploymentService {

  def connector: EmploymentsConnector

  def employments(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[Employment]] = {
    connector.employments(nino, year)
  }

  def employment(nino: Nino, id: Int)(implicit hc: HeaderCarrier): Future[Option[Employment]] = {
    connector.employment(nino, id.toString)
  }

  def endEmployment(nino: Nino, id: Int, endEmploymentData: EndEmployment)(implicit hc: HeaderCarrier): Future[String] = {
    connector.endEmployment(nino, id, endEmploymentData)
  }

  def addEmployment(nino: Nino, employment: AddEmployment)(implicit hc:HeaderCarrier): Future[String] = {
    connector.addEmployment(nino, employment) map {
      case Some(envId) => envId
      case _ => throw new RuntimeException(s"No envelope id was generated when adding the new employment for ${nino.nino}")
    }
  }

  def incorrectEmployment(nino: Nino, id: Int, incorrectEmployment: IncorrectIncome)(implicit hc: HeaderCarrier): Future[String] = {
    connector.incorrectEmployment(nino, id, incorrectEmployment) map {
      case Some(envId) => envId
      case _ => throw new RuntimeException(s"No envelope id was generated when sending incorrect employment details for ${nino.nino}")
    }
  }

  def employmentNames(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Map[Int, String]] =  {
     for{
       employments <- employments(nino, year)
     } yield {
       employments.map (employment => employment.sequenceNumber -> employment.name).toMap
     }
  }
}
// $COVERAGE-OFF$
object EmploymentService extends EmploymentService{
  override lazy val connector: EmploymentsConnector = EmploymentsConnector
}
// $COVERAGE-ON$

