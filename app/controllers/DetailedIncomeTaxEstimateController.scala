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

package controllers

import cats.implicits._
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.{CodingComponentService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.DetailedIncomeTaxEstimateViewModel
import views.html.estimatedIncomeTax.DetailedIncomeTaxEstimateView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

@Singleton
class DetailedIncomeTaxEstimateController @Inject()(
  taxAccountService: TaxAccountService,
  codingComponentService: CodingComponentService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  detailedIncomeTaxEstimate: DetailedIncomeTaxEstimateView,
  implicit val errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def taxExplanationPage(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino

    (
      taxAccountService.totalTax(nino, TaxYear()),
      taxAccountService.taxCodeIncomes(nino, TaxYear()),
      taxAccountService.taxAccountSummary(nino, TaxYear()),
      codingComponentService.taxFreeAmountComponents(nino, TaxYear()),
      taxAccountService.nonTaxCodeIncomes(nino, TaxYear())).mapN {
      case (
          totalTax,
          Right(taxCodeIncomes),
          taxAccountSummary,
          codingComponents,
          nonTaxCodeIncome
          ) =>
        implicit val user: AuthedUser = request.taiUser
        val model = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncomes,
          taxAccountSummary,
          codingComponents,
          nonTaxCodeIncome)
        Ok(detailedIncomeTaxEstimate(model))
    } recover {
      case NonFatal(e) => errorPagesHandler.internalServerError("Failed to fetch total tax details", Some(e))
    }
  }
}
