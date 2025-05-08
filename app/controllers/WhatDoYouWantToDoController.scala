/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.EitherT
import cats.implicits._
import controllers.auth.{AuthJourney, AuthedUser}
import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, UpstreamErrorResponse}
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.forms.WhatDoYouWantToDoForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.admin.{CyPlusOneToggle, IncomeTaxHistoryToggle}
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel
import views.html.WhatDoYouWantToDoTileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatDoYouWantToDoController @Inject() (
  employmentService: EmploymentService,
  taxCodeChangeService: TaxCodeChangeService,
  taxAccountService: TaxAccountService,
  val auditConnector: AuditConnector,
  auditService: AuditService,
  jrsService: JrsService,
  authenticate: AuthJourney,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  whatDoYouWantToDoTileView: WhatDoYouWantToDoTileView,
  featureFlagService: FeatureFlagService,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Logging {

  def whatDoYouWantToDoPage(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino = request.taiUser.nino
    implicit val user: AuthedUser = request.taiUser
    val messages = request2Messages

    (for {
      hasTaxCodeChanged <- taxCodeChangeService.hasTaxCodeChanged(nino)
      showJrsLink <-
        EitherT[Future, UpstreamErrorResponse, Boolean](jrsService.checkIfJrsClaimsDataExist(nino).map(_.asRight))
      model <- EitherT[Future, UpstreamErrorResponse, WhatDoYouWantToDoViewModel](
                 whatToDoView(nino, hasTaxCodeChanged, showJrsLink).map(_.asRight)
               )
      incomeTaxHistoryToggle <-
        EitherT[Future, UpstreamErrorResponse, FeatureFlag](
          featureFlagService.get(IncomeTaxHistoryToggle).map(_.asRight)
        )
      cyPlusOneToggle <-
        EitherT[Future, UpstreamErrorResponse, FeatureFlag](featureFlagService.get(CyPlusOneToggle).map(_.asRight))
      _ <- auditNumberOfTaxCodesReturned(nino, showJrsLink)
    } yield Ok(
      whatDoYouWantToDoTileView(
        WhatDoYouWantToDoForm.createForm,
        model,
        applicationConfig,
        incomeTaxHistoryToggle.isEnabled,
        cyPlusOneToggle.isEnabled
      )
    )).leftMap {
      case error if error.statusCode == NOT_FOUND =>
        Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage())
      case error =>
        logger.error(error.message)
        InternalServerError(errorPagesHandler.error5xx(messages("tai.technical.error.message")))
    }.merge
      .recover { case error: HttpException =>
        logger.error(error.getMessage)
        InternalServerError(errorPagesHandler.error5xx(messages("tai.technical.error.message")))
      }
  }

  private def whatToDoView(nino: Nino, hasTaxCodeChanged: Boolean, showJrsLink: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[WhatDoYouWantToDoViewModel] = {
    val taxCodeChangeDate = if (hasTaxCodeChanged) {
      taxCodeChangeService.taxCodeChange(nino).map(_.mostRecentTaxCodeChangeDate.some)
    } else {
      Future.successful(None)
    }
    taxCodeChangeDate.flatMap { maybeMostRecentTaxCodeChangeDate =>
      lazy val successfulResponseModel = WhatDoYouWantToDoViewModel(
        cyPlusOneDataAvailable = true,
        showJrsLink = showJrsLink,
        maybeMostRecentTaxCodeChangeDate = maybeMostRecentTaxCodeChangeDate
      )

      lazy val unsuccessfulResponseModel =
        WhatDoYouWantToDoViewModel(
          cyPlusOneDataAvailable = false,
          showJrsLink = showJrsLink,
          maybeMostRecentTaxCodeChangeDate = maybeMostRecentTaxCodeChangeDate
        )
      featureFlagService.get(CyPlusOneToggle).flatMap { toggle =>
        if (toggle.isEnabled) {
          taxAccountService
            .taxAccountSummary(nino, TaxYear().next)
            .map(_ => successfulResponseModel)
            .fold(
              {
                case error if error.statusCode == NOT_FOUND =>
                  logger.error("No CY+1 tax account summary found, consider disabling the CY+1 toggles")
                  unsuccessfulResponseModel
                case _ => unsuccessfulResponseModel
              },
              _ => successfulResponseModel
            )
        } else {
          Future.successful(successfulResponseModel)
        }
      }
    }
  }

  private def auditNumberOfTaxCodesReturned(nino: Nino, isJrsTileShown: Boolean)(implicit
    request: Request[AnyContent]
  ): EitherT[Future, UpstreamErrorResponse, Future[AuditResult]] =
    taxAccountService.taxCodeIncomes(nino, TaxYear()).map { noOfTaxCodes =>
      employmentService.employments(nino, TaxYear()).flatMap { employments =>
        auditService
          .sendUserEntryAuditEvent(
            nino,
            request.headers.get("Referer").getOrElse("NA"),
            employments,
            noOfTaxCodes,
            isJrsTileShown
          )
      }
    }

  private[controllers] def previousYearEmployments(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    employmentService.employments(nino, TaxYear().prev) recover { case _ =>
      Nil
    }
}
