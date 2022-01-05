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
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.{TaxFreeAmountDetails, TaxYear}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountViewModel
import views.html.TaxFreeAmountView

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class TaxFreeAmountController @Inject()(
  codingComponentService: CodingComponentService,
  employmentService: EmploymentService,
  taxAccountService: TaxAccountService,
  companyCarService: CompanyCarService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  taxFreeAmount: TaxFreeAmountView,
  implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def taxFreeAmount: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino

    (for {
      codingComponents   <- codingComponentService.taxFreeAmountComponents(nino, TaxYear())
      employmentNames    <- employmentService.employmentNames(nino, TaxYear())
      companyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, codingComponents)
      totalTax           <- taxAccountService.totalTax(nino, TaxYear())
    } yield {
      totalTax match {
        case TaiSuccessResponseWithPayload(totalTax: TotalTax) =>
          val viewModel = TaxFreeAmountViewModel(
            codingComponents,
            TaxFreeAmountDetails(employmentNames, companyCarBenefits, totalTax),
            applicationConfig
          )
          implicit val user: AuthedUser = request.taiUser
          Ok(taxFreeAmount(viewModel, applicationConfig))
        case TaiNotFoundResponse(_) => Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage())
        case _                      => throw new RuntimeException("Failed to fetch total tax details")
      }
    }) recover {
      case NonFatal(e) => errorPagesHandler.internalServerError(s"Could not get tax free amount", Some(e))
    }
  }
}
