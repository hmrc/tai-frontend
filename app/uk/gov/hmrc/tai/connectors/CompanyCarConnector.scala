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
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponse}
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCarBenefit, WithdrawCarAndFuel}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class CompanyCarConnector @Inject() (httpHandler: HttpHandler) extends DefaultServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def companyCarEmploymentUrl(nino: Nino, empId: Int): String = s"$serviceUrl/tai/$nino/tax-account/tax-components/employments/$empId/benefits/company-car"
  def companyCarUrl(nino: Nino): String = s"$serviceUrl/tai/$nino/tax-account/tax-components/benefits/company-cars"

  def companyCarBenefitForEmployment(nino: Nino, empId: Int)(implicit hc: HeaderCarrier): Future[Option[CompanyCarBenefit]] = {
    httpHandler.getFromApi(companyCarEmploymentUrl(nino, empId)) map (
      json =>
        Some((json \ "data").as[CompanyCarBenefit])
      ) recover {
      case e:NotFoundException => {
        Logger.warn(s"Couldn't retrieve company car benefits for nino: $nino employmentId:$empId")
        None
      }
    }
  }

  def withdrawCompanyCarAndFuel(nino: Nino, employmentSeqNum: Int, carSeqNum: Int, withdrawCarAndFuel: WithdrawCarAndFuel)
                               (implicit hc: HeaderCarrier): Future[TaiResponse] = {
    val withdrawUrl = s"${companyCarEmploymentUrl(nino, employmentSeqNum)}/$carSeqNum/withdrawn"
    httpHandler.putToApi(withdrawUrl,withdrawCarAndFuel) map (_ => TaiSuccessResponse)
  }

  def companyCarsForCurrentYearEmployments(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[CompanyCarBenefit]] = {
    httpHandler.getFromApi(companyCarUrl(nino)) map (
      json =>
        (json \ "data" \ "companyCarBenefits").as[Seq[CompanyCarBenefit]]
      ) recover {
      case NonFatal(_) => {
        Logger.warn(s"Couldn't retrieve company car benefits for nino: $nino")
        Seq.empty[CompanyCarBenefit]
      }
    }
  }

}
