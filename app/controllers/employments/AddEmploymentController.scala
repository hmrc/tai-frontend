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

import com.google.inject.name.Named
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.{AuthJourney, AuthedUser}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.{AddEmploymentFirstPayForm, AddEmploymentPayrollNumberForm, EmploymentAddDateForm, EmploymentNameForm}
import uk.gov.hmrc.tai.model.domain.AddEmployment
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.FutureOps._
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.PayrollNumberViewModel
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddEmploymentController @Inject() (
  auditService: AuditService,
  employmentService: EmploymentService,
  authenticate: AuthJourney,
  validatePerson: ValidatePerson,
  @Named("Add Employment") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  val auditConnector: AuditConnector,
  mcc: MessagesControllerComponents,
  addEmploymentStartDateForm: AddEmploymentStartDateFormView,
  addEmploymentNameForm: AddEmploymentNameFormView,
  addEmploymentFirstPayForm: AddEmploymentFirstPayFormView,
  addEmploymentErrorPage: AddEmploymentErrorPageView,
  addEmploymentPayrollNumberForm: AddEmploymentPayrollNumberFormView,
  canWeContactByPhone: CanWeContactByPhoneView,
  confirmationView: ConfirmationView,
  addIncomeCheckYourAnswers: AddIncomeCheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with EmptyCacheRedirect {

  def cancel(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService.flush() map { _ =>
      Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def telephoneNumberViewModel(implicit messages: Messages): CanWeContactByPhoneViewModel =
    CanWeContactByPhoneViewModel(
      messages("add.missing.employment"),
      messages("tai.canWeContactByPhone.title"),
      controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber().url,
      controllers.employments.routes.AddEmploymentController.submitTelephoneNumber().url,
      controllers.employments.routes.AddEmploymentController.cancel().url
    )

  def addEmploymentName(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService.currentValue(AddEmploymentConstants.NameKey) map { providedName =>
      implicit val user: AuthedUser = request.taiUser

      Ok(addEmploymentNameForm(EmploymentNameForm.form.fill(providedName.getOrElse(""))))
    }
  }

  def submitEmploymentName(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    EmploymentNameForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          implicit val user: AuthedUser = request.taiUser

          Future.successful(BadRequest(addEmploymentNameForm(formWithErrors)))
        },
        employmentName =>
          journeyCacheService
            .cache(Map(AddEmploymentConstants.NameKey -> employmentName))
            .map(_ => Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentStartDate()))
      )
  }

  def addEmploymentStartDate(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService
      .collectedJourneyValues(Seq(AddEmploymentConstants.NameKey), Seq(AddEmploymentConstants.StartDateKey))
      .map {
        case Right((mandatorySequence, optSeq)) =>
          val form = optSeq.head match {
            case Some(dateString) =>
              EmploymentAddDateForm(mandatorySequence.head).form.fill(LocalDate.parse(dateString))
            case _ => EmploymentAddDateForm(mandatorySequence.head).form
          }
          implicit val user: AuthedUser = request.taiUser

          Ok(addEmploymentStartDateForm(form, mandatorySequence.head))
        case Left(_) =>
          Redirect(taxAccountSummaryRedirect)
      }
  }

  def submitEmploymentStartDate(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService.currentCache map { currentCache =>
      EmploymentAddDateForm(currentCache(AddEmploymentConstants.NameKey)).form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            implicit val user: AuthedUser = request.taiUser

            BadRequest(addEmploymentStartDateForm(formWithErrors, currentCache(AddEmploymentConstants.NameKey)))
          },
          date => {
            val startDateBoundary = LocalDate.now.minusWeeks(6)
            val data = currentCache + (AddEmploymentConstants.StartDateKey -> date.toString)
            if (date.isAfter(startDateBoundary)) {
              val firstPayChoiceCacheData =
                data + (AddEmploymentConstants.StartDateWithinSixWeeks -> FormValuesConstants.YesValue)
              journeyCacheService.cache(firstPayChoiceCacheData)
              Redirect(controllers.employments.routes.AddEmploymentController.receivedFirstPay())
            } else {
              val firstPayChoiceCacheData =
                data + (AddEmploymentConstants.StartDateWithinSixWeeks -> FormValuesConstants.NoValue)
              journeyCacheService.cache(firstPayChoiceCacheData)
              Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber())
            }
          }
        )
    }
  }

  def receivedFirstPay(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService
      .collectedJourneyValues(Seq(AddEmploymentConstants.NameKey), Seq(AddEmploymentConstants.ReceivedFirstPayKey))
      .getOrFail
      .map { case (mandSeq, optSeq) =>
        implicit val user: AuthedUser = request.taiUser
        Ok(addEmploymentFirstPayForm(AddEmploymentFirstPayForm.form.fill(optSeq.head), mandSeq.head))
      }
  }

  def submitFirstPay(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    AddEmploymentFirstPayForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          journeyCacheService.mandatoryJourneyValue(AddEmploymentConstants.NameKey).getOrFail.map { employmentName =>
            implicit val user: AuthedUser = request.taiUser
            BadRequest(addEmploymentFirstPayForm(formWithErrors, employmentName))
          },
        firstPayYesNo =>
          journeyCacheService.cache(AddEmploymentConstants.ReceivedFirstPayKey, firstPayYesNo.getOrElse("")) map { _ =>
            firstPayYesNo match {
              case Some(FormValuesConstants.YesValue) =>
                Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber())
              case _ => Redirect(controllers.employments.routes.AddEmploymentController.sixWeeksError())
            }
          }
      )
  }

  def sixWeeksError(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService.mandatoryJourneyValue(AddEmploymentConstants.NameKey).map {
      case Right(employmentName) =>
        implicit val user: AuthedUser = request.taiUser
        auditService
          .createAndSendAuditEvent(AuditConstants.AddEmploymentCantAddEmployer, Map("nino" -> user.nino.toString()))
        Ok(addEmploymentErrorPage(employmentName))
      case Left(err) =>
        InternalServerError(err)
    }
  }

  def addEmploymentPayrollNumber(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService.currentCache map { cache =>
      val viewModel = PayrollNumberViewModel(cache)
      val payrollChoice = cache.get(AddEmploymentConstants.PayrollNumberQuestionKey)
      val payroll = payrollChoice match {
        case Some(FormValuesConstants.YesValue) => cache.get(AddEmploymentConstants.PayrollNumberKey)
        case _                                  => None
      }
      implicit val user: AuthedUser = request.taiUser

      Ok(
        addEmploymentPayrollNumberForm(
          AddEmploymentPayrollNumberForm.form.fill(AddEmploymentPayrollNumberForm(payrollChoice, payroll)),
          viewModel
        )
      )
    }
  }

  def submitEmploymentPayrollNumber(): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      AddEmploymentPayrollNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            journeyCacheService.currentCache map { cache =>
              val viewModel = PayrollNumberViewModel(cache)
              implicit val user: AuthedUser = request.taiUser

              BadRequest(addEmploymentPayrollNumberForm(formWithErrors, viewModel))
            },
          form => {
            val payrollNumberToCache = Map(
              AddEmploymentConstants.PayrollNumberQuestionKey -> form.payrollNumberChoice.getOrElse(""),
              AddEmploymentConstants.PayrollNumberKey -> form.payrollNumberEntry
                .getOrElse(Messages("tai.addEmployment.employmentPayrollNumber.notKnown"))
            )
            journeyCacheService
              .cache(payrollNumberToCache)
              .map(_ => Redirect(controllers.employments.routes.AddEmploymentController.addTelephoneNumber()))
          }
        )
  }

  def addTelephoneNumber(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService
      .optionalValues(AddEmploymentConstants.TelephoneQuestionKey, AddEmploymentConstants.TelephoneNumberKey) map {
      optSeq =>
        val telNoToDisplay = optSeq.head match {
          case Some(FormValuesConstants.YesValue) => optSeq(1)
          case _                                  => None
        }
        implicit val user: AuthedUser = request.taiUser

        Ok(
          canWeContactByPhone(
            Some(user),
            telephoneNumberViewModel,
            YesNoTextEntryForm.form().fill(YesNoTextEntryForm(optSeq.head, telNoToDisplay))
          )
        )
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(TelephoneNumberConstraint.telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors => {
          implicit val user: AuthedUser = request.taiUser

          Future.successful(BadRequest(canWeContactByPhone(Some(user), telephoneNumberViewModel, formWithErrors)))
        },
        form => {
          val mandatoryData = Map(
            AddEmploymentConstants.TelephoneQuestionKey -> Messages(
              s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}"
            )
          )
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == FormValuesConstants.YesValue =>
              mandatoryData ++ Map(AddEmploymentConstants.TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(AddEmploymentConstants.TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentCheckYourAnswers())
          }
        }
      )
  }

  def addEmploymentCheckYourAnswers(): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      journeyCacheService
        .collectedJourneyValues(
          Seq(
            AddEmploymentConstants.NameKey,
            AddEmploymentConstants.StartDateKey,
            AddEmploymentConstants.PayrollNumberKey,
            AddEmploymentConstants.TelephoneQuestionKey
          ),
          Seq(AddEmploymentConstants.TelephoneNumberKey)
        )
        .map {
          case Right((mandatoryJourneyValues, optionalVals)) =>
            val model =
              IncomeCheckYourAnswersViewModel(
                Messages("add.missing.employment"),
                mandatoryJourneyValues.head,
                mandatoryJourneyValues(1),
                mandatoryJourneyValues(2),
                mandatoryJourneyValues(3),
                optionalVals.head,
                controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url,
                controllers.employments.routes.AddEmploymentController.submitYourAnswers().url,
                controllers.employments.routes.AddEmploymentController.cancel().url
              )
            Ok(addIncomeCheckYourAnswers(model))
          case Left(_) =>
            Redirect(taxAccountSummaryRedirect)
        }
  }

  def submitYourAnswers: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    for {
      (mandatoryVals, optionalVals) <- journeyCacheService
                                         .collectedJourneyValues(
                                           Seq(
                                             AddEmploymentConstants.NameKey,
                                             AddEmploymentConstants.StartDateKey,
                                             AddEmploymentConstants.PayrollNumberKey,
                                             AddEmploymentConstants.TelephoneQuestionKey
                                           ),
                                           Seq(AddEmploymentConstants.TelephoneNumberKey)
                                         )
                                         .getOrFail
      model = AddEmployment(
                mandatoryVals.head,
                LocalDate.parse(mandatoryVals(1)),
                mandatoryVals(2),
                mandatoryVals(3),
                optionalVals.head
              )
      _ <- employmentService.addEmployment(user.nino, model)
      _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourneyConstants.AddEmploymentKey, "true")
      _ <- journeyCacheService.flush()
    } yield Redirect(controllers.employments.routes.AddEmploymentController.confirmation())
  }

  def confirmation: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    Future.successful(Ok(confirmationView()))

  }

}
