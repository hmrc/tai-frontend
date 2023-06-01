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

import cats.data.EitherT
import cats.implicits._
import com.google.inject.name.Named
import controllers._
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.{DuplicateSubmissionWarningForm, EmploymentEndDateForm, IrregularPayForm, UpdateRemoveEmploymentForm}
import uk.gov.hmrc.tai.model.domain.EndEmployment
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.FutureOps._
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, IrregularPayConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, WithinSixWeeksViewModel}
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EndEmploymentController @Inject() (
  auditService: AuditService,
  employmentService: EmploymentService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  val auditConnector: AuditConnector,
  mcc: MessagesControllerComponents,
  errorPagesHandler: ErrorPagesHandler,
  updateRemoveEmploymentDecision: UpdateRemoveEmploymentDecisionView,
  endEmploymentWithinSixWeeksError: EndEmploymentWithinSixWeeksErrorView,
  endEmploymentIrregularPaymentError: EndEmploymentIrregularPaymentErrorView,
  endEmploymentView: EndEmploymentView,
  canWeContactByPhone: CanWeContactByPhoneView,
  duplicateSubmissionWarning: DuplicateSubmissionWarningView,
  confirmation: ConfirmationView,
  addIncomeCheckYourAnswers: AddIncomeCheckYourAnswersView,
  @Named("End Employment") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with EmptyCacheRedirect {

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
      journeyCacheService
        .mandatoryJourneyValues(EndEmploymentConstants.NameKey, EndEmploymentConstants.EmploymentIdKey) map {
        case Right(mandatoryValues) =>
          Ok(
            updateRemoveEmploymentDecision(
              UpdateRemoveEmploymentForm.form(mandatoryValues.head),
              mandatoryValues.head,
              mandatoryValues(1).toInt
            )
          )
        case Left(_) => Redirect(taxAccountSummaryRedirect)
      }
  }

  private def redirectToWarningOrDecisionPage(
    journeyCacheFuture: Future[Map[String, String]],
    successfulJourneyCacheFuture: Future[Option[String]]
  ): Future[Result] =
    for {
      _                      <- journeyCacheFuture
      successfulJourneyCache <- successfulJourneyCacheFuture
    } yield successfulJourneyCache match {
      case Some(_) => Redirect(controllers.employments.routes.EndEmploymentController.duplicateSubmissionWarning())
      case _       => Redirect(controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision())
    }

  def onPageLoad(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val nino = user.nino

    employmentService.employment(nino, empId) flatMap { case Some(employment) =>
      val journeyCacheFuture = journeyCacheService.cache(
        Map(EndEmploymentConstants.EmploymentIdKey -> empId.toString, EndEmploymentConstants.NameKey -> employment.name)
      )

      val successfulJourneyCacheFuture =
        successfulJourneyCacheService.currentValue(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$empId")

      redirectToWarningOrDecisionPage(journeyCacheFuture, successfulJourneyCacheFuture)
    } recover { case _ =>
      NotFound(errorPagesHandler.error4xxPageWithLink("No employment found"))
    }
  }

  def handleEmploymentUpdateRemove: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService
        .mandatoryJourneyValues(EndEmploymentConstants.NameKey, EndEmploymentConstants.EmploymentIdKey)
        .getOrFail
        .flatMap { mandatoryJourneyValues =>
          UpdateRemoveEmploymentForm
            .form(mandatoryJourneyValues.head)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future(
                  BadRequest(
                    updateRemoveEmploymentDecision(
                      formWithErrors,
                      mandatoryJourneyValues.head,
                      mandatoryJourneyValues(1).toInt
                    )
                  )
                ),
              {
                case Some(FormValuesConstants.YesValue) =>
                  Future(
                    Redirect(
                      controllers.employments.routes.UpdateEmploymentController
                        .updateEmploymentDetails(mandatoryJourneyValues(1).toInt)
                    )
                  )
                case _ =>
                  val nino = user.nino
                  employmentService.employment(nino, mandatoryJourneyValues(1).toInt) flatMap { case Some(employment) =>
                    val today = LocalDate.now
                    val latestPaymentDate: Option[LocalDate] = for {
                      latestAnnualAccount <- employment.latestAnnualAccount
                      latestPayment       <- latestAnnualAccount.latestPayment
                    } yield latestPayment.date

                    val hasIrregularPayment = employment.latestAnnualAccount.exists(_.isIrregularPayment)
                    if (
                      latestPaymentDate.isDefined && latestPaymentDate.get
                        .isAfter(today.minusWeeks(6).minusDays(1))
                    ) {

                      val errorPageCache =
                        Map(EndEmploymentConstants.LatestPaymentDateKey -> latestPaymentDate.get.toString)
                      journeyCacheService.cache(errorPageCache) map { _ =>
                        auditService
                          .createAndSendAuditEvent(
                            AuditConstants.EndEmploymentWithinSixWeeksError,
                            Map("nino" -> nino.nino)
                          )
                        Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentError())
                      }
                    } else if (hasIrregularPayment) {
                      auditService
                        .createAndSendAuditEvent(AuditConstants.EndEmploymentIrregularPayment, Map("nino" -> nino.nino))
                      Future(Redirect(controllers.employments.routes.EndEmploymentController.irregularPaymentError()))
                    } else {
                      Future(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage()))
                    }
                  } recover { case _ =>
                    NotFound(errorPagesHandler.error4xxPageWithLink("No employment found"))
                  }
              }
            )
        }
  }

  def endEmploymentError: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService
      .mandatoryJourneyValues(
        EndEmploymentConstants.LatestPaymentDateKey,
        EndEmploymentConstants.NameKey,
        EndEmploymentConstants.EmploymentIdKey
      )
      .getOrFail
      .map { data =>
        val date = LocalDate.parse(data.head)
        Ok(
          endEmploymentWithinSixWeeksError(
            WithinSixWeeksViewModel(date.plusWeeks(6).plusDays(1), data(1), date, data(2).toInt)
          )
        )
      }
  }

  def irregularPaymentError: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    EitherT(
      journeyCacheService
        .mandatoryJourneyValues(EndEmploymentConstants.NameKey, EndEmploymentConstants.EmploymentIdKey)
    )
      .map { mandatoryJourneyValues =>
        Ok(
          endEmploymentIrregularPaymentError(
            IrregularPayForm.createForm,
            EmploymentViewModel(mandatoryJourneyValues.head, mandatoryJourneyValues(1).toInt)
          )
        )
      }
      .getOrElse {
        InternalServerError(errorPagesHandler.error5xx("Could not retrieve mandatory journey values"))
      }
  }

  def handleIrregularPaymentError: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService
        .mandatoryJourneyValues(EndEmploymentConstants.NameKey, EndEmploymentConstants.EmploymentIdKey)
        .getOrFail
        .map { mandatoryJourneyValues =>
          IrregularPayForm.createForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                BadRequest(
                  endEmploymentIrregularPaymentError(
                    formWithErrors,
                    EmploymentViewModel(mandatoryJourneyValues.head, mandatoryJourneyValues(1).toInt)
                  )
                ),
              formData =>
                formData.irregularPayDecision match {
                  case Some(IrregularPayConstants.ContactEmployer) =>
                    Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
                  case _ =>
                    Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage())
                }
            )
        }
  }

  def endEmploymentPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheService
      .collectedJourneyValues(
        Seq(EndEmploymentConstants.NameKey, EndEmploymentConstants.EmploymentIdKey),
        Seq(EndEmploymentConstants.EndDateKey)
      )
      .map {
        case Right((mandatorySequence, optionalSeq)) =>
          optionalSeq match {
            case Seq(Some(date)) =>
              Ok(
                endEmploymentView(
                  EmploymentEndDateForm(mandatorySequence.head).form.fill(LocalDate.parse(date)),
                  EmploymentViewModel(mandatorySequence.head, mandatorySequence(1).toInt)
                )
              )
            case _ =>
              Ok(
                endEmploymentView(
                  EmploymentEndDateForm(mandatorySequence.head).form,
                  EmploymentViewModel(mandatorySequence.head, mandatorySequence(1).toInt)
                )
              )
          }
        case Left(_) =>
          Redirect(taxAccountSummaryRedirect)
      }
  }

  def handleEndEmploymentPage(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino
      employmentService.employment(nino, employmentId).flatMap {
        case Some(employment) =>
          EmploymentEndDateForm(employment.name).form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(endEmploymentView(formWithErrors, EmploymentViewModel(employment.name, employmentId)))
                ),
              date => {
                val employmentJourneyCacheData = Map(EndEmploymentConstants.EndDateKey -> date.toString)
                journeyCacheService.cache(employmentJourneyCacheData) map { _ =>
                  Redirect(controllers.employments.routes.EndEmploymentController.addTelephoneNumber())
                }
              }
            )
        case _ => Future.successful(NotFound(errorPagesHandler.error4xxPageWithLink("No employment found")))
      }
  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (
      journeyCacheService.mandatoryJourneyValueAsInt(EndEmploymentConstants.EmploymentIdKey),
      journeyCacheService
        .optionalValues(EndEmploymentConstants.TelephoneQuestionKey, EndEmploymentConstants.TelephoneNumberKey)
    ).mapN {
      case (Right(mandatoryEmploymentId), telephoneCache) =>
        Ok(
          canWeContactByPhone(
            Some(user),
            telephoneNumberViewModel(mandatoryEmploymentId),
            YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache.head, telephoneCache(1)))
          )
        )
      case _ => Redirect(taxAccountSummaryRedirect)
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
        formWithErrors =>
          journeyCacheService.mandatoryJourneyValueAsInt(EndEmploymentConstants.EmploymentIdKey) map {
            case Right(employmentId) =>
              BadRequest(canWeContactByPhone(Some(user), telephoneNumberViewModel(employmentId), formWithErrors))
          },
        form => {
          val mandatoryData = Map(
            EndEmploymentConstants.TelephoneQuestionKey -> Messages(
              s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}"
            )
          )
          val dataForCache = form.yesNoChoice match {
            case Some(FormValuesConstants.YesValue) =>
              mandatoryData ++ Map(EndEmploymentConstants.TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(EndEmploymentConstants.TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentCheckYourAnswers())
          }
        }
      )
  }

  def endEmploymentCheckYourAnswers: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService
        .collectedJourneyValues(
          Seq(
            EndEmploymentConstants.EmploymentIdKey,
            EndEmploymentConstants.EndDateKey,
            EndEmploymentConstants.TelephoneQuestionKey
          ),
          Seq(EndEmploymentConstants.TelephoneNumberKey)
        )
        .map {
          case Right((mandatoryValues, optionalSeq)) =>
            val model = IncomeCheckYourAnswersViewModel(
              employmentId = mandatoryValues.head.toInt,
              preHeading = Messages("tai.endEmployment.preHeadingText"),
              incomeSourceEnd = mandatoryValues(1),
              contactableByPhone = mandatoryValues(2),
              phoneNumber = optionalSeq.head,
              backLinkUrl = controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url,
              submissionUrl = controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url,
              cancelUrl = controllers.employments.routes.EndEmploymentController.cancel(mandatoryValues.head.toInt).url
            )
            Ok(addIncomeCheckYourAnswers(model))
          case Left(_) => Redirect(taxAccountSummaryRedirect)
        }
  }

  def confirmAndSendEndEmployment(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino
      for {
        (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService
                                                   .collectedJourneyValues(
                                                     Seq(
                                                       EndEmploymentConstants.EmploymentIdKey,
                                                       EndEmploymentConstants.EndDateKey,
                                                       EndEmploymentConstants.TelephoneQuestionKey
                                                     ),
                                                     Seq(EndEmploymentConstants.TelephoneNumberKey)
                                                   )
                                                   .getOrFail
        model = EndEmployment(LocalDate.parse(mandatoryCacheSeq(1)), mandatoryCacheSeq(2), optionalCacheSeq.head)
        _ <- employmentService.endEmployment(nino, mandatoryCacheSeq.head.toInt, model)
        _ <- successfulJourneyCacheService.cache(
               Map(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-${mandatoryCacheSeq.head}" -> "true")
             )
        _ <- journeyCacheService.flush()
      } yield Redirect(controllers.employments.routes.EndEmploymentController.showConfirmationPage())
  }

  def duplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService
      .mandatoryJourneyValues(EndEmploymentConstants.NameKey, EndEmploymentConstants.EmploymentIdKey) map {
      case Right(mandatoryValues) =>
        Ok(
          duplicateSubmissionWarning(
            DuplicateSubmissionWarningForm.createForm,
            mandatoryValues.head,
            mandatoryValues(1).toInt
          )
        )
      case Left(_) => Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService
        .mandatoryJourneyValues(EndEmploymentConstants.NameKey, EndEmploymentConstants.EmploymentIdKey)
        .getOrFail
        .flatMap { mandatoryValues =>
          val empId = mandatoryValues(1).toInt

          DuplicateSubmissionWarningForm.createForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(duplicateSubmissionWarning(formWithErrors, mandatoryValues.head, empId))),
              success =>
                success.yesNoChoice match {
                  case Some(FormValuesConstants.YesValue) =>
                    Future.successful(
                      Redirect(controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision())
                    )
                  case Some(FormValuesConstants.NoValue) =>
                    Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
                }
            )
        }
  }

  def showConfirmationPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    Future.successful(Ok(confirmation()))
  }
}
