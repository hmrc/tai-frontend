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

package controllers.employments

import com.google.inject.name.Named
import controllers._
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import javax.inject.Inject
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.{DuplicateSubmissionWarningForm, EmploymentEndDateForm, IrregularPayForm, UpdateRemoveEmploymentForm}
import uk.gov.hmrc.tai.model.domain.EndEmployment
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, IrregularPayConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, WithinSixWeeksViewModel}
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.addIncomeCheckYourAnswers

import scala.Function.tupled
import scala.concurrent.{ExecutionContext, Future}

class EndEmploymentController @Inject()(
  auditService: AuditService,
  employmentService: EmploymentService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  val auditConnector: AuditConnector,
  mcc: MessagesControllerComponents,
  update_remove_employment_decision: UpdateRemoveEmploymentDecisionView,
  endEmploymentWithinSixWeeksError: EndEmploymentWithinSixWeeksErrorView,
  EndEmploymentIrregularPaymentError: EndEmploymentIrregularPaymentErrorView,
  endEmploymentView: EndEmploymentView,
  can_we_contact_by_phone: CanWeContactByPhoneView,
  duplicateSubmissionWarning: duplicateSubmissionWarning,
  confirmation: ConfirmationView,
  addIncomeCheckYourAnswers: addIncomeCheckYourAnswers,
  @Named("End Employment") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  implicit val templateRenderer: TemplateRenderer,
  implicit val partialRetriever: FormPartialRetriever)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with FormValuesConstants with IrregularPayConstants
    with AuditConstants with EmptyCacheRedirect {

  def cancel(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.flush() map { _ =>
      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
    }
  }

  private def telephoneNumberViewModel(employmentId: Int)(implicit messages: Messages) =
    CanWeContactByPhoneViewModel(
      messages("tai.endEmployment.preHeadingText"),
      messages("tai.canWeContactByPhone.title"),
      controllers.employments.routes.EndEmploymentController.endEmploymentPage().url,
      controllers.employments.routes.EndEmploymentController.submitTelephoneNumber().url,
      controllers.employments.routes.EndEmploymentController.cancel(employmentId).url
    )

  def employmentUpdateRemoveDecision: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService.mandatoryJourneyValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map {
        case Right(mandatoryValues) =>
          Ok(
            update_remove_employment_decision(
              UpdateRemoveEmploymentForm.form,
              mandatoryValues(0),
              mandatoryValues(1).toInt))
        case Left(_) => Redirect(taxAccountSummaryRedirect)
      }
  }

  private def redirectToWarningOrDecisionPage(
    journeyCacheFuture: Future[Map[String, String]],
    successfullJourneyCacheFuture: Future[Option[String]])(implicit hc: HeaderCarrier): Future[Result] =
    for {
      _                      <- journeyCacheFuture
      successfulJourneyCache <- successfullJourneyCacheFuture
    } yield {
      successfulJourneyCache match {
        case Some(_) => Redirect(routes.EndEmploymentController.duplicateSubmissionWarning())
        case _       => Redirect(routes.EndEmploymentController.employmentUpdateRemoveDecision())
      }
    }

  def onPageLoad(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val nino = user.nino

    employmentService.employment(nino, empId) flatMap {
      case Some(employment) =>
        val journeyCacheFuture = journeyCacheService.cache(
          Map(EndEmployment_EmploymentIdKey -> empId.toString, EndEmployment_NameKey -> employment.name))

        val successfulJourneyCacheFuture =
          successfulJourneyCacheService.currentValue(s"$TrackSuccessfulJourney_UpdateEndEmploymentKey-$empId")

        redirectToWarningOrDecisionPage(journeyCacheFuture, successfulJourneyCacheFuture)
      case _ => throw new RuntimeException("No employment found")
    }
  }

  def handleEmploymentUpdateRemove: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) flatMap {
        mandatoryValues =>
          UpdateRemoveEmploymentForm.form.bindFromRequest.fold(
            formWithErrors => {
              Future(
                BadRequest(
                  update_remove_employment_decision(formWithErrors, mandatoryValues(0), mandatoryValues(1).toInt)))
            }, {
              case Some(YesValue) =>
                Future(
                  Redirect(controllers.employments.routes.UpdateEmploymentController
                    .updateEmploymentDetails(mandatoryValues(1).toInt)))
              case _ =>
                val nino = user.nino
                employmentService.employment(nino, mandatoryValues(1).toInt) flatMap {
                  case Some(employment) =>
                    val today = new LocalDate()
                    val latestPaymentDate: Option[LocalDate] = for {
                      latestAnnualAccount <- employment.latestAnnualAccount
                      latestPayment       <- latestAnnualAccount.latestPayment
                    } yield latestPayment.date

                    val hasIrregularPayment = employment.latestAnnualAccount.exists(_.isIrregularPayment)
                    if (latestPaymentDate.isDefined && latestPaymentDate.get
                          .isAfter(today.minusWeeks(6).minusDays(1))) {

                      val errorPagecache = Map(EndEmployment_LatestPaymentDateKey -> latestPaymentDate.get.toString)
                      journeyCacheService.cache(errorPagecache) map { _ =>
                        auditService
                          .createAndSendAuditEvent(EndEmployment_WithinSixWeeksError, Map("nino" -> nino.nino))
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

  def endEmploymentError: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService
      .mandatoryValues(EndEmployment_LatestPaymentDateKey, EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map {
      data =>
        val date = new LocalDate(data.head)
        Ok(
          endEmploymentWithinSixWeeksError(
            WithinSixWeeksViewModel(date.plusWeeks(6).plusDays(1), data(1), date, data(2).toInt)))
    }
  }

  def irregularPaymentError: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map { mandatoryValues =>
      Ok(
        EndEmploymentIrregularPaymentError(
          IrregularPayForm.createForm,
          EmploymentViewModel(mandatoryValues(0), mandatoryValues(1).toInt)))
    }
  }

  def handleIrregularPaymentError: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map { mandatoryValues =>
        IrregularPayForm.createForm.bindFromRequest.fold(
          formWithErrors => {
            BadRequest(
              EndEmploymentIrregularPaymentError(
                formWithErrors,
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

  def endEmploymentPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val nino = user.nino
    journeyCacheService.collectedJourneyValues(
      Seq(EndEmployment_NameKey, EndEmployment_EmploymentIdKey),
      Seq(EndEmployment_EndDateKey)) map tupled { (mandatorySeq, optionalSeq) =>
      {
        mandatorySeq match {
          case Right(mandatorySequence) => {
            optionalSeq match {
              case Seq(Some(date)) =>
                Ok(
                  endEmploymentView(
                    EmploymentEndDateForm(mandatorySequence(0)).form.fill(new LocalDate(date)),
                    EmploymentViewModel(mandatorySequence(0), mandatorySequence(1).toInt)))
              case _ =>
                Ok(
                  endEmploymentView(
                    EmploymentEndDateForm(mandatorySequence(0)).form,
                    EmploymentViewModel(mandatorySequence(0), mandatorySequence(1).toInt)))
            }
          }
          case Left(_) => Redirect(taxAccountSummaryRedirect)
        }
      }
    }
  }

  def handleEndEmploymentPage(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino
      employmentService.employment(nino, employmentId) flatMap {
        case Some(employment) =>
          EmploymentEndDateForm(employment.name).form.bindFromRequest.fold(
            formWithErrors => {
              Future.successful(
                BadRequest(endEmploymentView(formWithErrors, EmploymentViewModel(employment.name, employmentId))))
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

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    for {
      employmentId <- journeyCacheService.mandatoryJourneyValueAsInt(EndEmployment_EmploymentIdKey)
      telephoneCache <- journeyCacheService
                         .optionalValues(EndEmployment_TelephoneQuestionKey, EndEmployment_TelephoneNumberKey)
    } yield {

      employmentId match {
        case Right(mandatoryEmploymentId) =>
          Ok(
            can_we_contact_by_phone(
              Some(user),
              telephoneNumberViewModel(mandatoryEmploymentId),
              YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache(0), telephoneCache(1)))))
        case Left(_) => Redirect(taxAccountSummaryRedirect)
      }
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(TelephoneNumberConstraint.telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors => {
          journeyCacheService.mandatoryValueAsInt(EndEmployment_EmploymentIdKey) map { employmentId =>
            BadRequest(can_we_contact_by_phone(Some(user), telephoneNumberViewModel(employmentId), formWithErrors))
          }
        },
        form => {
          val mandatoryData = Map(
            EndEmployment_TelephoneQuestionKey -> Messages(
              s"tai.label.${form.yesNoChoice.getOrElse(NoValue).toLowerCase}"))
          val dataForCache = form.yesNoChoice match {
            case Some(YesValue) =>
              mandatoryData ++ Map(EndEmployment_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
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
      implicit val user: AuthedUser = request.taiUser

      journeyCacheService.collectedJourneyValues(
        Seq(EndEmployment_EmploymentIdKey, EndEmployment_EndDateKey, EndEmployment_TelephoneQuestionKey),
        Seq(EndEmployment_TelephoneNumberKey)) map tupled { (mandatorySeq, optionalSeq) =>
        mandatorySeq match {
          case Right(mandatoryValues) => {

            val model = IncomeCheckYourAnswersViewModel(
              mandatoryValues(0).toInt,
              Messages("tai.endEmployment.preHeadingText"),
              mandatoryValues(1),
              mandatoryValues(2),
              optionalSeq(0),
              controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url,
              controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url,
              controllers.employments.routes.EndEmploymentController.cancel(mandatoryValues.head.toInt).url
            )
            Ok(addIncomeCheckYourAnswers(model))
          }
          case Left(_) => Redirect(taxAccountSummaryRedirect)
        }
      }
  }

  def confirmAndSendEndEmployment(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino
      for {
        (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService.collectedValues(
                                                  Seq(
                                                    EndEmployment_EmploymentIdKey,
                                                    EndEmployment_EndDateKey,
                                                    EndEmployment_TelephoneQuestionKey),
                                                  Seq(EndEmployment_TelephoneNumberKey))
        model = EndEmployment(LocalDate.parse(mandatoryCacheSeq(1)), mandatoryCacheSeq(2), optionalCacheSeq(0))
        _ <- employmentService.endEmployment(nino, mandatoryCacheSeq(0).toInt, model)
        _ <- successfulJourneyCacheService.cache(
              Map(s"$TrackSuccessfulJourney_UpdateEndEmploymentKey-${mandatoryCacheSeq.head}" -> "true"))
        _ <- journeyCacheService.flush
      } yield Redirect(routes.EndEmploymentController.showConfirmationPage())
  }

  def duplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService.mandatoryJourneyValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map {
      case Right(mandatoryValues) =>
        Ok(
          duplicateSubmissionWarning(
            DuplicateSubmissionWarningForm.createForm,
            mandatoryValues(0),
            mandatoryValues(1).toInt))
      case Left(_) => Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) flatMap {
        mandatoryValues =>
          val empId = mandatoryValues(1).toInt

          DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(duplicateSubmissionWarning(formWithErrors, mandatoryValues(0), empId)))
            },
            success => {
              success.yesNoChoice match {
                case Some(YesValue) =>
                  Future.successful(
                    Redirect(controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision()))
                case Some(NoValue) =>
                  Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
              }
            }
          )
      }
  }

  def showConfirmationPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    Future.successful(Ok(confirmation()))
  }
}
