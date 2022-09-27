/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.{TaxAccountService, TaxCodeChangeService}
import uk.gov.hmrc.tai.viewModels.{TaxCodeViewModel, TaxCodeViewModelPreviousYears}
import views.html.{TaxCodeDetailsPreviousYearsView, TaxCodeDetailsView}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class YourTaxCodeController @Inject()(
  taxAccountService: TaxAccountService,
  taxCodeChangeService: TaxCodeChangeService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  applicationConfig: ApplicationConfig,
  taxCodeDetails: TaxCodeDetailsView,
  taxCodeDetailsPreviousYears: TaxCodeDetailsPreviousYearsView,
  implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  private[controllers] def renderTaxCodes(employmentId: Option[Int]): Action[AnyContent] =
    (authenticate andThen validatePerson).async { implicit request =>
      val nino = request.taiUser.nino
      val year = TaxYear()

      (for {
        TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]) <- taxAccountService
                                                                              .taxCodeIncomes(nino, year)
        scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, year, taxCodeIncomes.map(_.taxCode))
      } yield {

        val filteredTaxCodes =
          employmentId.fold(taxCodeIncomes) { id =>
            taxCodeIncomes.filter(_.employmentId.contains(id))
          }

        val taxCodeViewModel = TaxCodeViewModel(filteredTaxCodes, scottishTaxRateBands, employmentId, applicationConfig)

        implicit val user: AuthedUser = request.taiUser

        Ok(taxCodeDetails(taxCodeViewModel))
      }) recover {
        case NonFatal(e) =>
          errorPagesHandler.internalServerError(s"Exception: ${e.getClass}")
      }
    }

  def taxCode(employmentId: Int): Action[AnyContent] = renderTaxCodes(Some(employmentId))
  def taxCodes: Action[AnyContent] = renderTaxCodes(None)

  def prevTaxCodes(year: TaxYear): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      val nino = request.taiUser.nino

      (for {
        taxCodeRecords       <- taxCodeChangeService.lastTaxCodeRecordsInYearPerEmployment(nino, year)
        scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, year, taxCodeRecords.map(_.taxCode))
      } yield {
        val taxCodeViewModel =
          TaxCodeViewModelPreviousYears(taxCodeRecords, scottishTaxRateBands, year, applicationConfig)
        implicit val user: AuthedUser = request.taiUser
        Ok(taxCodeDetailsPreviousYears(taxCodeViewModel, request.fullName))
      }) recover {
        case NonFatal(e) =>
          errorPagesHandler.internalServerError(s"Exception: ${e.getClass()}")
      }
  }
}
