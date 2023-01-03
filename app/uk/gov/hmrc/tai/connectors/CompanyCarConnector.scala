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

package uk.gov.hmrc.tai.connectors

import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class CompanyCarConnector @Inject()(httpHandler: HttpHandler, servicesConfig: ServicesConfig)(
  implicit ec: ExecutionContext)
    extends Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def companyCarEmploymentUrl(nino: Nino, empId: Int): String =
    s"$serviceUrl/tai/$nino/tax-account/tax-components/employments/$empId/benefits/company-car"
  def companyCarUrl(nino: Nino): String = s"$serviceUrl/tai/$nino/tax-account/tax-components/benefits/company-cars"

  def companyCarBenefitForEmployment(nino: Nino, empId: Int)(
    implicit hc: HeaderCarrier): Future[Option[CompanyCarBenefit]] =
    httpHandler.getFromApiV2(companyCarEmploymentUrl(nino, empId)) map (
      json => Some((json \ "data").as[CompanyCarBenefit])
    ) recover {
      case e: NotFoundException =>
        logger.warn(s"Couldn't retrieve company car benefits for nino: $nino employmentId:$empId")
        None
    }

  def companyCarsForCurrentYearEmployments(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[CompanyCarBenefit]] =
    httpHandler.getFromApiV2(companyCarUrl(nino)) map (
      json => (json \ "data" \ "companyCarBenefits").as[Seq[CompanyCarBenefit]]
    ) recover {
      case NonFatal(_) =>
        logger.warn(s"Couldn't retrieve company car benefits for nino: $nino")
        Seq.empty[CompanyCarBenefit]
    }

}
