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

package controllers.employments

import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.UpdateEmploymentDetailsForm
import uk.gov.hmrc.tai.model.domain.IncorrectIncome
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FutureOps.FutureEitherStringOps
import uk.gov.hmrc.tai.util.Referral
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, UpdateEmploymentCheckYourAnswersViewModel}
import views.html.CanWeContactByPhoneView
import views.html.employments.ConfirmationView
import views.html.employments.update.{UpdateEmploymentCheckYourAnswersView, WhatDoYouWantToTellUsView}

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UpdateEmploymentController @Inject() (
  employmentService: EmploymentService,
  val auditConnector: AuditConnector,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  whatDoYouWantToTellUs: WhatDoYouWantToTellUsView,
  canWeContactByPhone: CanWeContactByPhoneView,
  updateEmploymentCheckYourAnswers: UpdateEmploymentCheckYourAnswersView,
  confirmationView: ConfirmationView,
  @Named("Update Employment") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Referral with EmptyCacheRedirect {

  def cancel(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.flush() map { _ =>
      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
    }
  }

  def telephoneNumberViewModel(id: Int)(implicit messages: Messages): CanWeContactByPhoneViewModel =
    CanWeContactByPhoneViewModel(
      messages("tai.updateEmployment.whatDoYouWantToTellUs.preHeading"),
      messages("tai.canWeContactByPhone.title"),
      controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(id).url,
      controllers.employments.routes.UpdateEmploymentController.submitTelephoneNumber().url,
      controllers.employments.routes.UpdateEmploymentController.cancel(id).url
    )

  def updateEmploymentDetails(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      (for {
        userSuppliedDetails <- journeyCacheService.currentValue(UpdateEmploymentConstants.EmploymentDetailsKey)
        employment          <- employmentService.employment(user.nino, empId)
        futureResult <- employment match {
                          case Some(emp) =>
                            val cache = Map(
                              UpdateEmploymentConstants.EmploymentIdKey -> empId.toString,
                              UpdateEmploymentConstants.NameKey         -> emp.name
                            )
                            journeyCacheService
                              .cache(cache)
                              .map(_ =>
                                Ok(
                                  whatDoYouWantToTellUs(
                                    EmploymentViewModel(emp.name, empId),
                                    UpdateEmploymentDetailsForm.form.fill(userSuppliedDetails.getOrElse(""))
                                  )
                                )
                              )
                          case _ =>
                            Future.successful(
                              errorPagesHandler.internalServerError("Error during employment details retrieval")
                            )
                        }
      } yield futureResult).recover { case NonFatal(exception) =>
        errorPagesHandler.internalServerError(exception.getMessage)
      }

  }

  def submitUpdateEmploymentDetails(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      UpdateEmploymentDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            journeyCacheService.currentCache map { currentCache =>
              implicit val user: AuthedUser = request.taiUser
              BadRequest(
                whatDoYouWantToTellUs(
                  EmploymentViewModel(currentCache(UpdateEmploymentConstants.NameKey), empId),
                  formWithErrors
                )
              )
            },
          employmentDetails =>
            journeyCacheService
              .cache(Map(UpdateEmploymentConstants.EmploymentDetailsKey -> employmentDetails))
              .map(_ => Redirect(controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber()))
        )
  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    for {
      employmentId <- journeyCacheService.mandatoryJourneyValueAsInt(EndEmploymentConstants.EmploymentIdKey)
      telephoneCache <-
        journeyCacheService
          .optionalValues(UpdateEmploymentConstants.TelephoneQuestionKey, UpdateEmploymentConstants.TelephoneNumberKey)
    } yield {
      implicit val user: AuthedUser = request.taiUser
      employmentId match {
        case Right(empId) =>
          Ok(
            canWeContactByPhone(
              Some(user),
              telephoneNumberViewModel(empId),
              YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache.head, telephoneCache(1)))
            )
          )
        case Left(_) => Redirect(taxAccountSummaryRedirect)
      }
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.enter.number"),
        Some(TelephoneNumberConstraint.telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors =>
          journeyCacheService.currentCache map { currentCache =>
            implicit val user: AuthedUser = request.taiUser
            BadRequest(
              canWeContactByPhone(
                Some(user),
                telephoneNumberViewModel(currentCache(UpdateEmploymentConstants.EmploymentIdKey).toInt),
                formWithErrors
              )
            )
          },
        form => {
          val mandatoryData = Map(
            UpdateEmploymentConstants.TelephoneQuestionKey -> Messages(
              s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}"
            )
          )
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == FormValuesConstants.YesValue =>
              mandatoryData ++ Map(UpdateEmploymentConstants.TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(UpdateEmploymentConstants.TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.employments.routes.UpdateEmploymentController.updateEmploymentCheckYourAnswers())
          }
        }
      )
  }

  def updateEmploymentCheckYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      journeyCacheService
        .collectedJourneyValues(
          Seq(
            UpdateEmploymentConstants.EmploymentIdKey,
            UpdateEmploymentConstants.NameKey,
            UpdateEmploymentConstants.EmploymentDetailsKey,
            UpdateEmploymentConstants.TelephoneQuestionKey
          ),
          Seq(UpdateEmploymentConstants.TelephoneNumberKey)
        )
        .map {
          case Right((mandatoryJourneyValues, optionalSeq)) =>
            Ok(
              updateEmploymentCheckYourAnswers(
                UpdateEmploymentCheckYourAnswersViewModel(
                  mandatoryJourneyValues.head.toInt,
                  mandatoryJourneyValues(1),
                  mandatoryJourneyValues(2),
                  mandatoryJourneyValues(3),
                  optionalSeq.head
                )
              )
            )
          case Left(_) => Redirect(taxAccountSummaryRedirect)
        }
  }

  def submitYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    for {
      (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService
                                                 .collectedJourneyValues(
                                                   Seq(
                                                     UpdateEmploymentConstants.EmploymentIdKey,
                                                     UpdateEmploymentConstants.EmploymentDetailsKey,
                                                     UpdateEmploymentConstants.TelephoneQuestionKey
                                                   ),
                                                   Seq(UpdateEmploymentConstants.TelephoneNumberKey)
                                                 )
                                                 .getOrFail
      model = IncorrectIncome(mandatoryCacheSeq(1), mandatoryCacheSeq(2), optionalCacheSeq.head)
      _ <- employmentService.incorrectEmployment(user.nino, mandatoryCacheSeq.head.toInt, model)
      _ <-
        successfulJourneyCacheService
          .cache(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-${mandatoryCacheSeq.head}", true.toString)
      _ <- journeyCacheService.flush()
    } yield Redirect(controllers.employments.routes.UpdateEmploymentController.confirmation())
  }

  def confirmation: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    Future.successful(Ok(confirmationView()))
  }
}
