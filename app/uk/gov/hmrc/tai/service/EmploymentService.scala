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

package uk.gov.hmrc.tai.service

import cats.data.EitherT
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.EmploymentsConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentService @Inject() (employmentsConnector: EmploymentsConnector)(implicit ec: ExecutionContext) {

  def employmentOnly(nino: Nino, id: Int, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Option[Employment]] =
    employmentsConnector.employmentOnly(nino, id, taxYear)

  def employmentsOnly(nino: Nino, taxYear: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[Employment]] =
    employmentsConnector.employmentsOnly(nino, taxYear)

  def endEmployment(nino: Nino, id: Int, endEmploymentData: EndEmployment)(implicit hc: HeaderCarrier): Future[String] =
    employmentsConnector.endEmployment(nino, id, endEmploymentData)

  def addEmployment(nino: Nino, employment: AddEmployment)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    employmentsConnector.addEmployment(nino, employment) map {
      case Some(envId) => envId
      case _           =>
        throw new RuntimeException(s"No envelope id was generated when adding the new employment for ${nino.nino}")
    }

  def incorrectEmployment(nino: Nino, id: Int, incorrectEmployment: IncorrectIncome)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    employmentsConnector.incorrectEmployment(nino, id, incorrectEmployment) map {
      case Some(envId) => envId
      case _           =>
        throw new RuntimeException(
          s"No envelope id was generated when sending incorrect employment details for ${nino.nino}"
        )
    }

  def employmentNames(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): Future[Map[Int, String]] =
    employmentsOnly(nino, year).value.map {
      case Right(employments) => employments.iterator.map(e => e.sequenceNumber -> e.name).toMap
      case Left(error)        =>
        throw new RuntimeException(s"No employment names found, error occurred with message ${error.message}")
    }
}
