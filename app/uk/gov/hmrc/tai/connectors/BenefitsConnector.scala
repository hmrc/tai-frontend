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
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, EndedCompanyBenefit}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BenefitsConnector @Inject() (httpHandler: HttpHandler) extends DefaultServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def benefitsUrl(nino: String, taxYear: Int): String = s"$serviceUrl/tai/$nino/tax-account/$taxYear/benefits"
  def endedCompanyBenefitUrl (nino: String, employmentId: Int) = s"$serviceUrl/tai/$nino/tax-account/tax-component/employments/$employmentId/benefits/ended-benefit"

  def benefits(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier): Future[Benefits] = {
      httpHandler.getFromApi(benefitsUrl(nino.nino, taxYear)) map (
        json =>
          (json \ "data" ).as[Benefits]
        ) recover {
        case _: RuntimeException => {
          Logger.warn(s"Couldn't retrieve benefits for nino: $nino")
          throw new RuntimeException(s"Couldn't retrieve benefits for nino: $nino")
        }
      }
  }

  def endedCompanyBenefit(nino: Nino, employmentId: Int, endedCompanyBenefit: EndedCompanyBenefit)(implicit hc: HeaderCarrier): Future[Option[String]]  = {
    httpHandler.postToApi[EndedCompanyBenefit](endedCompanyBenefitUrl(nino.nino, employmentId), endedCompanyBenefit).map { response =>
      (response.json \ "data").asOpt[String]
    }
  }

}
