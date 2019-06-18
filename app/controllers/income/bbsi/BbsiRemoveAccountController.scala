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


import javax.inject.Inject
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.service.BbsiService


class BbsiRemoveAccountController @Inject()(bbsiService: BbsiService,
                                            authenticate: AuthAction,
                                            validatePerson: ValidatePerson,
                                            override implicit val partialRetriever: FormPartialRetriever,
                                            override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController {

  def checkYourAnswers(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      bbsiService.bankAccount(Nino(user.getNino), id) map {
        case Some(bankAccount) =>
          Ok(views.html.incomes.bbsi.remove.bank_building_society_check_your_answers(id, bankAccount.bankName.getOrElse("")))
        case None => NotFound
      }
  }

  def submitYourAnswers(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      bbsiService.removeBankAccount(Nino(user.getNino), id) map { _ =>
        Redirect(controllers.income.bbsi.routes.BbsiController.removeConfirmation())
      }
  }
}
