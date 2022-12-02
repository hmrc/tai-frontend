/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{HasTaxCodeChanged, TaxCodeChange, TaxCodeMismatch, TaxCodeRecord}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxCodeChangeService @Inject()(taxCodeChangeConnector: TaxCodeChangeConnector) extends Logging {

  def taxCodeChange(nino: Nino)(implicit hc: HeaderCarrier): Future[TaxCodeChange] =
    taxCodeChangeConnector.taxCodeChange(nino)

  def hasTaxCodeChanged(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[TaxCodeError, HasTaxCodeChanged]] = {
    val hasTaxCodeChangedFuture = taxCodeChanged(nino)
    val taxCodeMismatchFuture = taxCodeMismatch(nino)

    val hasTaxChanged = for {
      hasTaxCodeChanged <- EitherT(hasTaxCodeChangedFuture)
      taxCodeMismatch   <- EitherT.right[TaxCodeError](taxCodeMismatchFuture)
    } yield
      taxCodeMismatch match {
        case TaiSuccessResponseWithPayload(taxCodeMismatch: TaxCodeMismatch) =>
          logger.debug(s"TCMismatch $taxCodeMismatch")
          HasTaxCodeChanged(hasTaxCodeChanged, Some(taxCodeMismatch))
        case _: TaiTaxAccountFailureResponse =>
          HasTaxCodeChanged(changed = false, None)
      }
    hasTaxChanged.leftMap { _ =>
      logger.error("Could not fetch the changed tax code")
      TaxCodeError(nino, Some("Could not fetch the changed tax code"))
    }.value
  }

  def lastTaxCodeRecordsInYearPerEmployment(nino: Nino, year: TaxYear)(
    implicit hc: HeaderCarrier): Future[List[TaxCodeRecord]] =
    taxCodeChangeConnector.lastTaxCodeRecords(nino, year)

  def hasTaxCodeRecordsInYearPerEmployment(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Boolean] =
    taxCodeChangeConnector.lastTaxCodeRecords(nino, year).map {
      case taxCodeRecords: List[TaxCodeRecord] if taxCodeRecords.nonEmpty => true
      case _                                                              => false
    } recover {
      case e => false
    }

  def latestTaxCodeChangeDate(nino: Nino)(implicit hc: HeaderCarrier): Future[LocalDate] =
    taxCodeChange(nino).map(_.mostRecentTaxCodeChangeDate)

  private def taxCodeMismatch(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    taxCodeChangeConnector.taxCodeMismatch(nino)

  private def taxCodeChanged(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[TaxCodeError, Boolean]] =
    taxCodeChangeConnector.hasTaxCodeChanged(nino) map {
      case TaiSuccessResponseWithPayload(hasTaxCodeChanged: Boolean) => Right(hasTaxCodeChanged)
      case _                                                         => Left(TaxCodeError(nino, Some("Could not fetch tax code change")))
    }
}
