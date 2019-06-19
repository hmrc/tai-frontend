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

package uk.gov.hmrc.tai.service.benefits

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.BenefitsConnector
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, EndedCompanyBenefit}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BenefitsService  @Inject() (benefitsConnector: BenefitsConnector) {

  def benefits(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier): Future[Benefits] = {
    benefitsConnector.benefits(nino, taxYear)
  }

  def endedCompanyBenefit(nino: Nino, employmentId: Int, endedCompanyBenefit: EndedCompanyBenefit)(implicit hc: HeaderCarrier): Future[String]  = {
    benefitsConnector.endedCompanyBenefit(nino, employmentId, endedCompanyBenefit) map {
      case Some(envId) => envId
      case _ => throw new RuntimeException(s"No envelope id was generated when attempting to end company benefit for ${nino.nino}")
    }
  }
}
