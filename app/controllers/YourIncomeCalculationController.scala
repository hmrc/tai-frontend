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

import controllers.auth.*
import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.IabdDetails
import uk.gov.hmrc.tai.service.{EmploymentService, IabdService, PaymentsService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.{HistoricIncomeCalculationViewModel, YourIncomeCalculationViewModel}
import views.html.incomes.{HistoricIncomeCalculationView, YourIncomeCalculationView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourIncomeCalculationController @Inject() (
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  iabdService: IabdService,
  paymentsService: PaymentsService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  historicIncomeCalculation: HistoricIncomeCalculationView,
  yourIncomeCalculation: YourIncomeCalculationView,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc)
    with Logging {

  def yourIncomeCalculationPage(empId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      incomeCalculationPage(empId)
  }

  private def incomeCalculationPage(empId: Int)(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val nino = request.taiUser.nino

    val taxCodeIncomesFuture = taxAccountService.taxCodeIncomes(nino, TaxYear())
    val employmentFuture     = employmentService.employment(nino, empId)

    val iabdForEmpFuture: Future[Option[IabdDetails]] =
      iabdService
        .getIabds(nino, TaxYear())
        .value
        .map {
          case Right(all) =>
            all.find(_.employmentSequenceNumber.contains(empId))
          case Left(e)    =>
            logger.warn(s"IABD fetch failed for empId=$empId: ${e.statusCode} ${e.message}")
            None
        }
        .recover { case t =>
          logger.warn(s"IABD fetch threw for empId=$empId: ${t.getMessage}", t)
          None
        }

    for {
      taxCodeEither <- taxCodeIncomesFuture
      employmentOpt <- employmentFuture
      iabdOpt       <- iabdForEmpFuture
    } yield (taxCodeEither, employmentOpt) match {
      case (Right(taxCodeIncomes), Some(employment)) =>
        val paymentDetails            = paymentsService.filterDuplicates(employment)
        val model                     = YourIncomeCalculationViewModel(
          taxCodeIncome = taxCodeIncomes.find(_.employmentId.contains(empId)),
          employment = employment,
          maybeIabd = iabdOpt,
          paymentDetails = paymentDetails,
          username = request.fullName
        )
        implicit val user: AuthedUser = request.taiUser
        Ok(yourIncomeCalculation(model))

      case (tcEither, empOpt) =>
        logger.error(
          s"yourIncomeCalculationPage: Unable to retrieve tax code incomes or employment for empId=$empId " +
            s"(taxCodeIncomesError=${tcEither.isLeft}, employmentMissing=${empOpt.isEmpty})"
        )
        errorPagesHandler.internalServerError("Error while fetching RTI details")
    }
  }

  def yourIncomeCalculationHistoricYears(year: TaxYear, empId: Int): Action[AnyContent] =
    incomeCalculationHistoricYears(year, empId)

  private def incomeCalculationHistoricYears(year: TaxYear, empId: Int): Action[AnyContent] =
    authenticate.authWithValidatePerson.async { implicit request =>
      if (year <= TaxYear().prev) {
        val nino = request.taiUser.nino

        employmentService.employments(nino, year) map { employments =>
          val historicIncomeCalculationViewModel = HistoricIncomeCalculationViewModel(employments, empId, year)

          historicIncomeCalculationViewModel.realTimeStatus.toString match {
            case ("TemporarilyUnavailable") =>
              errorPagesHandler.internalServerError(
                "Employment contains stub annual account data found meaning payment information can't be displayed"
              )
            case _                          => Ok(historicIncomeCalculation(historicIncomeCalculationViewModel))
          }
        }

      } else {
        Future.successful(
          errorPagesHandler.internalServerError(s"yourIncomeCalculationHistoricYears: Doesn't support year $year")
        )
      }
    }
}
