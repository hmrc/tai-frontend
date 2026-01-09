/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.implicits.*
import controllers.auth.{AuthJourney, AuthedUser}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.IabdDetails
import uk.gov.hmrc.tai.service.{CodingComponentService, IabdService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.DetailedIncomeTaxEstimateViewModel
import views.html.estimatedIncomeTax.DetailedIncomeTaxEstimateView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import uk.gov.hmrc.tai.util.EitherTExtensions.*
import uk.gov.hmrc.tai.util.TaxAccountHelper

@Singleton
class DetailedIncomeTaxEstimateController @Inject() (
  taxAccountService: TaxAccountService,
  codingComponentService: CodingComponentService,
  iabdService: IabdService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  detailedIncomeTaxEstimate: DetailedIncomeTaxEstimateView,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def taxExplanationPage(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino = request.taiUser.nino

    (
      taxAccountService.totalTax(nino, TaxYear()),
      taxAccountService.taxCodeIncomes(nino, TaxYear()),
      taxAccountService.taxAccountSummary(nino, TaxYear()).toFutureOrThrow,
      codingComponentService.taxFreeAmountComponents(nino, TaxYear()),
      taxAccountService.nonTaxCodeIncomes(nino, TaxYear()),
      iabdService.getIabds(nino, TaxYear()).getOrElse(Seq.empty[IabdDetails])
    ).mapN {
      case (
            totalTax,
            Right(taxCodeIncomes),
            taxAccountSummary,
            codingComponents,
            nonTaxCodeIncome,
            iabds: Seq[IabdDetails]
          ) =>
        implicit val user: AuthedUser  = request.taiUser
        val newIncomeEstimateAvailable =
          TaxAccountHelper.getIabdLatestEstimatedIncome(iabds, taxAccountSummary.date, None).nonEmpty

        val model = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncomes,
          taxAccountSummary,
          codingComponents,
          nonTaxCodeIncome,
          newIncomeEstimateAvailable
        )
        Ok(detailedIncomeTaxEstimate(model))
    } recover { case NonFatal(e) =>
      errorPagesHandler.internalServerError("Failed to fetch total tax details", Some(e))
    }
  }
}
