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

package controllers.income.estimatedPay.update

import controllers.TaiBaseController
import controllers.auth.{AuthJourney, AuthedUser}
import pages.income.{UpdateIncomeOtherInDaysPage, UpdateIncomePayPeriodPage}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.forms.income.incomeCalculator.PayPeriodForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import views.html.incomes.PayPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class IncomeUpdatePayPeriodController @Inject() (
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  payPeriodView: PayPeriodView,
  journeyCacheNewRepository: JourneyCacheNewRepository
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def payPeriodPage: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers = request.userAnswers
    val payPeriod = userAnswers.get(UpdateIncomePayPeriodPage)
    val payPeriodInDays = userAnswers.get(UpdateIncomeOtherInDaysPage)

    IncomeSource.create(journeyCacheNewRepository, userAnswers).map {
      case Right(incomeSource) if payPeriod.isDefined && payPeriodInDays.isDefined =>
        val form: Form[PayPeriodForm] = PayPeriodForm.createForm(None).fill(PayPeriodForm(payPeriod, payPeriodInDays))
        Ok(payPeriodView(form, incomeSource.id, incomeSource.name))
      case _ => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def handlePayPeriod: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val payPeriod: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("payPeriod").flatMap(_.headOption))

    PayPeriodForm
      .createForm(payPeriod)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          for {
            incomeSourceEither <- IncomeSource.create(journeyCacheNewRepository, request.userAnswers)
          } yield incomeSourceEither match {
            case Right(incomeSource) =>
              BadRequest(payPeriodView(formWithErrors, incomeSource.id, incomeSource.name))
            case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
          },
        formData => {
          val updatedUserAnswers = formData.otherInDays match {
            case Some(days) =>
              request.userAnswers
                .set(UpdateIncomePayPeriodPage, formData.payPeriod.getOrElse(""))
                .flatMap(_.set(UpdateIncomeOtherInDaysPage, days))
            case _ =>
              request.userAnswers.set(UpdateIncomePayPeriodPage, formData.payPeriod.getOrElse(""))
          }

          updatedUserAnswers match {
            case Success(newUserAnswers) =>
              journeyCacheNewRepository.set(newUserAnswers).map { _ =>
                Redirect(routes.IncomeUpdatePayslipAmountController.payslipAmountPage())
              }
            case Failure(_) =>
              Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
          }
        }
      )
  }

}
