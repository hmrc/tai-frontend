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


import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import controllers.{ServiceCheckLite, TaiBaseController}
import uk.gov.hmrc.tai.forms.incomes.bbsi.UpdateInterestForm
import uk.gov.hmrc.tai.viewModels.income.{BbsiUpdateAccountViewModel, BbsiUpdateInterestViewModel}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Result}
import play.mvc.Http.Request
import uk.gov.hmrc.tai.service.{BbsiService, JourneyCacheService, PersonService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.tai.config.{FrontEndDelegationConnector, FrontendAuthConnector, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.model.AmountRequest
import uk.gov.hmrc.tai.model.domain.BankAccount
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

import scala.concurrent.Future


trait BbsiUpdateAccountController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with JourneyCacheConstants {

  def personService: PersonService

  def bbsiService: BbsiService

  def journeyCache: JourneyCacheService

  def captureInterest(id: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            for{
              cachedData <- journeyCache.currentValue(UpdateBankAccountInterestKey)
              untaxedInterest <-  bbsiService.untaxedInterest(Nino(user.getNino))
            }yield untaxedInterest.bankAccounts.find(_.id == id) match {
                case Some(bankAccount) =>
                  val model = BbsiUpdateAccountViewModel(id,
                    untaxedInterest.amount, bankAccount.bankName.getOrElse(""))
                  val form =  UpdateInterestForm.form.fill(cachedData.getOrElse(""))
                  Ok(views.html.incomes.bbsi.update.bank_building_society_update_interest(model, form))
                case None => throw new RuntimeException(s"Not able to found account with id $id")
              }
            }

          }


  def submitInterest(id: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          bbsiService.untaxedInterest(Nino(user.getNino)) flatMap { untaxedInterest =>
            untaxedInterest.bankAccounts.find(_.id == id) match {
              case Some(bankAccount) =>
                UpdateInterestForm.form.bindFromRequest().fold(
                  formWithErrors => {
                    val model = BbsiUpdateAccountViewModel(id,
                      untaxedInterest.amount, bankAccount.bankName.getOrElse(""))
                    Future.successful(BadRequest(views.html.incomes.bbsi.update.bank_building_society_update_interest(model, formWithErrors)))
                  },
                  interest => {
                    journeyCache.cache(Map(UpdateBankAccountInterestKey -> interest, UpdateBankAccountNameKey -> bankAccount.bankName.getOrElse(""))).map(_ =>
                      Redirect(controllers.income.bbsi.routes.BbsiUpdateAccountController.checkYourAnswers(id)))
                  }
                )
              case None => throw new RuntimeException(s"Not able to found account with id $id")
            }
          }
  }

  def checkYourAnswers(id: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCache.mandatoryValues(UpdateBankAccountInterestKey, UpdateBankAccountNameKey) map { mandatory =>
              val interest = FormHelper.stripNumber(mandatory.head)
              val bankName = mandatory.last
              Ok(views.html.incomes.bbsi.update.bank_building_society_check_your_answers(BbsiUpdateInterestViewModel(id, interest, bankName)))
            }
          }
  }

  def submitYourAnswers(id: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          for {
            mandatory <- journeyCache.mandatoryValues(UpdateBankAccountInterestKey, UpdateBankAccountNameKey)
            _ <- bbsiService.updateBankAccountInterest(Nino(user.getNino), id, AmountRequest(BigDecimal(FormHelper.stripNumber(mandatory.head))))
            _ <- journeyCache.flush()
          } yield Redirect(controllers.income.bbsi.routes.BbsiController.updateConfirmation())

  }

}
// $COVERAGE-OFF$
object BbsiUpdateAccountController extends BbsiUpdateAccountController {

  override val personService = PersonService
  override val bbsiService = BbsiService

  override val journeyCache = JourneyCacheService(UpdateBankAccountJourneyKey)

  override protected val delegationConnector = FrontEndDelegationConnector
  override protected val authConnector = FrontendAuthConnector

  override implicit val templateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever = TaiHtmlPartialRetriever
}
// $COVERAGE-ON$
