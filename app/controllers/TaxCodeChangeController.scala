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

package controllers

import com.google.inject.Inject
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{CodingComponentsWithCarBenefits, YourTaxFreeAmount}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import uk.gov.hmrc.urls.Link

import scala.concurrent.Future

class TaxCodeChangeController @Inject()(codingComponentService: CodingComponentService,
                                        employmentService: EmploymentService,
                                        companyCarService: CompanyCarService,
                                        taxCodeChangeService: TaxCodeChangeService,
                                        taxAccountService: TaxAccountService,
                                        authenticate: AuthAction,
                                        validatePerson: ValidatePerson,
                                        override implicit val partialRetriever: FormPartialRetriever,
                                        override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with FeatureTogglesConfig
  with YourTaxFreeAmount {

  def taxCodeComparison: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      val nino: Nino = request.taiUser.nino
      for {
        taxCodeChange <- taxCodeChangeService.taxCodeChange(nino)
        scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, TaxYear(), taxCodeChange.uniqueTaxCodes)
      } yield {
        val viewModel = TaxCodeChangeViewModel(taxCodeChange, scottishTaxRateBands)
        implicit val user = request.taiUser
        Ok(views.html.taxCodeChange.taxCodeComparison(viewModel))
      }
  }

  def yourTaxFreeAmount: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      val nino: Nino = request.taiUser.nino
      val taxFreeAmountViewModel =
        if (taxFreeAmountComparisonEnabled) {
          taxFreeAmountWithPrevious(nino)
        } else {
          taxFreeAmount(nino)
        }

      implicit val user = request.taiUser
      taxFreeAmountViewModel.map(viewModel => {
        Ok(views.html.taxCodeChange.yourTaxFreeAmount(viewModel))
      })
  }

  def whatHappensNext: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      Future.successful(Ok(views.html.taxCodeChange.whatHappensNext()))
  }

  private def taxFreeAmountWithPrevious(nino: Nino)(implicit request: Request[AnyContent]): Future[YourTaxFreeAmountViewModel] = {

    val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())
    val taxCodeChangeFuture = taxCodeChangeService.taxCodeChange(nino)
    val taxFreeAmountComparisonFuture = codingComponentService.taxFreeAmountComparison(nino)

    for {
      employmentNames <- employmentNameFuture
      taxCodeChange <- taxCodeChangeFuture
      taxFreeAmountComparison <- taxFreeAmountComparisonFuture
      currentCompanyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, taxFreeAmountComparison.current)
      previousCompanyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, taxFreeAmountComparison.previous)
    } yield {
      buildTaxFreeAmount(
        Some(CodingComponentsWithCarBenefits(
          taxCodeChange.mostRecentPreviousTaxCodeChangeDate,
          taxFreeAmountComparison.previous,
          previousCompanyCarBenefits
        )),
        CodingComponentsWithCarBenefits(
          taxCodeChange.mostRecentTaxCodeChangeDate,
          taxFreeAmountComparison.current,
          currentCompanyCarBenefits
        ),
        employmentNames)
    }
  }

  private def taxFreeAmount(nino: Nino)(implicit request: Request[AnyContent]): Future[YourTaxFreeAmountViewModel] = {

    val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())
    val taxCodeChangeFuture = taxCodeChangeService.taxCodeChange(nino)
    val codingComponentsFuture = codingComponentService.taxFreeAmountComponents(nino, TaxYear())

    for {
      employmentNames <- employmentNameFuture
      taxCodeChange <- taxCodeChangeFuture
      currentCodingComponents <- codingComponentsFuture
      currentCompanyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, currentCodingComponents)
    } yield {
      buildTaxFreeAmount(
        None,
        CodingComponentsWithCarBenefits(
          taxCodeChange.mostRecentTaxCodeChangeDate,
          currentCodingComponents,
          currentCompanyCarBenefits
        ),
        employmentNames)
    }
  }
}
