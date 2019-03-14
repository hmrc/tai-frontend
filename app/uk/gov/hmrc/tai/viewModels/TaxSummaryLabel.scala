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

package uk.gov.hmrc.tai.viewModels

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.{CarBenefit, EstimatedTaxYouOweThisYear, TaxComponentType, UnderPaymentFromPreviousYear}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.CompanyCarMakeModel


case class TaxSummaryLabel(value: String, link: Option[HelpLink] = None)

case class HelpLink(value: String, href: String, id: String)


object TaxSummaryLabel {
  def apply(taxComponentType: TaxComponentType, employmentId: Option[Int], companyCarBenefits: Seq[CompanyCarBenefit], employmentIdNameMap: Map[Int, String])(implicit messages: Messages): TaxSummaryLabel = {

    val labelString = describe(taxComponentType,
      employmentId,
      companyCarBenefits,
      employmentIdNameMap)

    val labelLink = createLabelLink(taxComponentType)

    TaxSummaryLabel(labelString, labelLink)
  }

  private def createLabelLink(taxComponentType: TaxComponentType)(implicit messages: Messages): Option[HelpLink] = {
    taxComponentType match {
      case UnderPaymentFromPreviousYear =>
        val href = controllers.routes.UnderpaymentFromPreviousYearController.underpaymentExplanation.url.toString
        val id = "underPaymentFromPreviousYear"
        Some(HelpLink(Messages("what.does.this.mean"), href, id))

      case EstimatedTaxYouOweThisYear =>
        val href = controllers.routes.PotentialUnderpaymentController.potentialUnderpaymentPage.url.toString
        val id = "estimatedTaxOwedLink"
        Some(HelpLink(Messages("what.does.this.mean"), href, id))

      case _ =>
        None
    }
  }

  private def describe(componentType: TaxComponentType, employmentId: Option[Int], companyCarBenefits: Seq[CompanyCarBenefit], employmentIdNameMap: Map[Int, String])
  (implicit messages: Messages): String = {
    (componentType, employmentId) match {
      case (CarBenefit, Some(id)) if employmentIdNameMap.contains(id) =>

        val makeModel = CompanyCarMakeModel.description(id, companyCarBenefits).
          getOrElse(messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"))

        s"${messages("tai.taxFreeAmount.table.taxComponent.CarBenefitMakeModel", makeModel)}" + " " +
          s"${messages("tai.taxFreeAmount.table.taxComponent.from.employment", employmentIdNameMap(id))}"

      case (_, Some(id)) if employmentIdNameMap.contains(id) =>
        componentType.toMessage() + " " +
          s"${messages("tai.taxFreeAmount.table.taxComponent.from.employment", employmentIdNameMap(id))}"

      case _ =>
        messages(s"tai.taxFreeAmount.table.taxComponent.${componentType.toString}")
    }
  }
}
