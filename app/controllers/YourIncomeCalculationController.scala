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

import controllers.auth._
import play.api.mvc._
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.{EmploymentService, PaymentsService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.{HistoricIncomeCalculationViewModel, YourIncomeCalculationViewModel}
import views.html.incomes.{HistoricIncomeCalculationView, YourIncomeCalculationView}
import views.html.print.HistoricIncomePrintView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourIncomeCalculationController @Inject() (
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  paymentsService: PaymentsService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  historicIncomeCalculation: HistoricIncomeCalculationView,
  yourIncomeCalculation: YourIncomeCalculationView,
  historicIncomePrintView: HistoricIncomePrintView,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def yourIncomeCalculationPage(empId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      incomeCalculationPage(empId)
  }

  private def incomeCalculationPage(empId: Int)(implicit request: AuthenticatedRequest[AnyContent]) = {
    val nino = request.taiUser.nino

    lazy val taxCodeIncomesFuture = taxAccountService.taxCodeIncomes(nino, TaxYear())
    lazy val employmentFuture = employmentService.employment(nino, empId)

    for {
      taxCodeIncomeDetails <- taxCodeIncomesFuture
      employmentDetails    <- employmentFuture
    } yield (taxCodeIncomeDetails, employmentDetails) match {
      case (Right(taxCodeIncomes), Some(employment)) =>
        val paymentDetails = paymentsService.filterDuplicates(employment)

        val model = YourIncomeCalculationViewModel(
          taxCodeIncomes.find(_.employmentId.contains(empId)),
          employment,
          paymentDetails,
          request.fullName
        )
        implicit val user: AuthedUser = request.taiUser
        Ok(yourIncomeCalculation(model))
      case _ => errorPagesHandler.internalServerError("Error while fetching RTI details")
    }
  }

  def yourIncomeCalculationHistoricYears(year: TaxYear, empId: Int): Action[AnyContent] =
    yourIncomeCalculationHistoricYears(year, empId, printPage = false)

  def printYourIncomeCalculationHistoricYears(year: TaxYear, empId: Int): Action[AnyContent] =
    yourIncomeCalculationHistoricYears(year, empId, printPage = true)

  private def yourIncomeCalculationHistoricYears(year: TaxYear, empId: Int, printPage: Boolean): Action[AnyContent] =
    authenticate.authWithValidatePerson.async { implicit request =>
      if (year <= TaxYear().prev) {
        val nino = request.taiUser.nino

        employmentService.employments(nino, year) map { employments =>
          val historicIncomeCalculationViewModel = HistoricIncomeCalculationViewModel(employments, empId, year)

          (printPage, historicIncomeCalculationViewModel.realTimeStatus.toString) match {
            case (_, "TemporarilyUnavailable") =>
              errorPagesHandler.internalServerError(
                "Employment contains stub annual account data found meaning payment information can't be displayed"
              )
            case (true, _) =>
              Ok(historicIncomePrintView(historicIncomeCalculationViewModel))
            case (false, _) => Ok(historicIncomeCalculation(historicIncomeCalculationViewModel))
          }
        }

      } else {
        Future.successful(
          errorPagesHandler.internalServerError(s"yourIncomeCalculationHistoricYears: Doesn't support year $year")
        )
      }
    }
}
