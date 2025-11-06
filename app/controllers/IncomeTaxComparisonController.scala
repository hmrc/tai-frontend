/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import controllers.auth.{AuthJourney, DataRequest}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{Employment, TaxAccountSummary}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.FutureOps.AttemptTNel
import uk.gov.hmrc.tai.viewModels._
import uk.gov.hmrc.tai.viewModels.incomeTaxComparison.{EstimatedIncomeTaxComparisonItem, EstimatedIncomeTaxComparisonViewModel, IncomeTaxComparisonViewModel}
import views.html.incomeTaxComparison.MainView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IncomeTaxComparisonController @Inject() (
  val auditConnector: AuditConnector,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  codingComponentService: CodingComponentService,
  updateNextYearsIncomeService: UpdateNextYearsIncomeService,
  authenticate: AuthJourney,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  mainView: MainView,
  errorPagesHandler: ErrorPagesHandler
)(implicit val ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def onPageLoad(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val nino           = request.taiUser.nino
    val currentTaxYear = TaxYear()
    val nextTaxYear    = currentTaxYear.next

    (
      taxAccountService.taxAccountSummary(nino, currentTaxYear).leftMap(msg => NonEmptyList.one(new Throwable(msg))),
      taxAccountService.taxAccountSummary(nino, nextTaxYear).leftMap(msg => NonEmptyList.one(new Throwable(msg))),
      EitherT(taxAccountService.taxCodeIncomes(nino, currentTaxYear)).leftMap(msg =>
        NonEmptyList.one(new Throwable(msg))
      ),
      EitherT(taxAccountService.taxCodeIncomes(nino, nextTaxYear)).leftMap(msg => NonEmptyList.one(new Throwable(msg))),
      codingComponentService.taxFreeAmountComponents(nino, currentTaxYear).attemptTNel,
      codingComponentService.taxFreeAmountComponents(nino, nextTaxYear).attemptTNel,
      employmentService.employmentsOnly(nino, currentTaxYear).leftMap(msg => NonEmptyList.one(new Throwable(msg))),
      updateNextYearsIncomeService.isEstimatedPayJourneyComplete(request.userAnswers).attemptTNel
    ).parMapN(computeModel(currentTaxYear, nextTaxYear))
      .fold(
        errorPagesHandler
          .internalServerError("Not able to fetch income tax comparison details in IncomeTaxComparisonController", _),
        model => Ok(mainView(model, applicationConfig))
      )
  }

  private def computeModel(currentTaxYear: TaxYear, nextTaxYear: TaxYear)(
    taxAccountSummaryCY: TaxAccountSummary,
    taxAccountSummaryCYPlusOne: TaxAccountSummary,
    taxCodeIncomesCY: Seq[TaxCodeIncome],
    taxCodeIncomesCYPlusOne: Seq[TaxCodeIncome],
    codingComponentsCY: Seq[CodingComponent],
    codingComponentsCYPlusOne: Seq[CodingComponent],
    employmentsCY: Seq[Employment],
    isEstimatedPayJourneyComplete: Boolean
  )(implicit request: DataRequest[AnyContent]) = {
    val estimatedIncomeTaxComparisonViewModel = {
      val cyEstimatedTax        = EstimatedIncomeTaxComparisonItem(currentTaxYear, taxAccountSummaryCY.totalEstimatedTax)
      val cyPlusOneEstimatedTax =
        EstimatedIncomeTaxComparisonItem(nextTaxYear, taxAccountSummaryCYPlusOne.totalEstimatedTax)
      EstimatedIncomeTaxComparisonViewModel(Seq(cyEstimatedTax, cyPlusOneEstimatedTax))
    }

    val taxCodeComparisonModel = {
      val cyTaxCodeIncomeSources        = TaxCodeIncomesForYear(currentTaxYear, taxCodeIncomesCY)
      val cyPlusOneTaxCodeIncomeSources = TaxCodeIncomesForYear(nextTaxYear, taxCodeIncomesCYPlusOne)
      TaxCodeComparisonViewModel(Seq(cyTaxCodeIncomeSources, cyPlusOneTaxCodeIncomeSources))
    }

    val taxFreeAmountComparisonModel = {
      val cyCodingComponents     = CodingComponentForYear(currentTaxYear, codingComponentsCY)
      val cyPlusOneTaxComponents = CodingComponentForYear(nextTaxYear, codingComponentsCYPlusOne)
      val cyTaxSummary           = TaxAccountSummaryForYear(currentTaxYear, taxAccountSummaryCY)
      val cyPlusOneTaxSummary    = TaxAccountSummaryForYear(currentTaxYear, taxAccountSummaryCYPlusOne)

      TaxFreeAmountComparisonViewModel(
        Seq(cyCodingComponents, cyPlusOneTaxComponents),
        Seq(cyTaxSummary, cyPlusOneTaxSummary)
      )
    }

    val employmentViewModel =
      IncomeSourceComparisonViewModel(taxCodeIncomesCY, employmentsCY, taxCodeIncomesCYPlusOne)

    IncomeTaxComparisonViewModel(
      request.fullName,
      estimatedIncomeTaxComparisonViewModel,
      taxCodeComparisonModel,
      taxFreeAmountComparisonModel,
      employmentViewModel,
      isEstimatedPayJourneyComplete
    )
  }
}
