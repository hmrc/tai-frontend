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

import cats.data.OptionT
import cats.implicits._
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser, AuthenticatedRequest}
import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.forms.WhatDoYouWantToDoForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{Employment, HasTaxCodeChanged}
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
  jrsService: JrsService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  whatDoYouWantToDoTileView: WhatDoYouWantToDoTileView,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Logging {

  private implicit val recoveryLocation: errorPagesHandler.RecoveryLocation = classOf[WhatDoYouWantToDoController]

  def whatDoYouWantToDoPage(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    {
      implicit val user: AuthedUser = request.taiUser
      val nino = request.taiUser.nino
      val ninoString = request.taiUser.nino.toString()

      lazy val employmentsFuture = employmentService.employments(nino, TaxYear())
      lazy val redirectFuture =
        OptionT(taxAccountService.taxAccountSummary(nino, TaxYear()).map(_ => none[Result]).recoverWith { case ex =>
          previousYearEmployments(nino).map { prevYearEmployments =>
            val handler: PartialFunction[Throwable, Option[Result]] =
              errorPagesHandler.npsTaxAccountAbsentResult_withEmployCheck(prevYearEmployments, ninoString) orElse
                errorPagesHandler
                  .npsTaxAccountCYAbsentResult_withEmployCheck(prevYearEmployments, ninoString) orElse
                errorPagesHandler.npsNoEmploymentForCYResult_withEmployCheck(prevYearEmployments, ninoString) orElse
                errorPagesHandler.npsNoEmploymentResult(ninoString) orElse
                errorPagesHandler.npsTaxAccountDeceasedResult(ninoString) orElse { case _ => none }
            handler(ex)
          }
        }).getOrElseF(allowWhatDoYouWantToDo)

      for {
        _        <- employmentsFuture
        redirect <- redirectFuture
      } yield redirect
    } recoverWith {
      val nino = request.taiUser.nino

      errorPagesHandler.hodBadRequestResult(nino.toString()) orElse errorPagesHandler.hodInternalErrorResult(
        nino.toString()
      )
    }
  }

  private[controllers] def retrieveTaxCodeChange(hasTaxCodeChanged: HasTaxCodeChanged): Boolean = {
    (hasTaxCodeChanged.changed, hasTaxCodeChanged.mismatch) match {
      case (true, Some(mismatch)) if mismatch.confirmedTaxCodes.size > 1 => true
      case _                                                             => false
    }
  }

  private def mostRecentTaxCodeChangeDate(nino: Nino, hasTaxCodeChanged: HasTaxCodeChanged)(implicit
    request: Request[AnyContent]
  ): Future[Option[LocalDate]] =
    if (retrieveTaxCodeChange(hasTaxCodeChanged)) {
      taxCodeChangeService.taxCodeChange(nino).map(_.mostRecentTaxCodeChangeDate.some)
    } else {
      Future.successful(none)
    }

  private def whatToDoView(nino: Nino, hasTaxCodeChanged: HasTaxCodeChanged, showJrsLink: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[WhatDoYouWantToDoViewModel] =
    mostRecentTaxCodeChangeDate(nino, hasTaxCodeChanged).flatMap { maybeMostRecentTaxCodeChangeDate =>
      lazy val successfulResponseModel = WhatDoYouWantToDoViewModel(
        isCyPlusOneEnabled = applicationConfig.cyPlusOneEnabled,
        showJrsLink = showJrsLink,
        maybeMostRecentTaxCodeChangeDate = maybeMostRecentTaxCodeChangeDate
      )

      lazy val unsuccessfulResponseModel =
        WhatDoYouWantToDoViewModel(
          isCyPlusOneEnabled = false,
          showJrsLink = showJrsLink,
          maybeMostRecentTaxCodeChangeDate = maybeMostRecentTaxCodeChangeDate
        )

      if (applicationConfig.cyPlusOneEnabled) {
        taxAccountService.taxAccountSummary(nino, TaxYear().next).map(_ => successfulResponseModel) recover {
          case _: NotFoundException =>
            logger.error("No CY+1 tax account summary found, consider disabling the CY+1 toggles")
            unsuccessfulResponseModel
          case _ =>
            unsuccessfulResponseModel
        }
      } else {
        Future.successful(successfulResponseModel)
      }
    }

  private def allowWhatDoYouWantToDo(implicit request: AuthenticatedRequest[AnyContent], user: AuthedUser) = {
    val nino = user.nino

    taxCodeChangeService.hasTaxCodeChanged(nino) flatMap {
      case Right(taxCodeChanged: HasTaxCodeChanged) =>
        for {
          showJrsLink <- jrsService.checkIfJrsClaimsDataExist(nino)
          (model, _) <-
            (whatToDoView(nino, taxCodeChanged, showJrsLink), auditNumberOfTaxCodesReturned(nino, showJrsLink)).tupled
        } yield Ok(whatDoYouWantToDoTileView(WhatDoYouWantToDoForm.createForm, model, applicationConfig))
      case Left(taxCodeError) =>
        Future.successful(
          BadRequest(errorPagesHandler.error4xxPageWithLink(taxCodeError.errorMessage.getOrElse("Tax Code not Found")))
        )
    }
  }

  private def auditNumberOfTaxCodesReturned(nino: Nino, isJrsTileShown: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[AuditResult] = {

    val noOfTaxCodesF = taxAccountService.taxCodeIncomes(nino, TaxYear()).map { currentTaxYearTaxCodes =>
      currentTaxYearTaxCodes.getOrElse(Seq.empty[TaxCodeIncome])
    }

    noOfTaxCodesF.flatMap { noOfTaxCodes =>
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
  }

  private[controllers] def previousYearEmployments(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    employmentService.employments(nino, TaxYear().prev) recover { case _ =>
      Nil
    }
}
