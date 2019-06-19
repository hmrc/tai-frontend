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
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.income.bbsi.{BankAccountsDecisionForm, BankAccountsDecisionFormData}
import uk.gov.hmrc.tai.model.domain.BankAccount
import uk.gov.hmrc.tai.service.BbsiService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{BankAccountDecisionConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.income.BbsiAccountsDecisionViewModel

import scala.concurrent.Future


class BbsiController @Inject()(bbsiService: BbsiService,
                               authenticate: AuthAction,
                               validatePerson: ValidatePerson,
                               @Named("Update Bank Account Choice") journeyCacheService: JourneyCacheService,
                               override implicit val partialRetriever: FormPartialRetriever,
                               override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with BankAccountDecisionConstants
  with JourneyCacheConstants {

  def accounts(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser
      for {
        untaxedInterest <- bbsiService.untaxedInterest(Nino(user.getNino))
      } yield {
        Ok(views.html.incomes.bbsi.bank_building_society_accounts(untaxedInterest))
      }
  }

  def endConfirmation(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      Future.successful(Ok(views.html.incomes.bbsi.bank_building_society_confirmation()))
  }

  def updateConfirmation(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      Future.successful(Ok(views.html.incomes.bbsi.bank_building_society_confirmation()))
  }

  def removeConfirmation(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      Future.successful(Ok(views.html.incomes.bbsi.bank_building_society_confirmation()))
  }

  def untaxedInterestDetails(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      (bbsiService.untaxedInterest(Nino(user.getNino)) map { untaxedInterest =>
        Ok(views.html.incomes.bbsi.bank_building_society_overview(untaxedInterest.amount))
      }).recover {
        case e: Exception => internalServerError(e.getMessage)
      }
  }

  def decision(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      (for {
        bankAccount <- bbsiService.bankAccount(Nino(user.getNino), id)
        cacheDetails <- journeyCacheService.currentValue(UpdateBankAccountUserChoiceKey)
      } yield bankAccount match {
        case Some(BankAccount(_, Some(_), Some(_), Some(bankName), _, _)) =>
          val viewModel = BbsiAccountsDecisionViewModel(id, bankName)
          Ok(views.html.incomes.bbsi.bank_building_society_accounts_decision(viewModel, BankAccountsDecisionForm.createForm.fill(BankAccountsDecisionFormData(cacheDetails))))
        case Some(_) => throw new RuntimeException(s"Bank account does not contain name, number or sortcode for nino: [${user.getNino}] and id: [$id]")
        case None => NotFound
      }).recover {
        case e: Exception => internalServerError(e.getMessage)
      }
  }

  def handleDecisionPage(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      BankAccountsDecisionForm.createForm.bindFromRequest.fold(
        formWithErrors => {
          bbsiService.bankAccount(Nino(user.getNino), id) flatMap {
            case Some(BankAccount(_, Some(_), Some(_), Some(bankName), _, _)) =>
              val viewModel = BbsiAccountsDecisionViewModel(id, bankName)
              Future.successful(BadRequest(views.html.incomes.bbsi.bank_building_society_accounts_decision(viewModel, formWithErrors)))
            case Some(_) => throw new RuntimeException(s"Bank account does not contain name, number or sortcode for nino: [${user.getNino}] and id: [$id]")
            case None => Future.successful(NotFound)
          }
        },
        (formData: BankAccountsDecisionFormData) => {

          journeyCacheService.cache(UpdateBankAccountUserChoiceKey, formData.bankAccountsDecision.getOrElse("")) map { _ =>

            formData.bankAccountsDecision match {
              case Some(UpdateInterest) =>
                Redirect(controllers.income.bbsi.routes.BbsiUpdateAccountController.captureInterest(id))
              case Some(CloseAccount) =>
                Redirect(controllers.income.bbsi.routes.BbsiCloseAccountController.captureCloseDate(id))
              case Some(RemoveAccount) =>
                Redirect(controllers.income.bbsi.routes.BbsiRemoveAccountController.checkYourAnswers(id))
              case _ => Redirect(controllers.income.bbsi.routes.BbsiController.accounts())
            }
          }
        }
      ).recover {
        case e: Exception => internalServerError(e.getMessage)
      }
  }
}
