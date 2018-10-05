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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.AuditConstants
import uk.gov.hmrc.tai.viewModels.PreviousYearUnderpaymentViewModel
import views.html.previousYearUnderpayment


trait UnderpaymentFromPreviousYearController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with AuditConstants {

  def personService: PersonService

  def auditService: AuditService

  def taxAccountService: TaxAccountService

  def employmentService: EmploymentService

  def codingComponentService: CodingComponentService

  def underpaymentExplanation = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>

          val nino = Nino(user.getNino)
          val year = TaxYear()
          val totalTaxFuture = taxAccountService.totalTax(nino, year)
          val employmentsFuture = employmentService.employments(nino, year.prev)
          val codingComponentsFuture = codingComponentService.taxFreeAmountComponents(nino, year)

          for {
            employments <- employmentsFuture
            codingComponents <- codingComponentsFuture
            totalTax <- totalTaxFuture
          } yield {
            totalTax match {
              case TaiSuccessResponseWithPayload(totalTax: TotalTax) =>
                Ok(previousYearUnderpayment(PreviousYearUnderpaymentViewModel(codingComponents, employments, totalTax)))
              case _ => throw new RuntimeException("Failed to fetch total tax details")
            }
          }

  }
}


object UnderpaymentFromPreviousYearController extends UnderpaymentFromPreviousYearController with AuthenticationConnectors {
  override def personService: PersonService = PersonService
  override def auditService: AuditService = AuditService
  override def taxAccountService: TaxAccountService = TaxAccountService
  override def employmentService: EmploymentService = EmploymentService
  override def codingComponentService: CodingComponentService = CodingComponentService
  override implicit def templateRenderer: TemplateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}
