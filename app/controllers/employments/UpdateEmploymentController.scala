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

package controllers.employments

import javax.inject.{Inject, Named}
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.employments.UpdateEmploymentDetailsForm
import uk.gov.hmrc.tai.model.domain.IncorrectIncome
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, UpdateEmploymentCheckYourAnswersViewModel}

import scala.Function.tupled
import scala.concurrent.Future
import scala.util.control.NonFatal

class UpdateEmploymentController @Inject()(employmentService: EmploymentService,
                                           val auditConnector: AuditConnector,
                                           authenticate: AuthAction,
                                           validatePerson: ValidatePerson,
                                           @Named("Update Employment") journeyCacheService: JourneyCacheService,
                                           @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
                                           override implicit val partialRetriever: FormPartialRetriever,
                                           override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with JourneyCacheConstants
  with AuditConstants
  with FormValuesConstants {

  def cancel(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.flush() map { _ =>
        Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
      }
  }

  def telephoneNumberSizeConstraint(implicit messages: Messages): Constraint[String] =
    Constraint[String]((textContent: String) => textContent match {
      case txt if txt.length < 8 || txt.length > 30 => Invalid(messages("tai.canWeContactByPhone.telephone.invalid"))
      case _ => Valid
    })

  def telephoneNumberViewModel(id: Int)(implicit messages: Messages): CanWeContactByPhoneViewModel = CanWeContactByPhoneViewModel(
    messages("tai.updateEmployment.whatDoYouWantToTellUs.preHeading"),
    messages("tai.canWeContactByPhone.title"),
    controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(id).url,
    controllers.employments.routes.UpdateEmploymentController.submitTelephoneNumber().url,
    controllers.employments.routes.UpdateEmploymentController.cancel(id).url
  )

  def updateEmploymentDetails(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      (for {
        userSuppliedDetails <- journeyCacheService.currentValue(UpdateEmployment_EmploymentDetailsKey)
        employment <- employmentService.employment(Nino(user.getNino), empId)
        futureResult <-
          employment match {
            case Some(emp) => {
              val cache = Map(UpdateEmployment_EmploymentIdKey -> empId.toString, UpdateEmployment_NameKey -> emp.name)
              journeyCacheService.cache(cache).map(_ =>
                Ok(views.html.employments.update.whatDoYouWantToTellUs(EmploymentViewModel(emp.name, empId),
                  UpdateEmploymentDetailsForm.form.fill(userSuppliedDetails.getOrElse(""))))
              )
            }
            case _ => throw new RuntimeException("Error during employment details retrieval")
          }
      } yield futureResult ).recover {
        case NonFatal(exception) => internalServerError(exception.getMessage)
      }

  }

  def submitUpdateEmploymentDetails(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      UpdateEmploymentDetailsForm.form.bindFromRequest.fold(
        formWithErrors => {
          journeyCacheService.currentCache map { currentCache =>
            implicit val user = request.taiUser
            BadRequest(views.html.employments.update.whatDoYouWantToTellUs(
              EmploymentViewModel(currentCache(UpdateEmployment_NameKey), empId), formWithErrors))
          }
        },
        employmentDetails => {
          journeyCacheService.cache(Map(UpdateEmployment_EmploymentDetailsKey -> employmentDetails))
            .map(_ => Redirect(controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber()))
        }
      )
  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      for {
        employmentId <- journeyCacheService.mandatoryValueAsInt(EndEmployment_EmploymentIdKey)
        telephoneCache <- journeyCacheService.optionalValues(UpdateEmployment_TelephoneQuestionKey, UpdateEmployment_TelephoneNumberKey)
      } yield {
        implicit val user = request.taiUser
        Ok(views.html.can_we_contact_by_phone(Some(user), telephoneNumberViewModel(employmentId),
          YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache.head, telephoneCache(1)))))
      }

  }

  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      YesNoTextEntryForm.form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
        formWithErrors => {
          journeyCacheService.currentCache map { currentCache =>
            implicit val user = request.taiUser
            BadRequest(views.html.can_we_contact_by_phone(Some(user), telephoneNumberViewModel(currentCache(UpdateEmployment_EmploymentIdKey).toInt), formWithErrors))
          }
        },
        form => {
          val mandatoryData = Map(UpdateEmployment_TelephoneQuestionKey -> Messages(s"tai.label.${form.yesNoChoice.getOrElse(NoValue).toLowerCase}"))
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == YesValue => mandatoryData ++ Map(UpdateEmployment_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(UpdateEmployment_TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.employments.routes.UpdateEmploymentController.updateEmploymentCheckYourAnswers())
          }
        }
      )
  }

  def updateEmploymentCheckYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.collectedValues(Seq(UpdateEmployment_EmploymentIdKey, UpdateEmployment_NameKey,
        UpdateEmployment_EmploymentDetailsKey, UpdateEmployment_TelephoneQuestionKey),
        Seq(UpdateEmployment_TelephoneNumberKey)) map tupled { (mandatorySeq, optionalSeq) => {
        Ok(views.html.employments.update.UpdateEmploymentCheckYourAnswers(UpdateEmploymentCheckYourAnswersViewModel(
          mandatorySeq.head.toInt,
          mandatorySeq(1),
          mandatorySeq(2),
          mandatorySeq(3),
          optionalSeq.head)))
      }
      }

  }

  def submitYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      for {
        (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService.collectedValues(Seq(UpdateEmployment_EmploymentIdKey,
          UpdateEmployment_EmploymentDetailsKey, UpdateEmployment_TelephoneQuestionKey),
          Seq(UpdateEmployment_TelephoneNumberKey))
        model = IncorrectIncome(mandatoryCacheSeq(1), mandatoryCacheSeq(2), optionalCacheSeq.head)
        _ <- employmentService.incorrectEmployment(Nino(user.getNino), mandatoryCacheSeq.head.toInt, model)
        _ <- successfulJourneyCacheService.cache(s"$TrackSuccessfulJourney_UpdateEndEmploymentKey-${mandatoryCacheSeq.head}", true.toString)
        _ <- journeyCacheService.flush
      } yield Redirect(controllers.employments.routes.UpdateEmploymentController.confirmation())
  }

  def confirmation: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      Future.successful(Ok(views.html.employments.confirmation()))
  }
}