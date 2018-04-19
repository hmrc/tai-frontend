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
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{FeatureTogglesConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.model.TaiRoot
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, TaiService}
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountViewModel

trait TaxFreeAmountController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with FeatureTogglesConfig {

  def taiService: TaiService
  def codingComponentService: CodingComponentService
  def employmentService: EmploymentService
  def companyCarService: CompanyCarService

  def taxFreeAmount: Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
              taxFreeAmount()
          }
  }

  private def taxFreeAmount()(implicit user: TaiUser, request: Request[AnyContent], taiRoot: TaiRoot) = {
    val nino = Nino(user.getNino)
    for {
      codingComponents <- codingComponentService.taxFreeAmountComponents(nino, TaxYear())
      employmentNames <- employmentService.employmentNames(nino, TaxYear())
      companyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, codingComponents)
    } yield {
      val viewModel = TaxFreeAmountViewModel(codingComponents, employmentNames, companyCarBenefits)
      Ok(views.html.taxFreeAmountNew(viewModel))
    }
  }
}

object TaxFreeAmountController extends TaxFreeAmountController with AuthenticationConnectors {
  override val taiService: TaiService = TaiService
  override val codingComponentService: CodingComponentService = CodingComponentService
  override val employmentService: EmploymentService = EmploymentService
  override val companyCarService: CompanyCarService = CompanyCarService

  override implicit def templateRenderer = LocalTemplateRenderer

  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}

