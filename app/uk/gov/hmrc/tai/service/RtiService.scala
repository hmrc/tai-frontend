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
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.RtiConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RtiService @Inject() (rtiConnector: RtiConnector)(implicit ec: ExecutionContext) extends Logging {

  def getAllPaymentsForYear(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[AnnualAccount]] =
    rtiConnector.getPaymentsAllPaymentsForYear(nino, year)

  def getPaymentsForEmploymentAndYear(nino: Nino, year: TaxYear, empId: Int)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Option[AnnualAccount]] =
    rtiConnector.getPaymentsAllPaymentsForYear(nino, year).transform {
      case Right(payments) =>
        val paymentsForEmployment = payments.filter(_.sequenceNumber == empId)
        if (paymentsForEmployment.isEmpty) {
          Right(None)
        } else {
          if (paymentsForEmployment.length > 1) {
            val ex = new RuntimeException(
              s"There is more than one annual account for nino: $nino, year: ${year.year} and employment id: $empId"
            )
            logger.error(ex.getMessage, ex)
          }
          Right(paymentsForEmployment.headOption)
        }
      case Left(error)     => Left(error)
    }

}
