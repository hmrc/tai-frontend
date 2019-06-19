/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.income.bbsi


import javax.inject.{Inject, Named}
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.DateForm
import uk.gov.hmrc.tai.forms.income.bbsi.BankAccountClosingInterestForm
import uk.gov.hmrc.tai.model.domain.BankAccount
import uk.gov.hmrc.tai.model.{CloseAccountRequest, TaxYear}
import uk.gov.hmrc.tai.service.BbsiService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import uk.gov.hmrc.tai.viewModels.income.BbsiClosedCheckYourAnswersViewModel

import scala.concurrent.Future


class BbsiCloseAccountController @Inject()(bbsiService: BbsiService,
                                           authenticate: AuthAction,
                                           validatePerson: ValidatePerson,
                                           @Named("Close Bank Account") journeyCacheService: JourneyCacheService,
                                           override implicit val partialRetriever: FormPartialRetriever,
                                           override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with JourneyCacheConstants {

  def futureDateValidation: (LocalDate => Boolean, String) = ((date: LocalDate) => !date.isAfter(LocalDate.now()), Messages("tai.date.error.future"))

  def captureCloseDate(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      (for {
        bankAccount <- bbsiService.bankAccount(Nino(user.getNino), id)
        dateCache <- journeyCacheService.currentValueAsDate(CloseBankAccountDateKey)
      } yield {
        val form = DateForm(Seq(futureDateValidation), Messages("tai.closeBankAccount.closeDateForm.blankDate")).form
        val updatedForm = dateCache match {
          case Some(d) => form.fill(d)
          case _ => form
        }
        bankAccount match {
          case Some(BankAccount(_, Some(_), Some(_), Some(bankName), _, _)) =>
            Ok(views.html.incomes.bbsi.close.bank_building_society_close_date(updatedForm, bankName, id))
          case Some(_) => throw new RuntimeException(s"Bank account does not contain name, number or sortcode for nino: [${user.getNino}] and id: [$id]")
          case None => throw new RuntimeException(s"Bank account not found for nino: [${user.getNino}] and id: [$id]")
        }
      }).recover {
        case e: Exception => internalServerError(e.getMessage)
      }
  }

  def submitCloseDate(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      bbsiService.bankAccount(Nino(user.getNino), id) flatMap {
        case Some(BankAccount(_, Some(_), Some(_), Some(bankName), _, _)) =>
          DateForm(Seq(futureDateValidation), Messages("tai.closeBankAccount.closeDateForm.blankDate", bankName))
            .form.bindFromRequest()
            .fold(
              formWithErrors => {
                Future.successful(
                  BadRequest(views.html.incomes.bbsi.close.bank_building_society_close_date(formWithErrors, bankName, id)))
              },
              date => {
                journeyCacheService.cache(Map(CloseBankAccountDateKey -> date.toString, CloseBankAccountNameKey -> bankName)).map(_ =>
                  if (TaxYear().within(date)) {
                    Redirect(controllers.income.bbsi.routes.BbsiCloseAccountController.captureClosingInterest(id))
                  } else {
                    Redirect(controllers.income.bbsi.routes.BbsiCloseAccountController.checkYourAnswers(id))
                  })
              }
            )
        case Some(_) => Future.successful(internalServerError(s"Bank account does not contain name, number or sortcode for nino: [${user.getNino}] and id: [$id]"))
        case None => Future.successful(internalServerError(s"Bank account not found for nino: [${user.getNino}] and id: [$id]"))
      }
  }

  def captureClosingInterest(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      (for {
        interestCache <- journeyCacheService.optionalValues(CloseBankAccountInterestChoice, CloseBankAccountInterestKey)
        bankAccount <- bbsiService.bankAccount(Nino(user.getNino), id)
      } yield bankAccount match {
        case Some(BankAccount(_, Some(_), Some(_), Some(bankName), _, _)) =>
          Ok(views.html.incomes.bbsi.close.bank_building_society_closing_interest(id, BankAccountClosingInterestForm.form.fill
          (BankAccountClosingInterestForm(interestCache(0), interestCache(1)))))
        case Some(_) => throw new RuntimeException(s"Bank account does not contain name, number or sortcode for nino: [${user.getNino}] and id: [$id]")
        case None => throw new RuntimeException(s"Bank account not found for nino: [${user.getNino}] and id: [$id]")
      }) recover {
        case e: Exception => internalServerError(e.getMessage())
      }
  }

  def submitClosingInterest(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      BankAccountClosingInterestForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.incomes.bbsi.close.bank_building_society_closing_interest(id, formWithErrors)))
        },
        form => {
          journeyCacheService.cache(Map(CloseBankAccountInterestKey -> FormHelper.stripNumber(form.closingInterestEntry.getOrElse("")),
            CloseBankAccountInterestChoice -> form.closingBankAccountInterestChoice.getOrElse(""))) map { _ =>
            Redirect(controllers.income.bbsi.routes.BbsiCloseAccountController.checkYourAnswers(id))
          }
        }
      )
  }

  def submitYourAnswers(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      for {
        endDate <- journeyCacheService.mandatoryValueAsDate(CloseBankAccountDateKey)
        closingInterest <- journeyCacheService.currentValueAs(CloseBankAccountInterestKey, string => BigDecimal(string))
        _ <- bbsiService.closeBankAccount(Nino(user.getNino), id, CloseAccountRequest(endDate, closingInterest))
      } yield {
        journeyCacheService.flush()
        Redirect(controllers.income.bbsi.routes.BbsiController.endConfirmation())
      }
  }

  def checkYourAnswers(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      journeyCacheService.collectedValues(Seq(CloseBankAccountDateKey), Seq(CloseBankAccountNameKey, CloseBankAccountInterestKey)) map { seq =>
        val model = BbsiClosedCheckYourAnswersViewModel(id, seq._1.head, seq._2.head, seq._2.tail.head)
        Ok(views.html.incomes.bbsi.close.bank_building_society_check_your_answers(model))
      }
  }
}
