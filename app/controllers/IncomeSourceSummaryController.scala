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

import com.google.inject.Inject
import controllers.auth.WithAuthorisedForTaiLite
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel

class IncomeSourceSummaryController @Inject()(val personService: PersonService,
                                              val auditConnector: AuditConnector,
                                              val delegationConnector: DelegationConnector,
                                              val authConnector: AuthConnector,
                                              val taxAccountService: TaxAccountService,
                                              employmentService: EmploymentService,
                                              benefitsService: BenefitsService,
                                              override implicit val partialRetriever: FormPartialRetriever,
                                              override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with WithAuthorisedForTaiLite {

  def onPageLoad(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val taxCodeIncomesFuture = taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear())
            val employmentFuture = employmentService.employment(Nino(user.getNino), empId)
            val benefitsFuture = benefitsService.benefits(Nino(user.getNino), TaxYear().year)

            for {
              taxCodeIncomeDetails <- taxCodeIncomesFuture
              employmentDetails <- employmentFuture
              benefitsDetails <- benefitsFuture
            } yield {
              (taxCodeIncomeDetails, employmentDetails) match {
                case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
                  val incomeDetailsViewModel = IncomeSourceSummaryViewModel(empId, user.getDisplayName, taxCodeIncomes, employment, benefitsDetails)
                  Ok(views.html.IncomeSourceSummary(incomeDetailsViewModel))
                case _ => throw new RuntimeException("Error while fetching income summary details")
              }
            }
          }
  }
}
