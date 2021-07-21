/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.auth.{AuthAction, AuthedUser, AuthenticatedRequest}
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{Employment, TemporarilyUnavailable}
import uk.gov.hmrc.tai.service.{EmploymentService, TaxCodeChangeService}
import uk.gov.hmrc.tai.viewModels.HistoricPayAsYouEarnViewModel
import views.html.paye.{HistoricPayAsYouEarnView, RtiDisabledHistoricPayAsYouEarnView}

import scala.concurrent.{ExecutionContext, Future}

class PayeControllerHistoric @Inject()(
  val config: ApplicationConfig,
  taxCodeChangeService: TaxCodeChangeService,
  employmentService: EmploymentService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  RtiDisabledHistoricPayAsYouEarnView: RtiDisabledHistoricPayAsYouEarnView,
  historicPayAsYouEarnView: HistoricPayAsYouEarnView,
  implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def lastYearPaye(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    Future.successful(Redirect(controllers.routes.PayeControllerHistoric.payePage(TaxYear().prev)))
  }

  def payePage(year: TaxYear): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    getHistoricPayePage(year)
  }

  private def getHistoricPayePage(taxYear: TaxYear)(
    implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val nino = request.taiUser.nino

    if (taxYear >= TaxYear()) {
      Future.successful(Redirect(routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage()))
    } else {
      val employmentsFuture = employmentService.employments(nino, taxYear)
      val hasTaxCodeRecordsFuture = taxCodeChangeService.hasTaxCodeRecordsInYearPerEmployment(nino, taxYear)

      for {
        employments                          <- employmentsFuture
        hasTaxCodeRecordsInYearPerEmployment <- hasTaxCodeRecordsFuture
      } yield {
        implicit val user: AuthedUser = request.taiUser
        if (isRtiUnavailable(employments)) {
          Ok(
            RtiDisabledHistoricPayAsYouEarnView(
              HistoricPayAsYouEarnViewModel(taxYear, employments, hasTaxCodeRecordsInYearPerEmployment),
              config))
        } else {
          Ok(
            historicPayAsYouEarnView(
              HistoricPayAsYouEarnViewModel(taxYear, employments, hasTaxCodeRecordsInYearPerEmployment),
              config))
        }
      }
    }
  } recoverWith hodStatusRedirect

  private def isRtiUnavailable(employments: Seq[Employment]): Boolean =
    employments.headOption.exists(_.annualAccounts.headOption.exists(_.realTimeStatus == TemporarilyUnavailable))

  private def hodStatusRedirect(
    implicit request: AuthenticatedRequest[AnyContent]): PartialFunction[Throwable, Future[Result]] = {

    implicit val rl: errorPagesHandler.RecoveryLocation = classOf[WhatDoYouWantToDoController]
    val nino = request.taiUser.nino.toString()

    errorPagesHandler.npsEmploymentAbsentResult(nino) orElse
      errorPagesHandler.rtiEmploymentAbsentResult(nino) orElse
      errorPagesHandler.hodBadRequestResult(nino) orElse
      errorPagesHandler.hodInternalErrorResult(nino) orElse
      errorPagesHandler.hodAnyErrorResult(nino)
  }
}
