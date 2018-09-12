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

package controllers

import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import play.api.Play
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.viewModels.HistoricPayAsYouEarnViewModel

import scala.concurrent.Future

trait PayeControllerHistoric extends TaiBaseController
with DelegationAwareActions
with WithAuthorisedForTaiLite
with Auditable {

  def personService: PersonService
  def employmentService: EmploymentService
  def numberOfPreviousYearsToShow: Int

  def lastYearPaye(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          Future.successful(Redirect(controllers.routes.PayeControllerHistoric.payePage(TaxYear().prev)))
  }

  def payePage(year: TaxYear): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          getHistoricPayePage(year, Nino(user.getNino))
  }

  private[controllers] def getHistoricPayePage(taxYear: TaxYear,
                                               nino: Nino)
                                              (implicit request: Request[AnyContent],
                                               user: TaiUser, person: Person): Future[Result] = {
    if (taxYear >= TaxYear()) {
      Future.successful(Redirect(routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage()))
    } else {
      for {
        employments <- employmentService.employments(nino, taxYear)
      } yield {
        checkedAgainstPersonDetails(
          person,
          Ok(views.html.paye.historicPayAsYouEarn(HistoricPayAsYouEarnViewModel(taxYear, employments), numberOfPreviousYearsToShow))
        )
      }
    }
  } recoverWith hodStatusRedirect

  private def checkedAgainstPersonDetails(person: Person, proceed: Result)(implicit request: Request[AnyContent], user: TaiUser): Result = {
    if (person.isDeceased) {
      Redirect(routes.DeceasedController.deceased())
    } else if (person.hasCorruptData) {
      Redirect(routes.ServiceController.gateKeeper())
    } else {
      proceed
    }
  }

  def hodStatusRedirect(implicit request: Request[AnyContent], user: TaiUser, person: Person): PartialFunction[Throwable, Future[Result]] = {

    implicit val rl:RecoveryLocation = classOf[WhatDoYouWantToDoController]

    npsEmploymentAbsentResult orElse
    rtiEmploymentAbsentResult  orElse
    hodBadRequestResult orElse
    hodInternalErrorResult orElse
    hodAnyErrorResult
  }
}
// $COVERAGE-OFF$
object PayeControllerHistoric extends PayeControllerHistoric with AuthenticationConnectors {
  override val employmentService = EmploymentService
  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
  override val numberOfPreviousYearsToShow: Int = Play.configuration.getInt("tai.numberOfPreviousYearsToShow").getOrElse(3)
  override val personService: PersonService = PersonService
}
// $COVERAGE-ON$