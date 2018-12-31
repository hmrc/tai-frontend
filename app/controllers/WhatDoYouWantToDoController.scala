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

import com.google.inject.Inject
import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms.WhatDoYouWantToDoForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future

class WhatDoYouWantToDoController @Inject()(val personService: PersonService,
                                            val employmentService: EmploymentService,
                                            taxCodeChangeService: TaxCodeChangeService,
                                            val taxAccountService: TaxAccountService,
                                            trackingService: TrackingService,
                                            auditService: AuditService,
                                            val auditConnector: AuditConnector,
                                            val authConnector: AuthConnector,
                                            val delegationConnector: DelegationConnector,
                                            override implicit val partialRetriever: FormPartialRetriever,
                                            override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with FeatureTogglesConfig {

  implicit val recoveryLocation: RecoveryLocation = classOf[WhatDoYouWantToDoController]

  def whatDoYouWantToDoPage(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val possibleRedirectFuture =
              for {
                taxAccountSummary <- taxAccountService.taxAccountSummary(Nino(user.getNino), TaxYear())
                _ <- employmentService.employments(Nino(user.getNino), TaxYear())
                prevYearEmployments <- previousYearEmployments(Nino(user.getNino))
              } yield {
                val npsFailureHandlingPf: PartialFunction[TaiResponse, Option[Result]] =
                  npsTaxAccountAbsentResult_withEmployCheck(prevYearEmployments) orElse
                    npsTaxAccountCYAbsentResult_withEmployCheck(prevYearEmployments) orElse
                    npsNoEmploymentForCYResult_withEmployCheck(prevYearEmployments) orElse
                    npsNoEmploymentResult orElse
                    npsTaxAccountDeceasedResult orElse { case _ => None }

                npsFailureHandlingPf(taxAccountSummary)
              }

            possibleRedirectFuture.flatMap(
              _.map(Future.successful).getOrElse(allowWhatDoYouWantToDo)
            )

          } recoverWith (hodBadRequestResult orElse hodInternalErrorResult)
  }

  private def allowWhatDoYouWantToDo(implicit request: Request[AnyContent], user: TaiUser): Future[Result] = {

    val nino = Nino(user.getNino)

    auditNumberOfTaxCodesReturned(nino)

    trackingService.isAnyIFormInProgress(nino.nino) flatMap { trackingResponse =>

      if (cyPlusOneEnabled) {

        val hasTaxCodeChanged = taxCodeChangeService.hasTaxCodeChanged(nino)
        val cy1TaxAccountSummary = taxAccountService.taxAccountSummary(nino, TaxYear().next)

        for {
          taxCodeChanged <- hasTaxCodeChanged
          taxAccountSummary <- cy1TaxAccountSummary
        } yield {
          taxAccountSummary match {
            case TaiSuccessResponseWithPayload(_) =>
              Ok(views.html.whatDoYouWantToDoTileView(WhatDoYouWantToDoForm.createForm, WhatDoYouWantToDoViewModel(
                trackingResponse, cyPlusOneEnabled, taxCodeChanged.changed, taxCodeChanged.mismatch)))
            case _ =>
              Ok(views.html.whatDoYouWantToDoTileView(WhatDoYouWantToDoForm.createForm, WhatDoYouWantToDoViewModel(
                trackingResponse, isCyPlusOneEnabled = false)))
          }
        }
      }
      else {
        taxCodeChangeService.hasTaxCodeChanged(nino).map(hasTaxCodeChanged =>
          Ok(views.html.whatDoYouWantToDoTileView(WhatDoYouWantToDoForm.createForm, WhatDoYouWantToDoViewModel(
            trackingResponse, cyPlusOneEnabled, hasTaxCodeChanged.changed, hasTaxCodeChanged.mismatch)))
        )
      }
    }
  }

  private def auditNumberOfTaxCodesReturned(nino: Nino)
                                           (implicit request: Request[AnyContent],
                                            user: TaiUser) = {

    val currentTaxYearEmployments: Future[Seq[Employment]] = employmentService.employments(nino, TaxYear())
    val currentTaxYearTaxCodes: Future[TaiResponse] = taxAccountService.taxCodeIncomes(nino, TaxYear())

    (for {
      employments <- currentTaxYearEmployments
      taxCodes <- currentTaxYearTaxCodes
    } yield {
      val noOfTaxCodes: Seq[TaxCodeIncome] = taxCodes match {
        case TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]) => taxCodeIncomes
        case _ => Seq.empty[TaxCodeIncome]
      }
      auditService.sendUserEntryAuditEvent(nino, request.headers.get("Referer").getOrElse("NA"), employments, noOfTaxCodes)
    }).recover {
      auditError
    }
  }


  private def auditError(implicit request: Request[AnyContent], user: TaiUser): PartialFunction[Throwable, Unit] = {
    case e =>
      Logger.warn(s"<Send audit event failed to get either taxCodeIncomes or employments for nino ${user.getNino}  with exception: ${e.getClass()}", e)
  }

  private[controllers] def previousYearEmployments(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[Employment]] = {
    employmentService.employments(nino, TaxYear(TaxYearResolver.currentTaxYear - 1)) recover {
      case _ => Nil
    }
  }
}
