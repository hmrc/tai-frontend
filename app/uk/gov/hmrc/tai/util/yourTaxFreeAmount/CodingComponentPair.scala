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
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.{CarBenefit, TaxComponentType}

case class CodingComponentPair(componentType: TaxComponentType, employmentId: Option[Int], previous: BigDecimal, current: BigDecimal) {

  def description(employmentIds: Map[Int, String], companyCarBenefits: Seq[CompanyCarBenefit])(implicit messages: Messages): String = {
    (componentType, employmentId) match {
      case (CarBenefit, Some(id)) if employmentIds.contains(id) =>
        val makeModel = CompanyCarMakeModel.description(id, companyCarBenefits).getOrElse(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"))

        s"${Messages("tai.taxFreeAmount.table.taxComponent.CarBenefitMakeModel", makeModel)}" + " " +
          s"${Messages("tai.taxFreeAmount.table.taxComponent.from.employment", employmentIds(id))}"

      case (_, Some(id)) if employmentIds.contains(id) =>
        s"${Messages(s"tai.taxFreeAmount.table.taxComponent.${componentType.toString}")}" + " " +
          s"${Messages("tai.taxFreeAmount.table.taxComponent.from.employment", employmentIds(id))}"

      case _ =>
        Messages(s"tai.taxFreeAmount.table.taxComponent.${componentType.toString}")
    }
  }
}
