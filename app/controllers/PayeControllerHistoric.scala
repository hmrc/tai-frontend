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

package controllers

import javax.inject.Inject
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthenticatedRequest}
import play.api.Play
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.{EmploymentService, TaxCodeChangeService}
import uk.gov.hmrc.tai.viewModels.HistoricPayAsYouEarnViewModel

import scala.concurrent.Future

class PayeControllerHistoric @Inject()(val config: ApplicationConfig,
                                       taxCodeChangeService: TaxCodeChangeService,
                                       employmentService: EmploymentService,
                                       authenticate: AuthAction,
                                       validatePerson: ValidatePerson,
                                       override implicit val partialRetriever: FormPartialRetriever,
                                       override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController {

  val numberOfPreviousYearsToShow: Int = Play.configuration.getInt("tai.numberOfPreviousYearsToShow").getOrElse(3)

  def lastYearPaye(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    Future.successful(Redirect(controllers.routes.PayeControllerHistoric.payePage(TaxYear().prev)))
  }

  def payePage(year: TaxYear): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      getHistoricPayePage(year)
  }

  private def getHistoricPayePage(taxYear: TaxYear)(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val nino = request.taiUser.nino

    if (taxYear >= TaxYear()) {
      Future.successful(Redirect(routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage()))
    } else {
      val employmentsFuture = employmentService.employments(nino, taxYear)
      val hasTaxCodeRecordsFuture = taxCodeChangeService.hasTaxCodeRecordsInYearPerEmployment(nino, taxYear)

      for {
        employments <- employmentsFuture
        hasTaxCodeRecordsInYearPerEmployment <- hasTaxCodeRecordsFuture
      } yield {
        implicit val user = request.taiUser
        Ok(views.html.paye.historicPayAsYouEarn(HistoricPayAsYouEarnViewModel(
          taxYear, employments, hasTaxCodeRecordsInYearPerEmployment), numberOfPreviousYearsToShow))
      }
    }
  } recoverWith hodStatusRedirect


  def hodStatusRedirect(implicit request: AuthenticatedRequest[AnyContent]): PartialFunction[Throwable, Future[Result]] = {

    implicit val rl: RecoveryLocation = classOf[WhatDoYouWantToDoController]
    val nino = request.taiUser.getNino

    npsEmploymentAbsentResult(nino) orElse
      rtiEmploymentAbsentResult(nino) orElse
      hodBadRequestResult(nino) orElse
      hodInternalErrorResult(nino) orElse
      hodAnyErrorResult(nino)
  }
}
