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

import controllers.actions.{DataRetrievalActionProvider, ValidatePerson}
import controllers.auth.{AuthAction, AuthedUser, OptionalDataRequest}
import javax.inject.Inject
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms.WhatDoYouWantToDoForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel

import scala.concurrent.{ExecutionContext, Future}

class WhatDoYouWantToDoController @Inject()(
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
  dataRetrievalAction: DataRetrievalActionProvider,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  implicit val recoveryLocation: RecoveryLocation = classOf[WhatDoYouWantToDoController]

  def whatDoYouWantToDoPage(): Action[AnyContent] =
    (authenticate andThen validatePerson andThen dataRetrievalAction.getData()).async { implicit request =>
      {
        implicit val user = request.request.taiUser
        val nino = user.nino
        val ninoString = nino.toString()

        val possibleRedirectFuture =
          for {
            taxAccountSummary   <- taxAccountService.taxAccountSummary(nino, TaxYear())
            _                   <- employmentService.employments(nino, TaxYear())
            prevYearEmployments <- previousYearEmployments(nino)
          } yield {

            val npsFailureHandlingPf: PartialFunction[TaiResponse, Option[Result]] =
              npsTaxAccountAbsentResult_withEmployCheck(prevYearEmployments, ninoString) orElse
                npsTaxAccountCYAbsentResult_withEmployCheck(prevYearEmployments, ninoString) orElse
                npsNoEmploymentForCYResult_withEmployCheck(prevYearEmployments, ninoString) orElse
                npsNoEmploymentResult(ninoString) orElse
                npsTaxAccountDeceasedResult(ninoString) orElse { case _ => None }

            npsFailureHandlingPf(taxAccountSummary)
          }

        possibleRedirectFuture.flatMap(
          _.map(Future.successful).getOrElse(allowWhatDoYouWantToDo)
        )

      } recoverWith {
        val nino = request.request.taiUser.nino

        (hodBadRequestResult(nino.toString()) orElse hodInternalErrorResult(nino.toString()))
      }
    }

  private def allowWhatDoYouWantToDo(
    implicit request: OptionalDataRequest[AnyContent],
    user: AuthedUser): Future[Result] = {
    val nino = user.nino

    auditNumberOfTaxCodesReturned(nino)

    if (applicationConfig.cyPlusOneEnabled) {
      val hasTaxCodeChanged = taxCodeChangeService.hasTaxCodeChanged(nino)
      val cy1TaxAccountSummary = taxAccountService.taxAccountSummary(nino, TaxYear().next)

      for {
        taxCodeChanged    <- hasTaxCodeChanged
        taxAccountSummary <- cy1TaxAccountSummary
        showJrsTile       <- jrsService.checkIfJrsClaimsDataExist(nino)

      } yield {
        taxAccountSummary match {
          case TaiSuccessResponseWithPayload(_) => {
            val model = WhatDoYouWantToDoViewModel(
              applicationConfig.cyPlusOneEnabled,
              taxCodeChanged.changed,
              showJrsTile,
              taxCodeChanged.mismatch)

            Logger.debug(s"wdywtdViewModelCYEnabledAndGood $model")

            Ok(views.html.whatDoYouWantToDoTileView(WhatDoYouWantToDoForm.createForm, model))
          }
          case response: TaiResponse => {
            if (response.isInstanceOf[TaiNotFoundResponse])
              Logger.error("No CY+1 tax account summary found, consider disabling the CY+1 toggles")

            val model = WhatDoYouWantToDoViewModel(isCyPlusOneEnabled = false, showJrsTile = showJrsTile)
            Logger.debug(s"wdywtdViewModelCYEnabledButBad $model")
            Ok(views.html.whatDoYouWantToDoTileView(WhatDoYouWantToDoForm.createForm, model))
          }
        }
      }
    } else {
      for {
        hasTaxCodeChanged <- taxCodeChangeService.hasTaxCodeChanged(nino)
        showJrsTile       <- jrsService.checkIfJrsClaimsDataExist(nino)
      } yield {
        val model =
          WhatDoYouWantToDoViewModel(
            applicationConfig.cyPlusOneEnabled,
            hasTaxCodeChanged.changed,
            showJrsTile,
            hasTaxCodeChanged.mismatch)

        Logger.debug(s"wdywtdViewModelCYDisabled $model")

        Ok(views.html.whatDoYouWantToDoTileView(WhatDoYouWantToDoForm.createForm, model))
      }
    }
  }

  private def auditNumberOfTaxCodesReturned(nino: Nino)(implicit request: Request[AnyContent]) = {

    val currentTaxYearEmployments: Future[Seq[Employment]] = employmentService.employments(nino, TaxYear())
    val currentTaxYearTaxCodes: Future[TaiResponse] = taxAccountService.taxCodeIncomes(nino, TaxYear())

    (for {
      employments    <- currentTaxYearEmployments
      taxCodes       <- currentTaxYearTaxCodes
      isJrsTileShown <- jrsService.checkIfJrsClaimsDataExist(nino)
    } yield {
      val noOfTaxCodes: Seq[TaxCodeIncome] = taxCodes match {
        case TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]) => taxCodeIncomes
        case _                                                                 => Seq.empty[TaxCodeIncome]
      }
      auditService.sendUserEntryAuditEvent(
        nino,
        request.headers.get("Referer").getOrElse("NA"),
        employments,
        noOfTaxCodes,
        isJrsTileShown)
    }).recover {
      auditError(nino)
    }
  }

  private def auditError(nino: Nino)(implicit request: Request[AnyContent]): PartialFunction[Throwable, Unit] = {
    case e =>
      Logger.warn(
        s"<Send audit event failed to get either taxCodeIncomes or employments for nino $nino  with exception: ${e.getClass()}",
        e)
  }

  private[controllers] def previousYearEmployments(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    employmentService.employments(nino, TaxYear().prev) recover {
      case _ => Nil
    }
}
