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

case class CarGrossAmountPairs(grossAmount: BigDecimal, carDescription: String)

case class CompanyCarBenefitPairs(previous: Option[CarGrossAmountPairs], current: Option[CarGrossAmountPairs])

object CompanyCarBenefitPairs {
  def apply(employmentIds: Map[Int, String],
            previous: CodingComponentsWithCarBenefits,
            current: CodingComponentsWithCarBenefits)
           (implicit messages: Messages): CompanyCarBenefitPairs = {
    val previousPair = makePair(employmentIds, previous.codingComponents, previous.companyCarBenefits)
    val currentPair = makePair(employmentIds, current.codingComponents, current.companyCarBenefits)

    CompanyCarBenefitPairs(previousPair, currentPair)
  }

  private def makePair(employmentIds: Map[Int, String], codingCommponent: Seq[CodingComponent],
                       companyCarBenefits: Seq[CompanyCarBenefit])(implicit messages: Messages): Option[CarGrossAmountPairs] = {
    val carBenefitCodingComponent = codingCommponent.find(_.componentType == CarBenefit)

    if (carBenefitCodingComponent.isEmpty || companyCarBenefits.isEmpty) {
      None
    } else {
      val currentCarDescription = getCarDescription(employmentIds, carBenefitCodingComponent, companyCarBenefits)
      val currentGrossAmount: BigDecimal = carBenefitCodingComponent.map(_.amount).getOrElse(0)
      Some(CarGrossAmountPairs(currentGrossAmount, currentCarDescription))
    }
  }

  private def getCarDescription(employmentIds: Map[Int, String],
                                codingComponents: Option[CodingComponent],
                                companyCarBenefits: Seq[CompanyCarBenefit])
                               (implicit messages: Messages): String = {
    val carDescription = codingComponents match {
      case Some(CodingComponent(_, Some(id), _, _, _)) if employmentIds.contains(id) =>
        CompanyCarMakeModel.description(id, companyCarBenefits)
      case _ => None
    }

    carDescription.getOrElse(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"))
  }
}
