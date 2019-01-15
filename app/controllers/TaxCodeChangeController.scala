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
import controllers.actions.DeceasedActionFilter
import controllers.auth.{AuthAction, AuthActionedTaiUser}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import uk.gov.hmrc.urls.Link

import scala.concurrent.Future

class TaxCodeChangeController @Inject()(val codingComponentService: CodingComponentService,
                                        val employmentService: EmploymentService,
                                        val companyCarService: CompanyCarService,
                                        val taxCodeChangeService: TaxCodeChangeService,
                                        val taxAccountService: TaxAccountService,
                                        authenticate: AuthAction,
                                        filterDeceased: DeceasedActionFilter,
                                        override implicit val partialRetriever: FormPartialRetriever,
                                        override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with FeatureTogglesConfig {

  def taxCodeComparison: Action[AnyContent] = (authenticate andThen filterDeceased).async {
    implicit request =>
      if (taxCodeChangeEnabled) {
        val taiUser = request.taiUser
        val nino = taiUser.nino

        for {
          taxCodeChange <- taxCodeChangeService.taxCodeChange(nino)
          scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, TaxYear(), taxCodeChange.uniqueTaxCodes)
        } yield {
          val viewModel = TaxCodeChangeViewModel(taxCodeChange, scottishTaxRateBands)
          implicit val user: AuthActionedTaiUser = taiUser
          Ok(views.html.taxCodeChange.taxCodeComparison(viewModel))
        }
      } else {
        Future.successful(Ok(notFoundView))
      }
  }

  def yourTaxFreeAmount: Action[AnyContent] = (authenticate andThen filterDeceased).async {
    implicit request =>
      if (taxCodeChangeEnabled) {
        val nino = request.taiUser.nino
        val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())
        val taxCodeChangeFuture = taxCodeChangeService.taxCodeChange(nino)
        val codingComponentsFuture = codingComponentService.taxFreeAmountComponents(nino, TaxYear())

        for {
          employmentNames <- employmentNameFuture
          taxCodeChange <- taxCodeChangeFuture
          codingComponents <- codingComponentsFuture
          companyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, codingComponents)
        } yield {
          val viewModel = YourTaxFreeAmountViewModel(taxCodeChange.mostRecentTaxCodeChangeDate, codingComponents, employmentNames, companyCarBenefits)
          implicit val user: AuthActionedTaiUser = request.taiUser

          Ok(views.html.taxCodeChange.yourTaxFreeAmount(viewModel))
        }

      } else {
        Future.successful(Ok(notFoundView))
      }
  }


  def whatHappensNext: Action[AnyContent] = (authenticate andThen filterDeceased).async {
    implicit request =>
      if (taxCodeChangeEnabled) {
        implicit val user: AuthActionedTaiUser = request.taiUser
        Future.successful(Ok(views.html.taxCodeChange.whatHappensNext()))
      } else {
        Future.successful(Ok(notFoundView()))
      }
  }

  private def notFoundView()(implicit request: Request[_])
  = views.html.error_template_noauth(Messages("global.error.pageNotFound404.title"),
    Messages("tai.errorMessage.heading"),
    Messages("tai.errorMessage.frontend404", Link.toInternalPage(
      url = routes.TaxAccountSummaryController.onPageLoad().url,
      value = Some(Messages("tai.errorMessage.startAgain"))
    ).toHtml))

}
