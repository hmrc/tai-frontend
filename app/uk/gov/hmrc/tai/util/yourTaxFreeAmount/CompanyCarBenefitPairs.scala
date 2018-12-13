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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.CarBenefit
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

case class CarBenefitAmount(grossAmount: BigDecimal, companyCarDescription: String)

case class CompanyCarBenefitPairs(thing: CarBenefitAmount)

object CompanyCarBenefitPairs {
  def apply(employmentIds: Map[Int, String],
            previousCarBenefitCodingComponent: CodingComponent,
            currentCarBenefitCodingComponent: CodingComponent,
            previousCompanyCarBenefits: Seq[CompanyCarBenefit],
            currentCompanyCarBenefits: Seq[CompanyCarBenefit])
           (implicit messages: Messages): CarBenefitAmount = {

    // assuming previous and current employment Ids are equal
    val employmentId = currentCarBenefitCodingComponent.employmentId.get

    val makeModel = companyCarForEmployment(employmentId, currentCompanyCarBenefits).getOrElse(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"))

    val text =
      s"""${Messages("tai.taxFreeAmount.table.taxComponent.CarBenefitMakeModel", makeModel)}
         |${Messages("tai.taxFreeAmount.table.taxComponent.from.employment", employmentId)}""".stripMargin

    val grossAmount = currentCarBenefitCodingComponent.amount

    CarBenefitAmount(grossAmount, text)
  }

  private def companyCarForEmployment(employmentId: Int, companyCarBenefits: Seq[CompanyCarBenefit]): Option[String] =
    for {
      carBenefits <- companyCarBenefits.find(_.employmentSeqNo == employmentId)
      model <- carBenefits.companyCars.headOption.map(_.makeModel)
    } yield model

}
