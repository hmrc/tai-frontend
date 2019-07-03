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
import uk.gov.hmrc.tai.forms.incomes.bbsi.UpdateInterestForm
import uk.gov.hmrc.tai.model.AmountRequest
import uk.gov.hmrc.tai.service.BbsiService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import uk.gov.hmrc.tai.viewModels.income.{BbsiUpdateAccountViewModel, BbsiUpdateInterestViewModel}

import scala.concurrent.Future


class BbsiUpdateAccountController @Inject()(bbsiService: BbsiService,
                                            authenticate: AuthAction,
                                            validatePerson: ValidatePerson,
                                            @Named("Update Bank Account") journeyCacheService: JourneyCacheService,
                                            override implicit val partialRetriever: FormPartialRetriever,
                                            override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with JourneyCacheConstants {

  def captureInterest(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      (for {
          cachedData <- journeyCacheService.currentValue(UpdateBankAccountInterestKey)
          untaxedInterest <- bbsiService.untaxedInterest(Nino(user.getNino))
        } yield untaxedInterest.bankAccounts.find(_.id == id) match {
          case Some(bankAccount) =>
            val model = BbsiUpdateAccountViewModel(id,
              untaxedInterest.amount, bankAccount.bankName.getOrElse(""))
            val form = UpdateInterestForm.form.fill(cachedData.getOrElse(""))
            Ok(views.html.incomes.bbsi.update.bank_building_society_update_interest(model, form))
          case None => throw new RuntimeException(s"Not able to found account with id $id")
      }).recover {
        case e: Exception => internalServerError(e.getMessage)
      }
  }


  def submitInterest(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      (bbsiService.untaxedInterest(Nino(user.getNino)) flatMap { untaxedInterest =>
        untaxedInterest.bankAccounts.find(_.id == id) match {
          case Some(bankAccount) =>
            UpdateInterestForm.form.bindFromRequest().fold(
              formWithErrors => {
                val model = BbsiUpdateAccountViewModel(id,
                  untaxedInterest.amount, bankAccount.bankName.getOrElse(""))
                Future.successful(BadRequest(views.html.incomes.bbsi.update.bank_building_society_update_interest(model, formWithErrors)))
              },
              interest => {
                journeyCacheService.cache(Map(UpdateBankAccountInterestKey -> interest, UpdateBankAccountNameKey -> bankAccount.bankName.getOrElse(""))).map(_ =>
                  Redirect(controllers.income.bbsi.routes.BbsiUpdateAccountController.checkYourAnswers(id)))
              }
            )
          case None => throw new RuntimeException(s"Not able to found account with id $id")
        }
      }).recover {
        case e: Exception => internalServerError(e.getMessage)
      }
  }

  def checkYourAnswers(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.mandatoryValues(UpdateBankAccountInterestKey, UpdateBankAccountNameKey) map { mandatory =>
        val interest = FormHelper.stripNumber(mandatory.head)
        val bankName = mandatory.last
        Ok(views.html.incomes.bbsi.update.bank_building_society_check_your_answers(BbsiUpdateInterestViewModel(id, interest, bankName)))
      }
  }

  def submitYourAnswers(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      for {
        mandatory <- journeyCacheService.mandatoryValues(UpdateBankAccountInterestKey, UpdateBankAccountNameKey)
        _ <- bbsiService.updateBankAccountInterest(Nino(user.getNino), id, AmountRequest(BigDecimal(FormHelper.stripNumber(mandatory.head))))
        _ <- journeyCacheService.flush()
      } yield Redirect(controllers.income.bbsi.routes.BbsiController.updateConfirmation())
  }

}
