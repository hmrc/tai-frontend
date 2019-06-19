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

import javax.inject.Inject
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.viewModels._
import uk.gov.hmrc.tai.viewModels.incomeTaxComparison.{EstimatedIncomeTaxComparisonItem, EstimatedIncomeTaxComparisonViewModel, IncomeTaxComparisonViewModel}

import scala.util.control.NonFatal

class IncomeTaxComparisonController @Inject()(val auditConnector: AuditConnector,
                                              taxAccountService: TaxAccountService,
                                              employmentService: EmploymentService,
                                              codingComponentService: CodingComponentService,
                                              updateNextYearsIncomeService: UpdateNextYearsIncomeService,
                                              authenticate: AuthAction,
                                              validatePerson: ValidatePerson,
                                              override implicit val partialRetriever: FormPartialRetriever,
                                              override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with FeatureTogglesConfig {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      val nino = request.taiUser.nino
      val currentTaxYear = TaxYear()
      val nextTaxYear = currentTaxYear.next
      val taxSummaryCYFuture = taxAccountService.taxAccountSummary(nino, currentTaxYear)
      val taxSummaryCYPlusOneFuture = taxAccountService.taxAccountSummary(nino, nextTaxYear)
      val taxCodeIncomesCYFuture = taxAccountService.taxCodeIncomes(nino, currentTaxYear)
      val taxCodeIncomesCYPlusOneFuture = taxAccountService.taxCodeIncomes(nino, nextTaxYear)
      val taxComponentsCYFuture = codingComponentService.taxFreeAmountComponents(nino, currentTaxYear)
      val taxComponentsCYPlusOneFuture = codingComponentService.taxFreeAmountComponents(nino, nextTaxYear)
      val employmentsCYFuture = employmentService.employments(nino, currentTaxYear)

      (for {
        taxSummaryCY <- taxSummaryCYFuture
        taxSummaryCyPlusOne <- taxSummaryCYPlusOneFuture
        taxCodeIncomesForCy <- taxCodeIncomesCYFuture
        taxCodeIncomesForCyPlusOne <- taxCodeIncomesCYPlusOneFuture
        codingComponentsCY <- taxComponentsCYFuture
        codingComponentsCYPlusOne <- taxComponentsCYPlusOneFuture
        employmentsCY <- employmentsCYFuture
        isEstimatedPayJourneyComplete <- updateNextYearsIncomeService.isEstimatedPayJourneyComplete
      } yield {
        (taxSummaryCY, taxSummaryCyPlusOne, taxCodeIncomesForCy, taxCodeIncomesForCyPlusOne) match {
          case (TaiSuccessResponseWithPayload(taxAccountSummaryCY: TaxAccountSummary),
          TaiSuccessResponseWithPayload(taxAccountSummaryCYPlusOne: TaxAccountSummary),
          TaiSuccessResponseWithPayload(taxCodeIncomesCY: Seq[TaxCodeIncome]),
          TaiSuccessResponseWithPayload(taxCodeIncomesCYPlusOne: Seq[TaxCodeIncome])
            ) => {

            val estimatedIncomeTaxComparisonViewModel = {
              val cyEstimatedTax = EstimatedIncomeTaxComparisonItem(currentTaxYear, taxAccountSummaryCY.totalEstimatedTax)
              val cyPlusOneEstimatedTax = EstimatedIncomeTaxComparisonItem(nextTaxYear, taxAccountSummaryCYPlusOne.totalEstimatedTax)
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

              TaxFreeAmountComparisonViewModel(Seq(cyCodingComponents, cyPlusOneTaxComponents),
                Seq(cyTaxSummary, cyPlusOneTaxSummary))
            }

            val employmentViewModel = IncomeSourceComparisonViewModel(taxCodeIncomesCY, employmentsCY, taxCodeIncomesCYPlusOne)

            val model = IncomeTaxComparisonViewModel(request.taiUser.getDisplayName, estimatedIncomeTaxComparisonViewModel,
              taxCodeComparisonModel, taxFreeAmountComparisonModel, employmentViewModel, isEstimatedPayJourneyComplete)

            implicit val user = request.taiUser
            Ok(views.html.incomeTaxComparison.Main(model, cyPlus1EstimatedPayEnabled))
          }
          case _ => throw new RuntimeException("Not able to fetch income tax comparision details")
        }
      }) recover {
        case NonFatal(e) => internalServerError("IncomeTaxComparisonController exception", Some(e))
      }
  }

}
