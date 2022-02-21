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

package uk.gov.hmrc.tai.connectors

import javax.inject.Inject
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeMismatch, TaxCodeRecord}

import scala.concurrent.{ExecutionContext, Future}

class TaxCodeChangeConnector @Inject()(httpHandler: HttpHandler, servicesConfig: ServicesConfig)(
  implicit ec: ExecutionContext)
    extends Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def baseTaxAccountUrl(nino: String) = s"$serviceUrl/tai/$nino/tax-account/"

  def taxCodeChangeUrl(nino: String): String = baseTaxAccountUrl(nino) + "tax-code-change"

  def taxCodeChange(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    httpHandler.getFromApiV2(taxCodeChangeUrl(nino.nino)) map (
      json => TaiSuccessResponseWithPayload((json \ "data").as[TaxCodeChange])
    ) recover {
      case e: Exception =>
        logger.warn(s"Couldn't retrieve tax code change for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }

  def hasTaxCodeChangedUrl(nino: String): String = baseTaxAccountUrl(nino) + "tax-code-change/exists"

  def hasTaxCodeChanged(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    httpHandler.getFromApiV2(hasTaxCodeChangedUrl(nino.nino)) map (
      json => TaiSuccessResponseWithPayload(json.as[Boolean])
    ) recover {
      case e: Exception =>
        logger.warn(s"Couldn't retrieve tax code changed for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }

  def taxCodeMismatchUrl(nino: String): String = baseTaxAccountUrl(nino) + "tax-code-mismatch"

  def taxCodeMismatch(nino: Nino)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    httpHandler.getFromApiV2(taxCodeMismatchUrl(nino.nino)) map (
      json => TaiSuccessResponseWithPayload((json \ "data").as[TaxCodeMismatch])
    ) recover {
      case e: Exception =>
        logger.warn(s"Couldn't retrieve tax code mismatch for $nino with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }

  def lastTaxCodeRecordsUrl(nino: String, year: Int): String = baseTaxAccountUrl(nino) + s"$year/tax-code/latest"

  def lastTaxCodeRecords(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    httpHandler.getFromApiV2(lastTaxCodeRecordsUrl(nino.nino, year.year)) map (
      json => {
        TaiSuccessResponseWithPayload((json \ "data").as[List[TaxCodeRecord]])
      }
    ) recover {
      case e: Exception =>
        logger.warn(s"Couldn't retrieve tax code records for $nino for year $year with exception:${e.getMessage}")
        TaiTaxAccountFailureResponse(e.getMessage)
    }

}
