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

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.{HasTaxCodeChanged, TaxCodeChange, TaxCodeMismatch}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TaxCodeChangeService {

  def taxCodeChangeConnector: TaxCodeChangeConnector

  def taxCodeChange(nino: Nino)(implicit hc: HeaderCarrier): Future[TaxCodeChange] = {
    taxCodeChangeConnector.taxCodeChange(nino) map {
      case TaiSuccessResponseWithPayload(taxCodeChange: TaxCodeChange) => taxCodeChange
      case _ => throw new RuntimeException("Could not fetch tax code change")
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
        case(_: Boolean, TaiSuccessResponseWithPayload(taxCodeMismatch: TaxCodeMismatch)) => {
          HasTaxCodeChanged(hasTaxCodeChanged, Some(taxCodeMismatch))
        }
        case(_: Boolean, _: TaiTaxAccountFailureResponse) => {
          HasTaxCodeChanged(changed = false, None)
        }
        case _ => throw new RuntimeException("Could not fetch has tax code changed")
      }
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

object TaxCodeChangeService extends TaxCodeChangeService {
  override val taxCodeChangeConnector: TaxCodeChangeConnector = TaxCodeChangeConnector
}
