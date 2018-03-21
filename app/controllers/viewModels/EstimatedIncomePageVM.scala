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

package controllers.viewModels

import uk.gov.hmrc.tai.connectors.DomainConnector
import controllers._
import controllers.auth.TaiUser
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.viewModels.EstimatedIncomeViewModel
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.urls.Link

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.{PersonalTaxSummaryContainer, TaxSummaryDetails}

trait EstimatedIncomePageVMBuilder {
  def domainConnector: DomainConnector

  def createObject(nino: Nino, details: TaxSummaryDetails)(implicit user: TaiUser, hc: HeaderCarrier): Future[EstimatedIncomeViewModel] = {

    // Define the links that are relative to the front-end application.
    val links = Map(
      "marriageAllowance" -> Link.toInternalPage(
        url = routes.YourTaxCodeController.taxCodes().toString,
        value = Some(Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText"))
      ).toHtml.body,
      "maintenancePayments" -> routes.YourTaxCodeController.taxCodes().url,
      "taxExplanationScreen" -> Link.toInternalPage(
        url = routes.TaxExplanationController.taxExplanationPage().toString,
        value = Some(Messages("tai.mergedTaxBand.description")),
        id = Some("taxExplanation")
      ).toHtml.body,
      "underpaymentEstimatePageUrl" -> routes.PotentialUnderpaymentController.potentialUnderpaymentPage().url
    )

    // Request to build the domain model.
    DomainBuilderErrorHandler.errorWrapper {
      domainConnector.buildEstimatedIncomeView(nino, PersonalTaxSummaryContainer(details, links))
    }
  }

  def removeDecimalsToString(decimal: math.BigDecimal): String = {
    decimal.bigDecimal.stripTrailingZeros.toPlainString
  }
}

object EstimatedIncomePageVM extends EstimatedIncomePageVMBuilder {
  override def domainConnector: DomainConnector = DomainConnector
}