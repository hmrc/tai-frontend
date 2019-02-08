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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{Employment, IncomeSource, TaxAccountSummary}
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.{AuditConstants, TaiConstants}
import uk.gov.hmrc.tai.viewModels.TaxAccountSummaryViewModel

import scala.concurrent.Future

class TaxAccountSummaryController @Inject()(trackingService: TrackingService,
                                            employmentService: EmploymentService,
                                            taxAccountService: TaxAccountService,
                                            auditService: AuditService,
                                            personService: PersonService,
                                            val auditConnector: AuditConnector,
                                            val delegationConnector: DelegationConnector,
                                            val authConnector: AuthConnector,
                                            override implicit val partialRetriever: FormPartialRetriever,
                                            override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with AuditConstants {

  def onPageLoad: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {

            val nino = Nino(user.getNino)
            auditService.createAndSendAuditEvent(TaxAccountSummary_UserEntersSummaryPage, Map("nino" -> user.getNino))
            taxAccountService.taxAccountSummary(nino, TaxYear()).flatMap {
              case (TaiTaxAccountFailureResponse(message)) if message.toLowerCase.contains(TaiConstants.NpsTaxAccountDataAbsentMsg) ||
                message.toLowerCase.contains(TaiConstants.NpsNoEmploymentForCurrentTaxYear) =>
                Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
              case TaiSuccessResponseWithPayload(taxAccountSummary: TaxAccountSummary) =>
                taxAccountSummaryViewModel(nino, taxAccountSummary) map { vm =>
                  Ok(views.html.incomeTaxSummary(vm))
                }
              case _ => throw new RuntimeException("Failed to fetch tax account summary details")
            }
          }
  }

  private def taxAccountSummaryViewModel(nino: Nino, taxAccountSummary: TaxAccountSummary)
                                        (implicit hc: HeaderCarrier, messages: Messages): Future[TaxAccountSummaryViewModel] =
    for {
      livePensionIncomeSources <- taxAccountService.incomeSources(nino, TaxYear(), "PensionIncome", "Live")
      liveEmploymentIncomeSources <- taxAccountService.incomeSources(nino, TaxYear(), "EmploymentIncome", "Live")
      ceasedEmploymentIncomeSources <- taxAccountService.incomeSources(nino, TaxYear(), "EmploymentIncome", "Ceased")
      nonMatchingCeasedEmployments <- employmentService.ceasedEmployments(nino, TaxYear())
      nonTaxCodeIncome <- taxAccountService.nonTaxCodeIncomes(nino, TaxYear())
      isAnyFormInProgress <- trackingService.isAnyIFormInProgress(nino.nino)
    } yield {
      (livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources, nonMatchingCeasedEmployments, nonTaxCodeIncome) match {
        case (TaiSuccessResponseWithPayload(livePensionIncomeSources: Seq[IncomeSource]),
        TaiSuccessResponseWithPayload(liveEmploymentIncomeSources: Seq[IncomeSource]),
        TaiSuccessResponseWithPayload(ceasedEmploymentIncomeSources: Seq[IncomeSource]),
        nonMatchingCeasedEmployments: Seq[Employment],
        TaiSuccessResponseWithPayload(nonTaxCodeIncome: NonTaxCodeIncome)) =>
          TaxAccountSummaryViewModel(taxAccountSummary,
            isAnyFormInProgress,
            nonTaxCodeIncome,
            livePensionIncomeSources,
            liveEmploymentIncomeSources,
            ceasedEmploymentIncomeSources,
            nonMatchingCeasedEmployments)(messages)
        case _ => throw new RuntimeException("Failed to fetch income details")
      }
    }

}
