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

import controllers.auth.{AuthJourney, AuthedUser}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.{TaxFreeAmountDetails, TaxYear}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountViewModel
import views.html.TaxFreeAmountView

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class TaxFreeAmountController @Inject() (
  codingComponentService: CodingComponentService,
  employmentService: EmploymentService,
  taxAccountService: TaxAccountService,
  companyCarService: CompanyCarService,
  authenticate: AuthJourney,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  taxFreeAmount: TaxFreeAmountView,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Logging {

  def taxFreeAmount: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino = request.taiUser.nino

    (for {
      codingComponents   <- codingComponentService.taxFreeAmountComponents(nino, TaxYear())
      employmentNames    <- employmentService.employmentNames(nino, TaxYear())
      companyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, codingComponents)
      totalTax           <- taxAccountService.totalTax(nino, TaxYear())
    } yield {
      val viewModel = TaxFreeAmountViewModel(
        codingComponents,
        TaxFreeAmountDetails(employmentNames, companyCarBenefits, totalTax),
        applicationConfig
      )
      implicit val user: AuthedUser = request.taiUser
      Ok(taxFreeAmount(viewModel, applicationConfig, request.fullName))
    }) recover {
      case e: NotFoundException =>
        logger.warn(s"Total tax - No tax account information found: ${e.getMessage}")
        Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage())
      case NonFatal(e) => errorPagesHandler.internalServerError(s"Could not get tax free amount", Some(e))
    }
  }
}
