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

import cats.implicits._
import controllers.auth.{AuthJourney, AuthedUser}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.estimatedIncomeTax.EstimatedIncomeTaxService
import uk.gov.hmrc.tai.service.{CodingComponentService, HasFormPartialService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax._
import views.html.estimatedIncomeTax.{ComplexEstimatedIncomeTaxView, NoCurrentIncomeView, SimpleEstimatedIncomeTaxView, ZeroTaxEstimatedIncomeTaxView}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.tai.util.EitherTExtensions._

class EstimatedIncomeTaxController @Inject() (
  codingComponentService: CodingComponentService,
  partialService: HasFormPartialService,
  taxAccountService: TaxAccountService,
  authenticate: AuthJourney,
  noCurrentIncome: NoCurrentIncomeView,
  complexEstimatedIncomeTax: ComplexEstimatedIncomeTaxView,
  simpleEstimatedIncomeTax: SimpleEstimatedIncomeTaxView,
  zeroTaxEstimatedIncomeTax: ZeroTaxEstimatedIncomeTaxView,
  implicit val
  mcc: MessagesControllerComponents,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def estimatedIncomeTax(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino = request.taiUser.nino

    (
      taxAccountService.taxAccountSummary(nino, TaxYear()).toFutureOrThrow,
      taxAccountService.totalTax(nino, TaxYear()),
      taxAccountService.nonTaxCodeIncomes(nino, TaxYear()),
      taxAccountService.taxCodeIncomes(nino, TaxYear()),
      codingComponentService.taxFreeAmountComponents(nino, TaxYear()),
      partialService.getIncomeTaxPartial
    )
      .mapN {
        case (
              taxAccountSummary,
              totalTaxDetails,
              nonTaxCodeIncome,
              Right(taxCodeIncomes),
              codingComponents,
              iFormLinks
            ) =>
          implicit val user: AuthedUser = request.taiUser

          val taxBands    = totalTaxDetails.incomeCategories.flatMap(_.taxBands).toList
          val taxViewType = EstimatedIncomeTaxService.taxViewType(
            codingComponents,
            totalTaxDetails,
            nonTaxCodeIncome,
            taxAccountSummary.totalEstimatedIncome,
            taxAccountSummary.taxFreeAllowance,
            taxAccountSummary.totalEstimatedTax,
            taxCodeIncomes.nonEmpty
          )
          taxViewType match {
            case NoIncomeTaxView => Ok(noCurrentIncome())
            case ComplexTaxView  =>
              val model =
                ComplexEstimatedIncomeTaxViewModel(codingComponents, taxAccountSummary, taxCodeIncomes, taxBands)
              Ok(complexEstimatedIncomeTax(model, iFormLinks successfulContentOrElse Html("")))
            case SimpleTaxView   =>
              val model =
                SimpleEstimatedIncomeTaxViewModel(codingComponents, taxAccountSummary, taxCodeIncomes, taxBands)
              Ok(simpleEstimatedIncomeTax(model, iFormLinks successfulContentOrElse Html("")))
            case ZeroTaxView     =>
              val model =
                ZeroTaxEstimatedIncomeTaxViewModel(codingComponents, taxAccountSummary, taxCodeIncomes, taxBands)
              Ok(zeroTaxEstimatedIncomeTax(model, iFormLinks successfulContentOrElse Html("")))
          }
        case _ =>
          errorPagesHandler.internalServerError("Failed to get estimated income tax")
      }
      .recover { case _ =>
        errorPagesHandler.internalServerError("Failed to get estimated income tax")
      }
  }
}
