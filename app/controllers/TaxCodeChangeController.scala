/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{FeatureTogglesConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, PersonService, TaxCodeChangeService}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import uk.gov.hmrc.urls.Link

import scala.concurrent.Future

trait TaxCodeChangeController extends TaiBaseController
  with WithAuthorisedForTaiLite
  with DelegationAwareActions
  with Auditable
  with FeatureTogglesConfig {
  def personService: PersonService

  def codingComponentService: CodingComponentService

  def employmentService: EmploymentService

  def companyCarService: CompanyCarService

  def taxCodeChangeService: TaxCodeChangeService

  def taxCodeComparison: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          if (taxCodeChangeEnabled) {
            ServiceCheckLite.personDetailsCheck {
              val nino: Nino = Nino(user.getNino)

//              val startDate = TaxYearResolver.startOfCurrentTaxYear
//              val previousTaxCodeRecord1 = TaxCodeRecord("B175", startDate, startDate.plusMonths(1),"Split Tax Code", false, "A-1234", false)
//              val currentTaxCodeRecord1 = previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)
//              val fullYearTaxCode = TaxCodeRecord("A1111", startDate, TaxYearResolver.endOfCurrentTaxYear, "Full Year", false, "B-1234", false)
//              val primaryFullYearTaxCode = fullYearTaxCode.copy(employerName = "Full Year Primary", payrollNumber = "C-1234", primary = true)
//              val unmatchedPreviousTaxCode = TaxCodeRecord("C11", startDate, startDate.plusMonths(1),"Unmatched Previous", false, "D-1234", false)
//              val unmatchedCurrentTaxCode = TaxCodeRecord("E183", startDate.plusMonths(1), TaxYearResolver.endOfCurrentTaxYear,"Unmatched Current", false, "E-1234", false)
//
//              val taxCodeChange = TaxCodeChange(
//                Seq(previousTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedPreviousTaxCode),
//                Seq(currentTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedCurrentTaxCode)
//              )

              taxCodeChangeService.taxCodeChange(nino) map { taxCodeChange =>
                val viewModel = TaxCodeChangeViewModel(taxCodeChange)
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

object TaxCodeChangeController extends TaxCodeChangeController with AuthenticationConnectors {

  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
  override implicit val templateRenderer = LocalTemplateRenderer
  override val personService: PersonService = PersonService
  override val taxCodeChangeService: TaxCodeChangeService = TaxCodeChangeService
  override val codingComponentService: CodingComponentService = CodingComponentService
  override val employmentService: EmploymentService = EmploymentService
  override val companyCarService: CompanyCarService = CompanyCarService
}