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

package uk.gov.hmrc.tai.service

import cats.data.EitherT
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.EmploymentsConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentService @Inject() (employmentsConnector: EmploymentsConnector) {

  def employments(nino: Nino, year: TaxYear)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[Employment]] =
    employmentsConnector.employments(nino, year).map(_.json.as[Seq[Employment]]) /// TODO - Consider .validate()

  def ceasedEmployments(nino: Nino, year: TaxYear)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[Employment]] =
    employmentsConnector.ceasedEmployments(nino, year).map(_.json.as[Seq[Employment]])

  def employment(nino: Nino, id: Int)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Option[Employment]] =
    employmentsConnector
      .employment(nino, id.toString)
      .map(_.json.asOpt[Employment]) // TODO - Merge this with employments()

  def endEmployment(nino: Nino, id: Int, endEmploymentData: EndEmployment)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, String] =
    employmentsConnector.endEmployment(nino, id, endEmploymentData).map(_.json.as[String])

  def addEmployment(nino: Nino, employment: AddEmployment)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, String] =
    employmentsConnector.addEmployment(nino, employment).map(_.json.as[String])
  def incorrectEmployment(nino: Nino, id: Int, incorrectEmployment: IncorrectIncome)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, String] =
    employmentsConnector.incorrectEmployment(nino, id, incorrectEmployment).map(_.json.as[String])

  def employmentNames(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Map[Int, String]] =
    employments(nino, year).fold(
      _ => Map.empty[Int, String],
      employments => employments.map(employment => employment.sequenceNumber -> employment.name).toMap
    )
}
