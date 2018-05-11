/*
 * Copyright 2018 HM Revenue & Customs
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


import controllers.auth.WithAuthorisedForTaiLite
import controllers.{ServiceCheckLite, TaiBaseController}
import uk.gov.hmrc.tai.forms.income.bbsi.BankAccountsDecisionForm
import uk.gov.hmrc.tai.viewModels.income.BbsiAccountsDecisionViewModel
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.tai.service.{BbsiService, PersonService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.tai.model.domain.BankAccount
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{FrontEndDelegationConnector, FrontendAuthConnector, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.util.BankAccountDecisionConstants

import scala.concurrent.Future


trait BbsiController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with BankAccountDecisionConstants {

  def personService: PersonService

  def bbsiService: BbsiService

  def accounts(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            for {
              untaxedInterest <- bbsiService.untaxedInterest(Nino(user.getNino))
            } yield {
              Ok(views.html.incomes.bbsi.bank_building_society_accounts(untaxedInterest))
            }
          }

  }

  def endConfirmation(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.incomes.bbsi.bank_building_society_confirmation()))
          }
  }

  def updateConfirmation(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.incomes.bbsi.bank_building_society_confirmation()))
          }
  }

  def removeConfirmation(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.incomes.bbsi.bank_building_society_confirmation()))
          }
  }

  def untaxedInterestDetails(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            bbsiService.untaxedInterest(Nino(user.getNino)) map { untaxedInterest =>
              Ok(views.html.incomes.bbsi.bank_building_society_overview(untaxedInterest.amount))
            }
          }
  }

  def decision(id: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            bbsiService.bankAccount(Nino(user.getNino), id) map {
              case Some(BankAccount(_, Some(_), Some(_), Some(bankName), _, _)) =>
                val viewModel = BbsiAccountsDecisionViewModel(id, bankName)
                Ok(views.html.incomes.bbsi.bank_building_society_accounts_decision(viewModel, BankAccountsDecisionForm.createForm))
              case Some(_) => throw new RuntimeException(s"Bank account does not contain name, number or sortcode for nino: [${user.getNino}] and id: [$id]")
              case None => NotFound
            }
          }
  }

  def handleDecisionPage(id: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
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
            formData => {
              formData.bankAccountsDecision match {
                case Some(UpdateInterest) =>
                  Future.successful(Redirect(controllers.income.bbsi.routes.BbsiUpdateAccountController.captureInterest(id)))
                case Some(CloseAccount) =>
                  Future.successful(Redirect(controllers.income.bbsi.routes.BbsiCloseAccountController.captureCloseDate(id)))
                case Some(RemoveAccount) =>
                  Future.successful(Redirect(controllers.income.bbsi.routes.BbsiRemoveAccountController.checkYourAnswers(id)))
                case _ => Future.successful(Redirect(controllers.income.bbsi.routes.BbsiController.accounts()))
              }
            }
          )
  }
}
// $COVERAGE-OFF$
object BbsiController extends BbsiController {

  override val personService = PersonService
  override val bbsiService = BbsiService

  override protected val delegationConnector = FrontEndDelegationConnector
  override protected val authConnector = FrontendAuthConnector
  override implicit val templateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}
// $COVERAGE-ON$