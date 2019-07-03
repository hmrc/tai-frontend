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

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeMismatch, TaxCodeRecord}

import scala.concurrent.Future

class TaxCodeChangeConnector @Inject() (httpHandler: HttpHandler) extends DefaultServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def baseTaxAccountUrl(nino: String) = s"$serviceUrl/tai/$nino/tax-account/"

  def taxCodeChangeUrl(nino: String): String = baseTaxAccountUrl(nino) + "tax-code-change"

  def taxCodeChange(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(taxCodeChangeUrl(nino.nino)) map (
      json =>
        TaiSuccessResponseWithPayload((json \ "data").as[TaxCodeChange])
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve tax code change for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

  def hasTaxCodeChangedUrl(nino: String): String = baseTaxAccountUrl(nino) + "tax-code-change/exists"

  def hasTaxCodeChanged(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(hasTaxCodeChangedUrl(nino.nino)) map (
      json => TaiSuccessResponseWithPayload(json.as[Boolean])
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve tax code changed for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

  def taxCodeMismatchUrl(nino: String): String = baseTaxAccountUrl(nino) + "tax-code-mismatch"

  def taxCodeMismatch(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(taxCodeMismatchUrl(nino.nino)) map (
      json => TaiSuccessResponseWithPayload((json \ "data").as[TaxCodeMismatch])
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve tax code mismatch for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

  def lastTaxCodeRecordsUrl(nino: String, year: Int): String = baseTaxAccountUrl(nino) + s"$year/tax-code/latest"

  def lastTaxCodeRecords(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.getFromApi(lastTaxCodeRecordsUrl(nino.nino, year.year)) map (
      json => {
        TaiSuccessResponseWithPayload((json \ "data").as[Seq[TaxCodeRecord]])
      }
      ) recover {
      case e: Exception =>
        Logger.warn(s"Couldn't retrieve tax code records for $nino for year $year with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }
  }

}
