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
import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import uk.gov.hmrc.urls.Link

import scala.concurrent.Future

class TaxCodeChangeController @Inject()(personService: PersonService,
                                        codingComponentService: CodingComponentService,
                                        employmentService: EmploymentService,
                                        companyCarService: CompanyCarService,
                                        taxCodeChangeService: TaxCodeChangeService,
                                        taxAccountService: TaxAccountService,
                                        val auditConnector: AuditConnector,
                                        val delegationConnector: DelegationConnector,
                                        val authConnector: AuthConnector,
                                        override implicit val partialRetriever: FormPartialRetriever,
                                        override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with WithAuthorisedForTaiLite
  with DelegationAwareActions
  with Auditable
  with FeatureTogglesConfig {

  def taxCodeComparison: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          if (taxCodeChangeEnabled) {
            ServiceCheckLite.personDetailsCheck {
              val nino: Nino = Nino(user.getNino)

              for {
                taxCodeChange <- taxCodeChangeService.taxCodeChange(nino)
                scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, TaxYear(), taxCodeChange.uniqueTaxCodes)
              } yield {
                val viewModel = TaxCodeChangeViewModel(taxCodeChange, scottishTaxRateBands)
                Ok(views.html.taxCodeChange.taxCodeComparison(viewModel))
              }
            }
          } else {
            ServiceCheckLite.personDetailsCheck {
              Future.successful(Ok(notFoundView))
            }
          }
  }

  def yourTaxFreeAmount: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          if (taxCodeChangeEnabled) {
            ServiceCheckLite.personDetailsCheck {
              val nino = Nino(user.getNino)

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
                Ok(views.html.taxCodeChange.yourTaxFreeAmount(viewModel))
              }
            }
          } else {
            ServiceCheckLite.personDetailsCheck {
              Future.successful(Ok(notFoundView))
            }
          }
  }

  def whatHappensNext: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          if (taxCodeChangeEnabled) {
            ServiceCheckLite.personDetailsCheck {
              Future.successful(Ok(views.html.taxCodeChange.whatHappensNext()))
            }
          }
          else {
            ServiceCheckLite.personDetailsCheck {
              Future.successful(Ok(notFoundView))
            }
          }
  }

  private def notFoundView(implicit request: Request[_]) = views.html.error_template_noauth(Messages("global.error.pageNotFound404.title"),
    Messages("tai.errorMessage.heading"),
    Messages("tai.errorMessage.frontend404", Link.toInternalPage(
      url = routes.TaxAccountSummaryController.onPageLoad().url,
      value = Some(Messages("tai.errorMessage.startAgain"))
    ).toHtml))

}
