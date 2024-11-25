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

import cats.data.EitherT
import cats.implicits._
import controllers.auth.AuthJourney
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{Employment, PensionIncome}
import uk.gov.hmrc.tai.service.{EmploymentService, TaxAccountService}
import uk.gov.hmrc.tai.util.MoneyPounds
import uk.gov.hmrc.tai.util.ViewModelHelper._
import uk.gov.hmrc.tai.viewModels.incomeTaxHistory.{IncomeTaxHistoryViewModel, IncomeTaxYear}
import views.html.incomeTaxHistory.IncomeTaxHistoryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeTaxHistoryController @Inject() (
  val config: ApplicationConfig,
  authenticate: AuthJourney,
  incomeTaxHistoryView: IncomeTaxHistoryView,
  mcc: MessagesControllerComponents,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Logging {

  def getIncomeTaxYear(nino: Nino, taxYear: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, IncomeTaxYear] =
    for {
      maybeTaxCodeIncomeDetails <- taxAccountService.taxCodeIncomes(nino, taxYear)
      employmentDetails         <- employmentService.employments(nino, taxYear)
    } yield {
      val taxCodesMap: Map[Option[Int], Seq[TaxCodeIncome]] = maybeTaxCodeIncomeDetails.groupBy(_.employmentId)
      val incomeTaxHistory: List[IncomeTaxHistoryViewModel] = employmentDetails.map { employment: Employment =>
        val maybeTaxCode = taxCodesMap.get(Some(employment.sequenceNumber)).flatMap(_.headOption)

        val maybeLastPayment = fetchLastPayment(employment, taxYear)
        val isPension = maybeTaxCode.exists(_.componentType == PensionIncome)

        IncomeTaxHistoryViewModel(
          employerName = employment.name,
          isPension = isPension,
          ern = s"${employment.taxDistrictNumber}/${employment.payeNumber}",
          payrollNumber = employment.payrollNumber,
          startDate = employment.startDate,
          maybeEndDate = employment.endDate,
          maybeTaxableIncome = maybeLastPayment.map { payment =>
            withPoundPrefix(MoneyPounds(payment.amountYearToDate))
          },
          maybeIncomeTaxPaid = maybeLastPayment.map { payment =>
            withPoundPrefix(MoneyPounds(payment.taxAmountYearToDate))
          },
          maybeTaxCode = maybeTaxCode.map(_.taxCode)
        )
      }.toList
      IncomeTaxYear(taxYear, incomeTaxHistory)
    }

  // This method follows the pattern set at HistoricIncomeCalculationViewModel.fetchEmploymentAndAnnualAccount
  private def fetchLastPayment(employment: Employment, taxYear: TaxYear) =
    employment.annualAccounts.find(_.taxYear.year == taxYear.year).flatMap(_.payments.lastOption)

  def onPageLoad(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino = request.taiUser.nino
    val taxYears = (TaxYear().year to (TaxYear().year - config.numberOfPreviousYearsToShowIncomeTaxHistory) by -1)
      .map(TaxYear(_))
      .toList

    taxYears
      .traverse(taxYear =>
        getIncomeTaxYear(nino, taxYear).recover { case NonFatal(e) =>
          logger.error(e.getMessage, e)
          IncomeTaxYear(taxYear, Nil)
        }
      )
      .fold(
        _ => errorPagesHandler.internalServerError,
        taxCodeIncome => Ok(incomeTaxHistoryView(request.person, taxCodeIncome))
      )
  }
}
