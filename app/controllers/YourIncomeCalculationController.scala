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

    lazy val taxCodeIncomesFuture = taxAccountService.taxCodeIncomes(nino, TaxYear())
    lazy val employmentFuture     = employmentService.employment(nino, empId)
    lazy val iabdDetailsFuture    = iabdService.getIabds(nino, TaxYear()).value

    for {
      taxCodeIncomeDetails <- taxCodeIncomesFuture
      employmentDetails    <- employmentFuture
      iabdDetails          <- iabdDetailsFuture
      maybeIabdDetail       = iabdDetails.map(_.find(_.employmentSequenceNumber.contains(empId)))
    } yield (taxCodeIncomeDetails, employmentDetails, maybeIabdDetail) match {
      case (Right(taxCodeIncomes), Some(employment), Right(maybeIabd)) =>
        val paymentDetails = paymentsService.filterDuplicates(employment)

        val model                     = YourIncomeCalculationViewModel(
          taxCodeIncomes.find(_.employmentId.contains(empId)),
          employment,
          maybeIabd,
          paymentDetails,
          request.fullName
        )
        implicit val user: AuthedUser = request.taiUser
        Ok(yourIncomeCalculation(model))
      case (taxCodeIncomes, employment, maybeIabdDetail)               =>
        logger.error(
          s"yourIncomeCalculationPage: Unable to retrieve tax code incomes, employment or IABD details for empId: $empId (taxCodeIncomes: ${taxCodeIncomes.isLeft}, employment: ${employment.isEmpty}, iabdDetsils: ${maybeIabdDetail.isLeft})"
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
