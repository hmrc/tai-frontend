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

package uk.gov.hmrc.tai.service

import javax.inject.Inject
import org.joda.time.LocalDate
import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{HasTaxCodeChanged, TaxCodeChange, TaxCodeMismatch, TaxCodeRecord}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxCodeChangeService @Inject() (taxCodeChangeConnector: TaxCodeChangeConnector) {

  def taxCodeChange(nino: Nino)(implicit hc: HeaderCarrier): Future[TaxCodeChange] = {

    taxCodeChangeConnector.taxCodeChange(nino) map {
      case TaiSuccessResponseWithPayload(taxCodeChange: TaxCodeChange) => taxCodeChange
      case _ => throw new RuntimeException(s"Could not fetch tax code change")
    }
  }

  def hasTaxCodeChanged(nino: Nino)(implicit hc: HeaderCarrier): Future[HasTaxCodeChanged] = {
    val hasTaxCodeChangedFuture: Future[Boolean] = taxCodeChanged(nino)
    val taxCodeMismatchFuture: Future[TaiResponse] = taxCodeMismatch(nino)

    for {
      hasTaxCodeChanged <- hasTaxCodeChangedFuture
      taxCodeMismatch <- taxCodeMismatchFuture
    } yield {
      (hasTaxCodeChanged, taxCodeMismatch) match {
        case (_: Boolean, TaiSuccessResponseWithPayload(taxCodeMismatch: TaxCodeMismatch)) => {
          Logger.debug(s"TCMismatch $taxCodeMismatch")
          HasTaxCodeChanged(hasTaxCodeChanged, Some(taxCodeMismatch))
        }
        case (_: Boolean, _: TaiTaxAccountFailureResponse) => {
          HasTaxCodeChanged(changed = false, None)
        }
        case _ => throw new RuntimeException("Could not fetch has tax code changed")
      }
    }
  }

  def lastTaxCodeRecordsInYearPerEmployment(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[TaxCodeRecord]] = {
    taxCodeChangeConnector.lastTaxCodeRecords(nino, year) map {
      case TaiSuccessResponseWithPayload(taxCodeRecords: Seq[TaxCodeRecord]) => taxCodeRecords
      case TaiTaxAccountFailureResponse(_) => throw new RuntimeException(s"Could not fetch last tax code records for year $year")
    }
  }

  def hasTaxCodeRecordsInYearPerEmployment(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Boolean] = {
    taxCodeChangeConnector.lastTaxCodeRecords(nino, year) map {
      case TaiSuccessResponseWithPayload(taxCodeRecords: Seq[TaxCodeRecord]) if taxCodeRecords.nonEmpty => true
      case _ => false
    }
  }

  def latestTaxCodeChangeDate(nino: Nino)(implicit hc: HeaderCarrier): Future[LocalDate] = {
    taxCodeChange(nino).map(_.mostRecentTaxCodeChangeDate)
  }

  private def taxCodeMismatch(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    taxCodeChangeConnector.taxCodeMismatch(nino)
  }

  private def taxCodeChanged(nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] = {
    taxCodeChangeConnector.hasTaxCodeChanged(nino) map {
      case TaiSuccessResponseWithPayload(hasTaxCodeChanged: Boolean) => hasTaxCodeChanged
      case _ => throw new RuntimeException("Could not fetch tax code change")
    }
  }
}