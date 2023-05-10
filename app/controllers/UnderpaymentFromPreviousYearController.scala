/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.Referral
import uk.gov.hmrc.tai.viewModels.PreviousYearUnderpaymentViewModel
import views.html.PreviousYearUnderpaymentView

import scala.concurrent.ExecutionContext

class UnderpaymentFromPreviousYearController @Inject() (
  codingComponentService: CodingComponentService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  previousYearUnderpayment: PreviousYearUnderpaymentView,
  implicit val templateRenderer: TemplateRenderer
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Referral {

  def underpaymentExplanation: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val nino = user.nino
    val year = TaxYear()
    val codingComponentsFuture = codingComponentService.taxFreeAmountComponents(nino, year)

    for {
      codingComponents <- codingComponentsFuture
    } yield {
      val model = PreviousYearUnderpaymentViewModel(codingComponents, referer, resourceName)
      Ok(previousYearUnderpayment(model))
    }
  }
}
