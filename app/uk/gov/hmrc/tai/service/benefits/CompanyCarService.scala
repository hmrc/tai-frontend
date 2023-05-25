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

import cats.data.EitherT

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.CompanyCarConnector
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.CarBenefit

import scala.concurrent.{ExecutionContext, Future}

class CompanyCarService @Inject() (carConnector: CompanyCarConnector) {

  def companyCars(
    nino: Nino
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, Seq[CompanyCarBenefit]] =
    carConnector.companyCarsForCurrentYearEmployments(nino).map(_.json.as[Seq[CompanyCarBenefit]])

  def companyCarOnCodingComponents(nino: Nino, codingComponents: Seq[CodingComponent])(
    implicit // TODO - Delete?
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Seq[CompanyCarBenefit]] = {
    if (codingComponents.exists(_.componentType == CarBenefit)) {
      companyCars(nino).getOrElse(Seq.empty[CompanyCarBenefit])
    } else {
      Future.successful(Seq.empty[CompanyCarBenefit])
    }
  }
}
