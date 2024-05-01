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
import controllers.{ErrorPagesHandler, TaiBaseController}
import controllers.auth.{AuthJourney, AuthedUser}
import pages.AddEmployment.{AddEmploymentNamePage, AddEmploymentReceivedFirstPayPage, AddEmploymentStartDatePage, AddEmploymentStartDateWithinSixWeeksPage}
import play.api.i18n.Messages
import play.api.libs.json.Format.GenericFormat
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.{AddEmploymentFirstPayForm, AddEmploymentPayrollNumberForm, EmploymentAddDateForm, EmploymentNameForm}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
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
  journeyCacheNewRepository: JourneyCacheNewRepository,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  @Named("Add Employment") journeyCacheService: JourneyCacheService,
  val auditConnector: AuditConnector,
  mcc: MessagesControllerComponents,
  addEmploymentStartDateForm: AddEmploymentStartDateFormView,
  addEmploymentNameForm: AddEmploymentNameFormView,
  addEmploymentFirstPayForm: AddEmploymentFirstPayFormView,
  addEmploymentErrorPage: AddEmploymentErrorPageView,
  addEmploymentPayrollNumberForm: AddEmploymentPayrollNumberFormView,
  canWeContactByPhone: CanWeContactByPhoneView,
  confirmationView: ConfirmationView,
  addIncomeCheckYourAnswers: AddIncomeCheckYourAnswersView,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with EmptyCacheRedirect {

  def cancel(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    journeyCacheNewRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino) map { _ =>
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

  private def error5xxInBadRequest()(implicit request: Request[_]): Result =
    BadRequest(errorPagesHandler.error5xx(Messages("global.error.InternalServerError500.message")))

  def addEmploymentName(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    Ok(
      addEmploymentNameForm(EmploymentNameForm.form.fill(request.userAnswers.get(AddEmploymentNamePage).getOrElse("")))
    )
  }

  def submitEmploymentName(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    EmploymentNameForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          implicit val user: AuthedUser = request.taiUser

          Future.successful(BadRequest(addEmploymentNameForm(formWithErrors)))
        },
        employmentName =>
          for {
            _ <-
              journeyCacheNewRepository.set(request.userAnswers.setOrException(AddEmploymentNamePage, employmentName))
          } yield Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentStartDate())
      )
  }

  def addEmploymentStartDate(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    println("vvvvvv " + request.userAnswers)
    (
      request.userAnswers.get(AddEmploymentNamePage),
      request.userAnswers.get(AddEmploymentStartDatePage)
    ) match {
      case (Some(name), Some(date)) =>
        Ok(addEmploymentStartDateForm(EmploymentAddDateForm(name).form.fill(date), name))
      case (Some(name), None) =>
        Ok(addEmploymentStartDateForm(EmploymentAddDateForm(name).form, name))
      case _ => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
    }
  }

  def submitEmploymentStartDate(): Action[AnyContent] = authenticate.authWithDataRetrieval async { implicit request =>
    request.userAnswers
      .get(AddEmploymentNamePage)
      .fold(
        Future.successful(error5xxInBadRequest())
      )(cachedValue =>
        EmploymentAddDateForm(cachedValue).form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              implicit val user: AuthedUser = request.taiUser

              Future.successful(BadRequest(addEmploymentStartDateForm(formWithErrors, cachedValue)))
            },
            date => {
              val startDateBoundary = LocalDate.now.minusWeeks(6)
              val startDateAnswers = request.userAnswers.setOrException(AddEmploymentStartDatePage, date)
              val wholeAnswers = if (date.isAfter(startDateBoundary)) {
                startDateAnswers.setOrException(AddEmploymentStartDateWithinSixWeeksPage, FormValuesConstants.YesValue)
              } else {
                startDateAnswers.setOrException(AddEmploymentStartDateWithinSixWeeksPage, FormValuesConstants.NoValue)
              }
              for {
                _ <- journeyCacheNewRepository.set(wholeAnswers)
              } yield
                if (date.isAfter(startDateBoundary)) {
                  Redirect(controllers.employments.routes.AddEmploymentController.receivedFirstPay())
                } else {
                  Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber())
                }
            }
          )
      )
  }

  def receivedFirstPay(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val authedUser: AuthedUser = request.taiUser
    (request.userAnswers.get(AddEmploymentNamePage), request.userAnswers.get(AddEmploymentReceivedFirstPayPage)) match {
      case (Some(name), bool) =>
        println(s"bbbbb $name $bool ${request.userAnswers}")
        Ok(addEmploymentFirstPayForm(AddEmploymentFirstPayForm.form.fill(bool), name))
      case _ =>
        println("aaaaaaaaa error5xx")
        error5xxInBadRequest()
    }
  }

  def submitFirstPay(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    request.userAnswers
      .get(AddEmploymentNamePage)
      .fold(
        Future.successful(error5xxInBadRequest())
      )(employmentName =>
        AddEmploymentFirstPayForm.form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              implicit val user: AuthedUser = request.taiUser
              Future.successful(BadRequest(addEmploymentFirstPayForm(formWithErrors, employmentName)))
            },
            firstPayYesNo =>
              for {
                _ <-
                  journeyCacheNewRepository
                    .set(
                      request.userAnswers.setOrException(AddEmploymentReceivedFirstPayPage, firstPayYesNo.getOrElse(""))
                    )
              } yield firstPayYesNo match {
                case Some(FormValuesConstants.YesValue) =>
                  Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber())
                case _ => Redirect(controllers.employments.routes.AddEmploymentController.sixWeeksError())
              }
          )
      )
  }

  def sixWeeksError(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
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

  def addEmploymentPayrollNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
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

  def submitEmploymentPayrollNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
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

  def addTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
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

  def submitTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
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

  def addEmploymentCheckYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
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
                // DONT FORGET THIS (below) should be controllers.employments.routes.AddEmploymentController.submitYourAnswers().url
                controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url,
                controllers.employments.routes.AddEmploymentController.cancel().url
              )
            Ok(addIncomeCheckYourAnswers(model))
          case Left(_) =>
            Redirect(taxAccountSummaryRedirect)
        }
  }

//  def submitYourAnswers: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
//    implicit val user: AuthedUser = request.taiUser
//    for {
//      (mandatoryVals, optionalVals) <- journeyCacheService
//                                         .collectedJourneyValues(
//                                           Seq(
//                                             AddEmploymentConstants.NameKey,
//                                             AddEmploymentConstants.StartDateKey,
//                                             AddEmploymentConstants.PayrollNumberKey,
//                                             AddEmploymentConstants.TelephoneQuestionKey
//                                           ),
//                                           Seq(AddEmploymentConstants.TelephoneNumberKey)
//                                         )
//                                         .getOrFail
//      model = AddEmployment(
//                mandatoryVals.head,
//                LocalDate.parse(mandatoryVals(1)),
//                mandatoryVals(2),
//                mandatoryVals(3),
//                optionalVals.head
//              )
//      _ <- employmentService.addEmployment(user.nino, model)
//      _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourneyConstants.AddEmploymentKey, "true")
//      _ <- journeyCacheService.flush()
//    } yield Redirect(controllers.employments.routes.AddEmploymentController.confirmation())
//  }

  def confirmation: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    Future.successful(Ok(confirmationView()))

  }

}
