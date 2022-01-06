/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import javax.inject.{Inject, Named}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.forms.PayPeriodForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import views.html.incomes.PayPeriodView

import scala.concurrent.ExecutionContext

class IncomeUpdatePayPeriodController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  payPeriodView: PayPeriodView,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with UpdatedEstimatedPayJourneyCache {

  def payPeriodPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    for {
      incomeSourceEither <- IncomeSource.create(journeyCacheService)
      payPeriod          <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
      payPeriodInDays    <- journeyCacheService.currentValue(UpdateIncome_OtherInDaysKey)
    } yield {
      val form: Form[PayPeriodForm] = PayPeriodForm.createForm(None).fill(PayPeriodForm(payPeriod, payPeriodInDays))
      incomeSourceEither match {
        case Right(incomeSource) => Ok(payPeriodView(form, incomeSource.id, incomeSource.name))
        case Left(_)             => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
      }
    }
  }

  def handlePayPeriod: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val payPeriod: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("payPeriod").flatMap(_.headOption))

    PayPeriodForm
      .createForm(None, payPeriod)
      .bindFromRequest()
      .fold(
        formWithErrors => {

          for {
            incomeSourceEither <- IncomeSource.create(journeyCacheService)
          } yield {
            val isDaysError = formWithErrors.errors.exists { error =>
              error.key == PayPeriodForm.OTHER_IN_DAYS_KEY
            }
            incomeSourceEither match {
              case Right(incomeSource) =>
                BadRequest(payPeriodView(formWithErrors, incomeSource.id, incomeSource.name, !isDaysError))
              case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
            }
          }
        },
        formData => {
          val cacheMap = formData.otherInDays match {
            case Some(days) =>
              Map(
                UpdateIncome_PayPeriodKey   -> formData.payPeriod.getOrElse(""),
                UpdateIncome_OtherInDaysKey -> days.toString)
            case _ => Map(UpdateIncome_PayPeriodKey -> formData.payPeriod.getOrElse(""))
          }

          journeyCache(cacheMap = cacheMap) map { _ =>
            Redirect(routes.IncomeUpdatePayslipAmountController.payslipAmountPage())
          }
        }
      )
  }

}
