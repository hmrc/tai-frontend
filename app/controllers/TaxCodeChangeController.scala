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
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxCodeHistory
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, PersonService, TaxCodeChangeService}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel

import scala.concurrent.Future

trait TaxCodeChangeController extends TaiBaseController
  with WithAuthorisedForTaiLite
  with DelegationAwareActions
  with Auditable
{
  def personService: PersonService
  def codingComponentService: CodingComponentService
  def employmentService: EmploymentService
  def companyCarService: CompanyCarService
  def taxCodeChangeService: TaxCodeChangeService

  def taxCodeComparison: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.taxCodeChange.taxCodeComparison()))
          }
  }

  def yourTaxFreeAmount: Action[AnyContent] = authorisedForTai(personService).async {
  implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          val nino = Nino(user.getNino)

          val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())
          val taxCodeChangeFuture = taxCodeChangeService.taxCodeHistory(nino)

          for {
            employmentNames <- employmentNameFuture
            taxCodeChange <- taxCodeChangeFuture
            codingComponents <- codingComponentService.taxFreeAmountComponents(nino, TaxYear())
            companyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, codingComponents)

          } yield {
            (taxCodeChange) match {
              case (TaiSuccessResponseWithPayload(taxCodeHistory: TaxCodeHistory)) => {
                taxCodeHistory.taxCodeRecords match {
                  case Some(taxCodeRecords) =>
                      val viewModel = YourTaxFreeAmountViewModel(taxCodeRecords, codingComponents, employmentNames, companyCarBenefits)
                      Ok(views.html.taxCodeChange.yourTaxFreeAmount(viewModel))
                }
              }
            }

          }
        }
  }

  def whatHappensNext : Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.taxCodeChange.whatHappensNext()))
          }

  }

}

object TaxCodeChangeController extends TaxCodeChangeController with AuthenticationConnectors{

  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
  override implicit val templateRenderer = LocalTemplateRenderer
  override val personService: PersonService = PersonService
  override val codingComponentService: CodingComponentService = CodingComponentService
  override val employmentService: EmploymentService = EmploymentService
  override val companyCarService: CompanyCarService = CompanyCarService
  override val taxCodeChangeService: TaxCodeChangeService = TaxCodeChangeService

}