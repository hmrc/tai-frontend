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
import cats.implicits._
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxCodeChangeService @Inject() (
  taxCodeChangeConnector: TaxCodeChangeConnector,
  implicit val executionContext: ExecutionContext
) extends Logging {

  def taxCodeChange(nino: Nino)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, TaxCodeChange] =
    taxCodeChangeConnector.taxCodeChange(nino)

  def hasTaxCodeChanged(
    nino: Nino
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Boolean] =
    taxCodeChangeConnector
      .hasTaxCodeChanged(nino)

  def lastTaxCodeRecordsInYearPerEmployment(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): Future[List[TaxCodeRecord]] =
    taxCodeChangeConnector.lastTaxCodeRecords(nino, year)

  def hasTaxCodeRecordsInYearPerEmployment(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): Future[Boolean] =
    taxCodeChangeConnector.lastTaxCodeRecords(nino, year).attemptT.map(_.nonEmpty).getOrElse(false)

}
