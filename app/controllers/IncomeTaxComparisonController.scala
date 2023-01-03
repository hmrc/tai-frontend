/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.implicits._
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.viewModels._
import uk.gov.hmrc.tai.viewModels.incomeTaxComparison.{EstimatedIncomeTaxComparisonItem, EstimatedIncomeTaxComparisonViewModel, IncomeTaxComparisonViewModel}
import views.html.incomeTaxComparison.MainView

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class IncomeTaxComparisonController @Inject()(
  val auditConnector: AuditConnector,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  codingComponentService: CodingComponentService,
  updateNextYearsIncomeService: UpdateNextYearsIncomeService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  mainView: MainView,
  errorPagesHandler: ErrorPagesHandler)(implicit val ec: ExecutionContext, templateRenderer: TemplateRenderer)
    extends TaiBaseController(mcc) {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino
    val currentTaxYear = TaxYear()
    val nextTaxYear = currentTaxYear.next

    (
      taxAccountService.taxAccountSummary(nino, currentTaxYear),
      taxAccountService.taxAccountSummary(nino, nextTaxYear),
      taxAccountService.taxCodeIncomes(nino, currentTaxYear),
      taxAccountService.taxCodeIncomes(nino, nextTaxYear),
      codingComponentService.taxFreeAmountComponents(nino, currentTaxYear),
      codingComponentService.taxFreeAmountComponents(nino, nextTaxYear),
      employmentService.employments(nino, currentTaxYear),
      updateNextYearsIncomeService.isEstimatedPayJourneyComplete
    ).mapN {
        case (
            taxAccountSummaryCY,
            taxAccountSummaryCYPlusOne,
            Right(taxCodeIncomesCY),
            Right(taxCodeIncomesCYPlusOne),
            codingComponentsCY,
            codingComponentsCYPlusOne,
            employmentsCY,
            isEstimatedPayJourneyComplete
            ) =>
          val estimatedIncomeTaxComparisonViewModel = {
            val cyEstimatedTax = EstimatedIncomeTaxComparisonItem(currentTaxYear, taxAccountSummaryCY.totalEstimatedTax)
            val cyPlusOneEstimatedTax =
              EstimatedIncomeTaxComparisonItem(nextTaxYear, taxAccountSummaryCYPlusOne.totalEstimatedTax)
            EstimatedIncomeTaxComparisonViewModel(Seq(cyEstimatedTax, cyPlusOneEstimatedTax))
          }

          val taxCodeComparisonModel = {
            val cyTaxCodeIncomeSources = TaxCodeIncomesForYear(currentTaxYear, taxCodeIncomesCY)
            val cyPlusOneTaxCodeIncomeSources = TaxCodeIncomesForYear(nextTaxYear, taxCodeIncomesCYPlusOne)
            TaxCodeComparisonViewModel(Seq(cyTaxCodeIncomeSources, cyPlusOneTaxCodeIncomeSources))
          }

          val taxFreeAmountComparisonModel = {
            val cyCodingComponents = CodingComponentForYear(currentTaxYear, codingComponentsCY)
            val cyPlusOneTaxComponents = CodingComponentForYear(nextTaxYear, codingComponentsCYPlusOne)
            val cyTaxSummary = TaxAccountSummaryForYear(currentTaxYear, taxAccountSummaryCY)
            val cyPlusOneTaxSummary = TaxAccountSummaryForYear(currentTaxYear, taxAccountSummaryCYPlusOne)

            TaxFreeAmountComparisonViewModel(
              Seq(cyCodingComponents, cyPlusOneTaxComponents),
              Seq(cyTaxSummary, cyPlusOneTaxSummary))
          }

          val employmentViewModel =
            IncomeSourceComparisonViewModel(taxCodeIncomesCY, employmentsCY, taxCodeIncomesCYPlusOne)

          val model = IncomeTaxComparisonViewModel(
            request.fullName,
            estimatedIncomeTaxComparisonViewModel,
            taxCodeComparisonModel,
            taxFreeAmountComparisonModel,
            employmentViewModel,
            isEstimatedPayJourneyComplete
          )

          Ok(mainView(model, applicationConfig))
        case _ =>
          throw new RuntimeException("Not able to fetch income tax comparision details")

      }
      .recover {
        case NonFatal(e) => errorPagesHandler.internalServerError("IncomeTaxComparisonController exception", Some(e))
      }
  }

}
