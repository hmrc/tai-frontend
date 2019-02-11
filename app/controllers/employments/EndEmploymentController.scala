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

import com.google.inject.Inject
import com.google.inject.name.Named
import controllers._
import controllers.actions.ValidatePerson
import controllers.audit.Auditable
import controllers.auth.{AuthAction, AuthedUser, WithAuthorisedForTaiLite}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.employments.{DuplicateSubmissionWarningForm, EmploymentEndDateForm, IrregularPayForm, UpdateRemoveEmploymentForm}
import uk.gov.hmrc.tai.model.domain.{Employment, EndEmployment}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService, PersonService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, IrregularPayConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, WithinSixWeeksViewModel}
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel

import scala.Function.tupled
import scala.concurrent.Future
import scala.util.control.NonFatal

class EndEmploymentController @Inject()(auditService: AuditService,
                                        employmentService: EmploymentService,
                                        authenticate: AuthAction,
                                        validatePerson: ValidatePerson,
                                        @Named("End Employment") journeyCacheService: JourneyCacheService,
                                        @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
                                        val auditConnector: AuditConnector,
                                        implicit val templateRenderer: TemplateRenderer,
                                        implicit val partialRetriever: FormPartialRetriever
                                       ) extends TaiBaseController
  with JourneyCacheConstants
  with FormValuesConstants
  with IrregularPayConstants
  with AuditConstants {

  private def telephoneNumberViewModel(employmentId: Int)(implicit messages: Messages) = CanWeContactByPhoneViewModel(
    messages("tai.endEmployment.preHeadingText"),
    messages("tai.canWeContactByPhone.title"),
    controllers.employments.routes.EndEmploymentController.endEmploymentPage().url,
    controllers.employments.routes.EndEmploymentController.submitTelephoneNumber().url,
    controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId).url)

  private def telephoneNumberSizeConstraint(implicit messages: Messages): Constraint[String] =
    Constraint[String]((textContent: String) => textContent match {
      case txt if txt.length < 8 || txt.length > 30 => Invalid(messages("tai.canWeContactByPhone.telephone.invalid"))
      case _ => Valid
    })

  def employmentUpdateRemoveDecision: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map { mandatoryValues =>
        Ok(views.html.employments.update_remove_employment_decision(UpdateRemoveEmploymentForm.form, mandatoryValues(0), mandatoryValues(1).toInt))
      }
  }

  private def redirectToWarningOrDecisionPage(journeyCacheFuture: Future[Map[String, String]],
                                              successfullJourneyCacheFuture: Future[Option[String]])
                                             (implicit hc: HeaderCarrier): Future[Result] = {
    for {
      _ <- journeyCacheFuture
      successfulJourneyCache <- successfullJourneyCacheFuture
    } yield {
      successfulJourneyCache match {
        case Some(_) => Redirect(routes.EndEmploymentController.duplicateSubmissionWarning())
        case _ => Redirect(routes.EndEmploymentController.employmentUpdateRemoveDecision())
      }
    }
  }

  def employmentUpdateRemove(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val nino = Nino(user.getNino)

      employmentService.employment(nino, empId) flatMap {
        case Some(employment) => {

          val journeyCacheFuture = journeyCacheService.
            cache(Map(EndEmployment_EmploymentIdKey -> empId.toString, EndEmployment_NameKey -> employment.name))

          val successfullJourneyCacheFuture = successfulJourneyCacheService.currentValue(s"$TrackSuccessfulJourney_UpdateEndEmploymentKey-${empId}")

          redirectToWarningOrDecisionPage(journeyCacheFuture, successfullJourneyCacheFuture)
        }
        case _ => throw new RuntimeException("No employment found")
      }
  }

  def handleEmploymentUpdateRemove: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) flatMap { mandatoryValues =>
        UpdateRemoveEmploymentForm.form.bindFromRequest.fold(
          formWithErrors => {
            Future(BadRequest(views.html.employments.update_remove_employment_decision(formWithErrors, mandatoryValues(0), mandatoryValues(1).toInt)))
          },
          {
            case Some(YesValue) => Future(Redirect(controllers.employments.routes.UpdateEmploymentController.
              updateEmploymentDetails(mandatoryValues(1).toInt)))
            case _ =>
              val nino = Nino(user.getNino)
              employmentService.employment(nino, mandatoryValues(1).toInt) flatMap {
                case Some(employment) =>
                  val today = new LocalDate()
                  val latestPaymentDate: Option[LocalDate] = for {
                    latestAnnualAccount <- employment.latestAnnualAccount
                    latestPayment <- latestAnnualAccount.latestPayment
                  } yield latestPayment.date

                  val hasIrregularPayment = employment.latestAnnualAccount.exists(_.isIrregularPayment)
                  if (latestPaymentDate.isDefined && latestPaymentDate.get.isAfter(today.minusWeeks(6).minusDays(1))) {

                    val errorPagecache = Map(EndEmployment_LatestPaymentDateKey -> latestPaymentDate.get.toString)
                    journeyCacheService.cache(errorPagecache) map { _ =>
                      auditService.createAndSendAuditEvent(EndEmployment_WithinSixWeeksError, Map("nino" -> nino.nino))
                      Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentError())
                    }
                  } else if (hasIrregularPayment) {
                    auditService.createAndSendAuditEvent(EndEmployment_IrregularPayment, Map("nino" -> nino.nino))
                    Future(Redirect(controllers.employments.routes.EndEmploymentController.irregularPaymentError()))
                  } else {
                    Future(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage()))
                  }
                case _ => throw new RuntimeException("No employment found")
              }
          }
        )
      }
  }

  def endEmploymentError: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_LatestPaymentDateKey, EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map { data =>
        val date = new LocalDate(data.head)
        Ok(views.html.employments.endEmploymentWithinSixWeeksError(WithinSixWeeksViewModel(date.plusWeeks(6).plusDays(1), data(1), date, data(2).toInt)))
      }
  }

  def irregularPaymentError: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map { mandatoryValues =>
        Ok(views.html.employments.EndEmploymentIrregularPaymentError(IrregularPayForm.createForm,
          EmploymentViewModel(mandatoryValues(0), mandatoryValues(1).toInt)))
      }
  }


  def handleIrregularPaymentError: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map { mandatoryValues =>
        IrregularPayForm.createForm.bindFromRequest.fold(
          formWithErrors => {
            BadRequest(views.html.employments.EndEmploymentIrregularPaymentError(formWithErrors,
              EmploymentViewModel(mandatoryValues(0), mandatoryValues(1).toInt)))
          },
          formData => {
            formData.irregularPayDecision match {
              case Some(ContactEmployer) =>
                Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
              case _ =>
                Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage())
            }
          }
        )
      }
  }

  def endEmploymentPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      val nino = Nino(user.getNino)
      journeyCacheService.collectedValues(Seq(EndEmployment_NameKey, EndEmployment_EmploymentIdKey),
        Seq(EndEmployment_EndDateKey)) map tupled { (mandatorySeq, optionalSeq) => {
        optionalSeq match {
          case Seq(Some(date)) => Ok(views.html.employments.endEmployment(EmploymentEndDateForm(mandatorySeq(0))
            .form.fill(new LocalDate(date)), EmploymentViewModel(mandatorySeq(0), mandatorySeq(1).toInt)))
          case _ => Ok(views.html.employments.endEmployment(EmploymentEndDateForm(mandatorySeq(0)).form,
            EmploymentViewModel(mandatorySeq(0), mandatorySeq(1).toInt)))
        }
      }
      }
  }

  def handleEndEmploymentPage(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      val nino = Nino(user.getNino)
      employmentService.employment(nino, employmentId) flatMap {
        case Some(employment) =>
          EmploymentEndDateForm(employment.name).form.bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(views.html.employments.endEmployment(formWithErrors, EmploymentViewModel(employment.name, employmentId))))
            },
            date => {
              val employmentJourneyCacheData = Map(EndEmployment_EndDateKey -> date.toString)
              journeyCacheService.cache(employmentJourneyCacheData) map { _ =>
                Redirect(controllers.employments.routes.EndEmploymentController.addTelephoneNumber())
              }
            }
          )
        case _ =>
          throw new RuntimeException("No employment found")
      }

  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      for {
        employmentId <- journeyCacheService.mandatoryValueAsInt(EndEmployment_EmploymentIdKey)
        telephoneCache <- journeyCacheService.optionalValues(EndEmployment_TelephoneQuestionKey, EndEmployment_TelephoneNumberKey)
      } yield {
        Ok(views.html.can_we_contact_by_phone(Some(user), None, telephoneNumberViewModel(employmentId),
          YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache(0), telephoneCache(1)))))
      }
  }

  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      YesNoTextEntryForm.form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
        formWithErrors => {
          journeyCacheService.mandatoryValueAsInt(EndEmployment_EmploymentIdKey) map { employmentId =>
            BadRequest(views.html.can_we_contact_by_phone(Some(user), None, telephoneNumberViewModel(employmentId), formWithErrors))
          }
        },
        form => {
          val mandatoryData = Map(EndEmployment_TelephoneQuestionKey -> Messages(s"tai.label.${form.yesNoChoice.getOrElse(NoValue).toLowerCase}"))
          val dataForCache = form.yesNoChoice match {
            case Some(YesValue) => mandatoryData ++ Map(EndEmployment_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(EndEmployment_TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentCheckYourAnswers())
          }
        }
      )
  }

  def endEmploymentCheckYourAnswers: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.collectedValues(Seq(EndEmployment_EmploymentIdKey,
        EndEmployment_EndDateKey, EndEmployment_TelephoneQuestionKey),
        Seq(EndEmployment_TelephoneNumberKey)) map tupled { (mandatorySeq, optionalSeq) =>
        val model = IncomeCheckYourAnswersViewModel(mandatorySeq(0).toInt, Messages("tai.endEmployment.preHeadingText"),
          mandatorySeq(1), mandatorySeq(2), optionalSeq(0),
          controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url,
          controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url,
          controllers.routes.IncomeSourceSummaryController.onPageLoad(mandatorySeq(0).toInt).url)
        Ok(views.html.incomes.addIncomeCheckYourAnswers(model))
      }
  }


  def confirmAndSendEndEmployment(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      val nino = Nino(user.getNino)
      for {
        (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService.collectedValues(Seq(EndEmployment_EmploymentIdKey, EndEmployment_EndDateKey,
          EndEmployment_TelephoneQuestionKey), Seq(EndEmployment_TelephoneNumberKey))
        model = EndEmployment(LocalDate.parse(mandatoryCacheSeq(1)), mandatoryCacheSeq(2), optionalCacheSeq(0))
        _ <- employmentService.endEmployment(nino, mandatoryCacheSeq(0).toInt, model)
        _ <- successfulJourneyCacheService.cache(Map(s"$TrackSuccessfulJourney_UpdateEndEmploymentKey-${mandatoryCacheSeq.head}" -> "true"))
        _ <- journeyCacheService.flush
      } yield Redirect(routes.EndEmploymentController.showConfirmationPage())
  }

  def duplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map { mandatoryValues =>
        Ok(views.html.employments.duplicateSubmissionWarning(DuplicateSubmissionWarningForm.createForm, mandatoryValues(0), mandatoryValues(1).toInt))
      }
  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) flatMap { mandatoryValues =>
        DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.employments.
              duplicateSubmissionWarning(formWithErrors, mandatoryValues(0), mandatoryValues(1).toInt)))
          },
          success => {
            success.yesNoChoice match {
              case Some(YesValue) => Future.successful(Redirect(controllers.employments.routes.EndEmploymentController.
                employmentUpdateRemoveDecision()))
              case Some(NoValue) => Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.
                onPageLoad(mandatoryValues(1).toInt)))
            }
          }
        )
      }
  }

  def showConfirmationPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request => Future.successful(Ok(views.html.employments.confirmation()))
  }
}