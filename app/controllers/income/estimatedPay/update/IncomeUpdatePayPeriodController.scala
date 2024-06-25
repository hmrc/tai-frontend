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

import cats.implicits._
import controllers.TaiBaseController
import controllers.auth.{AuthJourney, AuthedUser}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.forms.income.incomeCalculator.PayPeriodForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.PayPeriodConstants
import uk.gov.hmrc.tai.util.constants.journeyCache._
import views.html.incomes.PayPeriodView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class IncomeUpdatePayPeriodController @Inject() (
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  payPeriodView: PayPeriodView,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with UpdatedEstimatedPayJourneyCache {

  def payPeriodPage: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (
      IncomeSource.create(journeyCacheService),
      journeyCacheService
        .optionalValues(Seq(UpdateIncomeConstants.PayPeriodKey, UpdateIncomeConstants.OtherInDaysKey))
    ).mapN {
      case (Right(incomeSource), payPeriod :: payPeriodInDays :: _) =>
        val form: Form[PayPeriodForm] = PayPeriodForm.createForm(None).fill(PayPeriodForm(payPeriod, payPeriodInDays))
        Ok(payPeriodView(form, incomeSource.id, incomeSource.name))
      case _ => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }

  }

  def handlePayPeriod: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val payPeriod: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("payPeriod").flatMap(_.headOption))

    PayPeriodForm
      .createForm(None, payPeriod)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          for {
            incomeSourceEither <- IncomeSource.create(journeyCacheService)
          } yield {
            val isDaysError = formWithErrors.errors.exists { error =>
              error.key == PayPeriodConstants.OtherInDaysKey
            }
            incomeSourceEither match {
              case Right(incomeSource) =>
                BadRequest(payPeriodView(formWithErrors, incomeSource.id, incomeSource.name, !isDaysError))
              case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
            }
          },
        formData => {
          val cacheMap = formData.otherInDays match {
            case Some(days) =>
              Map(
                UpdateIncomeConstants.PayPeriodKey   -> formData.payPeriod.getOrElse(""),
                UpdateIncomeConstants.OtherInDaysKey -> days.toString
              )
            case _ => Map(UpdateIncomeConstants.PayPeriodKey -> formData.payPeriod.getOrElse(""))
          }

          journeyCache(cacheMap = cacheMap) map { _ =>
            Redirect(routes.IncomeUpdatePayslipAmountController.payslipAmountPage())
          }
        }
      )
  }

}
