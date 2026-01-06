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

import cats.implicits.*
import controllers.auth.AuthJourney
import pages.benefits.EndCompanyBenefitsUpdateIncomePage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.{EmploymentService, IabdService, RtiService, TaxAccountService}
import uk.gov.hmrc.tai.util.EmpIdCheck
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel
import views.html.IncomeSourceSummaryView
import uk.gov.hmrc.tai.model.domain.{Available, Employment, IabdDetails}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeSourceSummaryController @Inject() (
  val auditConnector: AuditConnector,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  iabdService: IabdService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  incomeSourceSummary: IncomeSourceSummaryView,
  rtiService: RtiService,
  empIdCheck: EmpIdCheck,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def onPageLoad(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>

    val nino = request.taiUser.nino

    val cacheUpdatedIncomeAmountFuture =
      Future.successful(request.userAnswers.get(EndCompanyBenefitsUpdateIncomePage(empId)).map(_.toInt))

    empIdCheck.checkValidId(empId).flatMap {
      case Some(result) => Future.successful(result)
      case _            =>
        (
          employmentService.employment(nino, empId, TaxYear()),
          taxAccountService.taxCodeIncomes(nino, TaxYear()),
          rtiService.getPaymentsForEmploymentAndYear(nino, TaxYear(), empId).value,
          cacheUpdatedIncomeAmountFuture,
          iabdService.getIabds(nino, TaxYear()).value
        ).mapN {
          case (
                Some(employment),
                taxCodeIncomes,
                payments,
                cacheUpdatedIncomeAmount,
                Right(iabds)
              ) =>
            println("******* " + iabds)
            val estimatedPayOverrides: Option[BigDecimal] =
              iabds
                .find { iabd =>
                  iabd.`type`.contains(IabdDetails.newEstimatedPayCode) &&
                  iabd.employmentSequenceNumber.contains(empId) &&
                  iabd.grossAmount.isDefined
                }
                .map(_.grossAmount.get)
            println("******* " + estimatedPayOverrides)

            val vm = IncomeSourceSummaryViewModel.apply(
              empId = empId,
              displayName = request.fullName,
              optTaxCodeIncome =
                taxCodeIncomes.fold(_ => None, _.find(_.employmentId.contains(employment.sequenceNumber))),
              employment = employment,
              payments = payments.toOption.flatten,
              // TODO: handle a failure vs no payment present
              // The way the rti availability is implemented using a stub Annual account is not compatible with None type
              // So when no annual account found for an employment, assuming rti is down.
              // The service also does not handle the case when there is no payments but assume the rti api not been available.
              rtiAvailable = payments.fold(_ => false, _.fold(false)(_.realTimeStatus == Available)),
              cacheUpdatedIncomeAmount = cacheUpdatedIncomeAmount,
              estimatedPayOverrides = estimatedPayOverrides
            )

            Ok(incomeSourceSummary(vm))

          case _ =>
            errorPagesHandler.internalServerError("Error while fetching income summary details")
        } recover { case NonFatal(e) =>
          errorPagesHandler.internalServerError("IncomeSourceSummaryController exception", Some(e))
        }
    }
  }
}
