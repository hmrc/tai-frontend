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
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Failure
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.forms.WhatDoYouWantToDoForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.admin.{CyPlusOneToggle, IncomeTaxHistoryToggle}
import uk.gov.hmrc.tai.model.domain.{Employment, TaxAccountSummary, TaxCodeChange}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel
import views.html.WhatDoYouWantToDoTileView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatDoYouWantToDoController @Inject() (
  employmentService: EmploymentService,
  taxCodeChangeService: TaxCodeChangeService,
  taxAccountService: TaxAccountService,
  val auditConnector: AuditConnector,
  auditService: AuditService,
  authenticate: AuthJourney,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  whatDoYouWantToDoTileView: WhatDoYouWantToDoTileView,
  featureFlagService: FeatureFlagService,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Logging {

  private def isAnyCurrentOrPreviousEmployments(
    nino: Nino
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Boolean] = {
    val taxYears =
      // start from the current year which is the most probable year to be present
      (TaxYear().year to (TaxYear().year - applicationConfig.numberOfPreviousYearsToShowIncomeTaxHistory) by -1)
        .map(TaxYear(_))
        .toList

    taxYears.foldLeft(EitherT.rightT[Future, UpstreamErrorResponse](false)) { (acc, year) =>
      EitherT(acc.value.flatMap {
        case Right(true) =>
          // Short-circuit if we've already found an employment
          Future.successful(Right[UpstreamErrorResponse, Boolean](true))
        case _ =>
          employmentService
            .employmentsOnly(nino, year)
            .transform {
              case Right(_)                                     => Right(true)
              case Left(error) if error.statusCode == NOT_FOUND => Right(false)
              case Left(error)                                  => Left(error)
            }
            .value
      })
    }
  }

  private def nonFailingTaxCodeChanged(
    nino: Nino
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Option[TaxCodeChange]] =
    for {
      hasTaxCodeChanged <- taxCodeChangeService.hasTaxCodeChanged(nino).transform {
                             case Right(taxCodeChange) => Right(taxCodeChange)
                             case Left(_)              =>
                               // don't fail the page when the tax code change banner is failing
                               Right(false)
                           }
      taxCodeChange <- if (hasTaxCodeChanged) {
                         taxCodeChangeService.taxCodeChange(nino).map(Some[TaxCodeChange](_))
                       } else {
                         EitherT.rightT[Future, UpstreamErrorResponse](None)
                       }
    } yield taxCodeChange

  private def nonFailingCyPlusOneTaxAccount(
    nino: Nino
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Option[TaxAccountSummary]] =
    for {
      cyPlusOneToggle <- featureFlagService.getAsEitherT[UpstreamErrorResponse](CyPlusOneToggle)
      cyPlusOneTaxAccount <- {
        if (cyPlusOneToggle.isEnabled) {
          taxAccountService.taxAccountSummary(nino, TaxYear().next).transform {
            case Right(taxAccount) =>
              Right[UpstreamErrorResponse, Option[TaxAccountSummary]](Some(taxAccount))
            case Left(error) if error.statusCode == NOT_FOUND =>
              logger.error(
                "No CY+1 tax account summary found, consider disabling the CY+1 toggles"
              )
              Right(None)
            case Left(_) =>
              // don't fail the page when we get an error for CY+1
              Right(none)
          }
        } else {
          EitherT.rightT[Future, UpstreamErrorResponse](None: Option[TaxAccountSummary])
        }
      }
    } yield cyPlusOneTaxAccount

  def whatDoYouWantToDoPage(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino = request.taiUser.nino
    implicit val user: AuthedUser = request.taiUser
    val messages = request2Messages

    (for {
      anyCurrentOrPastEmployment <- isAnyCurrentOrPreviousEmployments(nino)
      taxCodeChange              <- nonFailingTaxCodeChanged(nino)
      cyPlusOneTaxAccount        <- nonFailingCyPlusOneTaxAccount(nino)
      incomeTaxHistoryToggle     <- featureFlagService.getAsEitherT[UpstreamErrorResponse](IncomeTaxHistoryToggle)
      _                          <- auditNumberOfTaxCodesReturned(nino)
    } yield
      if (anyCurrentOrPastEmployment) {
        Ok(
          whatDoYouWantToDoTileView(
            WhatDoYouWantToDoForm.createForm,
            whatToDoView(taxCodeChange, cyPlusOneTaxAccount),
            applicationConfig,
            incomeTaxHistoryToggle.isEnabled,
            cyPlusOneTaxAccount.isDefined
          )
        )
      } else Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
      .leftMap { error =>
        logger.error(error.message)
        InternalServerError(errorPagesHandler.error5xx(messages("tai.technical.error.message")))
      }
      .merge
      .recover { case error: HttpException =>
        logger.error(error.getMessage)
        InternalServerError(errorPagesHandler.error5xx(messages("tai.technical.error.message")))
      }
  }

  private def whatToDoView(
    taxCodeChanged: Option[TaxCodeChange],
    maybeCyPlusOneTaxAccount: Option[TaxAccountSummary]
  ): WhatDoYouWantToDoViewModel = {
    val maybeMostRecentTaxCodeChangeDate: Option[LocalDate] = taxCodeChanged.map(_.mostRecentTaxCodeChangeDate)
    WhatDoYouWantToDoViewModel(
      cyPlusOneDataAvailable = maybeCyPlusOneTaxAccount.isDefined,
      maybeMostRecentTaxCodeChangeDate = maybeMostRecentTaxCodeChangeDate
    )
  }

  private def auditNumberOfTaxCodesReturned(nino: Nino)(implicit
    request: Request[AnyContent]
  ): EitherT[Future, UpstreamErrorResponse, Future[AuditResult]] =
    taxAccountService.newTaxCodeIncomes(nino, TaxYear()).transform {
      case Left(error) => Right(Future.successful(Failure(error.message)))
      case Right(noOfTaxCodes) =>
        Right(employmentService.employments(nino, TaxYear()).flatMap { employments =>
          auditService
            .sendUserEntryAuditEvent(
              nino,
              request.headers.get("Referer").getOrElse("NA"),
              employments,
              noOfTaxCodes
            )
        })
    }

  private[controllers] def previousYearEmployments(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    employmentService.employments(nino, TaxYear().prev) recover { case _ =>
      Nil
    }
}
