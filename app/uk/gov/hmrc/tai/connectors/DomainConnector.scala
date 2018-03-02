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

package uk.gov.hmrc.tai.connectors

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{CorePost, HeaderCarrier}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.tai.config.WSHttp
import uk.gov.hmrc.tai.model.PersonalTaxSummaryContainer
import uk.gov.hmrc.tai.viewModels.EstimatedIncomeViewModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DomainConnector extends RawResponseReads {
  def http: CorePost

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def buildEstimatedIncomeView(nino:Nino, container : PersonalTaxSummaryContainer)(implicit hc: HeaderCarrier): Future[EstimatedIncomeViewModel] = {
    val postUrl = url(s"/personal-tax/${nino.value}/buildestimatedincome")
    http.POST[PersonalTaxSummaryContainer, EstimatedIncomeViewModel](postUrl, container)
  }

}

object DomainConnector extends DomainConnector with ServicesConfig {

  lazy val serviceUrl = baseUrl("personal-tax-summary")
  override def http = WSHttp
}
