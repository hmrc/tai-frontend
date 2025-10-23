/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.auth.AuthJourney
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, Employment, Payment, PensionIncome}
import uk.gov.hmrc.tai.service.{EmploymentService, RtiService, TaxAccountService}
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
  rtiService: RtiService
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc)
    with Logging {

  def getIncomeTaxYear(nino: Nino, taxYear: TaxYear)(implicit
    hc: HeaderCarrier
  ): Future[IncomeTaxYear] =
    for {
      maybeTaxCodeIncomeDetails <-
        taxAccountService.taxCodeIncomes(nino, taxYear).map(_.toOption).recover { case _ =>
          None
        }
      employmentDetails         <- employmentService.employments(nino, taxYear)
      accounts                  <- rtiService.getPaymentsForYear(nino, taxYear).value
    } yield {
      val maybeTaxCodesMap                                  = maybeTaxCodeIncomeDetails.map(_.groupBy(_.employmentId))
      val incomeTaxHistory: List[IncomeTaxHistoryViewModel] = employmentDetails.map { (employment: Employment) =>
        val maybeTaxCode: Option[TaxCodeIncome] = for {
          taxCodesMap <- maybeTaxCodesMap
          incomes     <- taxCodesMap.get(Some(employment.sequenceNumber))
          taxCode     <- incomes.headOption
        } yield taxCode

        val maybeLastPayment: Option[Payment] =
          accounts match {
            case Right(account) => fetchLastPayment(employment, account)
            case _              => None
          }

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
  private def fetchLastPayment(employment: Employment, accounts: Seq[AnnualAccount]) =
    accounts.find(_.sequenceNumber == employment.sequenceNumber).flatMap(_.payments.lastOption)

  def onPageLoad(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino     = request.taiUser.nino
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
      .map { taxCodeIncome =>
        Ok(incomeTaxHistoryView(request.person, taxCodeIncome))
      }
  }
}
