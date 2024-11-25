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

import cats.data.EitherT
import controllers.auth.{AuthJourney, AuthedUser}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.{TaxFreeAmountDetails, TaxYear}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountViewModel
import views.html.TaxFreeAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

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
      codingComponents <- EitherT[Future, UpstreamErrorResponse, Seq[CodingComponent]](
                            codingComponentService.taxFreeAmountComponents(nino, TaxYear()).map(Right(_))
                          )
      employmentNames <- employmentService.employmentNames(nino, TaxYear())
      companyCarBenefits <- EitherT[Future, UpstreamErrorResponse, Seq[CompanyCarBenefit]](
                              companyCarService.companyCarOnCodingComponents(nino, codingComponents).map(Right(_))
                            )
      totalTax <-
        EitherT[Future, UpstreamErrorResponse, TotalTax](taxAccountService.totalTax(nino, TaxYear()).map(Right(_)))
    } yield {
      val viewModel = TaxFreeAmountViewModel(
        codingComponents,
        TaxFreeAmountDetails(employmentNames, companyCarBenefits, totalTax),
        applicationConfig
      )
      implicit val user: AuthedUser = request.taiUser
      Ok(taxFreeAmount(viewModel, applicationConfig, request.fullName))
    }).fold(
      error =>
        if (error.statusCode == NOT_FOUND) {
          logger.warn(s"Total tax - No tax account information found: ${error.getMessage}")
          Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage())
        } else {
          errorPagesHandler.internalServerError
        },
      identity
    )
  }
}
