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
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import play.api.Play.current
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
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, PersonService}
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountViewModel

class TaxFreeAmountController @Inject()(val personService: PersonService,
                                        val codingComponentService: CodingComponentService,
                                        val employmentService: EmploymentService,
                                        val companyCarService: CompanyCarService,
                                        val auditConnector: AuditConnector,
                                        val delegationConnector: DelegationConnector,
                                        val authConnector: AuthConnector,
                                        override implicit val partialRetriever: FormPartialRetriever,
                                        override implicit val templateRenderer: TemplateRenderer
                                       ) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with FeatureTogglesConfig {

  def taxFreeAmount: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            taxFreeAmount()
          }
  }

  private def taxFreeAmount()(implicit user: TaiUser, request: Request[AnyContent], person: Person) = {
    val nino = Nino(user.getNino)
    for {
      codingComponents <- codingComponentService.taxFreeAmountComponents(nino, TaxYear())
      employmentNames <- employmentService.employmentNames(nino, TaxYear())
      companyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, codingComponents)
    } yield {
      val viewModel = TaxFreeAmountViewModel(codingComponents, employmentNames, companyCarBenefits)
      Ok(views.html.taxFreeAmount(viewModel))
    }
  }
}
