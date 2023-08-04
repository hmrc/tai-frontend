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

package uk.gov.hmrc.tai.service.benefits

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.CompanyCarConnector
import uk.gov.hmrc.tai.model.domain.CarBenefit
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyCarService @Inject() (carConnector: CompanyCarConnector) {

  def companyCarOnCodingComponents(nino: Nino, codingComponents: Seq[CodingComponent])(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Seq[CompanyCarBenefit]] =
    if (codingComponents.exists(_.componentType == CarBenefit))
      companyCars(nino)
    else
      Future.successful(Seq.empty[CompanyCarBenefit])

  def companyCars(
    nino: Nino
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Seq[CompanyCarBenefit]] =
    carConnector.companyCarsForCurrentYearEmployments(nino).map(_.filterNot(isCompanyCarDateWithdrawn))

  def isCompanyCarDateWithdrawn(companyCarBenefit: CompanyCarBenefit): Boolean =
    companyCarBenefit.companyCars.exists(_.dateWithdrawn.isDefined)

}
