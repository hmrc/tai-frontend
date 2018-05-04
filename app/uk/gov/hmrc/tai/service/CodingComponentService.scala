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

import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaxAccountConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CodingComponentService {

  def taxAccountConnector: TaxAccountConnector

  def taxFreeAmountComponents(nino: Nino, year: TaxYear)(implicit hc: HeaderCarrier): Future[Seq[CodingComponent]] = {
    taxAccountConnector.codingComponents(nino, year) map {
      case TaiSuccessResponseWithPayload(codingComponents: Seq[CodingComponent]) => filterOutZeroAmountsComponents(codingComponents)
      case TaiTaxAccountFailureResponse(e) => throw new RuntimeException(e)
    }recover {
      case e: Exception =>
       throw new RuntimeException("Couldn't retrieve coding components")

    }
  }

  private def filterOutZeroAmountsComponents(codingComponents: Seq[CodingComponent]) = {
    codingComponents.filter {
      case component if component.amount == 0 => false
      case _ => true
    }
  }
}
// $COVERAGE-OFF$
object CodingComponentService extends CodingComponentService {
  override val taxAccountConnector: TaxAccountConnector = TaxAccountConnector
}
// $COVERAGE-ON$
