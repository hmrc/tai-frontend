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

import cats.data.EitherT
import cats.implicits.*
import controllers.auth.{AuthJourney, AuthenticatedRequest}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, Employment, Payment, PensionIncome}
import uk.gov.hmrc.tai.service.{EmploymentService, RtiService, TaxAccountService}
import uk.gov.hmrc.tai.util.MoneyPounds
import uk.gov.hmrc.tai.util.ViewModelHelper.*
import uk.gov.hmrc.tai.viewModels.incomeTaxHistory.{IncomeTaxHistoryViewModel, IncomeTaxYear}
import views.html.incomeTaxHistory.IncomeTaxHistoryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxHistoryController @Inject() (
  val config: ApplicationConfig,
  authenticate: AuthJourney,
  incomeTaxHistoryView: IncomeTaxHistoryView,
  mcc: MessagesControllerComponents,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  rtiService: RtiService,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc)
    with Logging {

  private def lastPaymentForEmployment(accounts: Either[UpstreamErrorResponse, Seq[AnnualAccount]], empId: Int) =
    accounts.fold(
      _ => None,
      accountSeq =>
        accountSeq
          .find(_.sequenceNumber == empId)
          .flatMap(_.latestPayment)
    )

  def getIncomeTaxYear(nino: Nino, taxYear: TaxYear)(implicit
    request: AuthenticatedRequest[AnyContent],
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, IncomeTaxYear] =
    EitherT(for {
      maybeTaxCodeIncomeDetails <-
        taxAccountService.taxCodeIncomes(nino, taxYear).map(_.toOption).recover { case _ =>
          None
        }
      employmentDetails         <- employmentService.employments(nino, taxYear).value
      accounts                  <- rtiService.getAllPaymentsForYear(nino, taxYear).value
    } yield (maybeTaxCodeIncomeDetails, employmentDetails, accounts) match {
      case (_, Right(employmentDetails), _) =>
        val maybeTaxCodesMap                                  = maybeTaxCodeIncomeDetails.map(_.groupBy(_.employmentId))
        val incomeTaxHistory: List[IncomeTaxHistoryViewModel] = employmentDetails.map { (employment: Employment) =>
          val maybeTaxCode: Option[TaxCodeIncome] = for {
            taxCodesMap <- maybeTaxCodesMap
            incomes     <- taxCodesMap.get(Some(employment.sequenceNumber))
            taxCode     <- incomes.headOption
          } yield taxCode

          val maybeLastPayment: Option[Payment] = lastPaymentForEmployment(accounts, employment.sequenceNumber)

          val isPension      = maybeTaxCode.exists(_.componentType == PensionIncome)
          val maybeTaxAmount = maybeLastPayment.map(_.taxAmountYearToDate)

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
            maybeIncomeTaxPaid = maybeTaxAmount.map { amount =>
              withPoundPrefix(MoneyPounds(amount))
            },
            isIncomeTaxRefund = maybeTaxAmount.exists(_ < 0),
            maybeTaxCode = maybeTaxCode.map(_.taxCode)
          )
        }.toList
        Right(IncomeTaxYear(taxYear, incomeTaxHistory))
      case (_, Left(error), _)              => Left(error)
    })

  def onPageLoad(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino     = request.taiUser.nino
    val messages = request2Messages
    val taxYears = (TaxYear().year to (TaxYear().year - config.numberOfPreviousYearsToShowIncomeTaxHistory) by -1)
      .map(TaxYear(_))
      .toList

    taxYears
      .traverse { taxYear =>
        getIncomeTaxYear(nino, taxYear).value.map {
          case Right(incomeTaxYear) =>
            Right(incomeTaxYear)

          case Left(e) if e.statusCode == 502 || e.statusCode == 503 || e.statusCode == 504 =>
            Left(e)

          case Left(e) =>
            logger.error(e.getMessage, e)
            Right(IncomeTaxYear(taxYear, Nil))
        }
      }
      .map { results =>
        results.collectFirst { case Left(e) => e } match {
          case Some(e) =>
            InternalServerError(errorPagesHandler.error5xx(messages("tai.technical.error.message")))
          case None    =>
            val years = results.collect { case Right(y) => y }
            Ok(incomeTaxHistoryView(request.person, years))
        }
      }
  }
}
