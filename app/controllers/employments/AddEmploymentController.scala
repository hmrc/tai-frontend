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
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import javax.inject.Inject
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.{AddEmploymentFirstPayForm, AddEmploymentPayrollNumberForm, EmploymentAddDateForm, EmploymentNameForm}
import uk.gov.hmrc.tai.model.domain.AddEmployment
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.PayrollNumberViewModel
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.addIncomeCheckYourAnswers

import scala.Function.tupled
import scala.concurrent.{ExecutionContext, Future}

class AddEmploymentController @Inject()(
  auditService: AuditService,
  employmentService: EmploymentService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  @Named("Add Employment") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  val auditConnector: AuditConnector,
  mcc: MessagesControllerComponents,
  add_employment_start_date_form: add_employment_start_date_form,
  add_employment_name_form: add_employment_name_form,
  add_employment_first_pay_form: add_employment_first_pay_form,
  add_employment_error_page: add_employment_error_page,
  add_employment_payroll_number_form: add_employment_payroll_number_form,
  can_we_contact_by_phone: CanWeContactByPhoneView,
  confirmationView: confirmation,
  addIncomeCheckYourAnswers: addIncomeCheckYourAnswers,
  implicit val partialRetriever: FormPartialRetriever,
  implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with AuditConstants with FormValuesConstants
    with EmptyCacheRedirect {

  def cancel(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
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

  def addEmploymentName(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.currentValue(AddEmployment_NameKey) map { providedName =>
      implicit val user: AuthedUser = request.taiUser

      Ok(add_employment_name_form(EmploymentNameForm.form.fill(providedName.getOrElse(""))))
    }
  }

  def submitEmploymentName(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    EmploymentNameForm.form.bindFromRequest.fold(
      formWithErrors => {
        implicit val user: AuthedUser = request.taiUser

        Future.successful(BadRequest(add_employment_name_form(formWithErrors)))
      },
      employmentName => {
        journeyCacheService
          .cache(Map(AddEmployment_NameKey -> employmentName))
          .map(_ => Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentStartDate()))
      }
    )
  }

  def addEmploymentStartDate(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.collectedJourneyValues(Seq(AddEmployment_NameKey), Seq(AddEmployment_StartDateKey)) map tupled {
      (mandSeq, optSeq) =>
        mandSeq match {
          case Right(mandatorySequence) =>
            val form = optSeq.head match {
              case Some(dateString) =>
                EmploymentAddDateForm(mandatorySequence.head).form.fill(new LocalDate(dateString))
              case _ => EmploymentAddDateForm(mandatorySequence.head).form
            }
            implicit val user: AuthedUser = request.taiUser

            Ok(add_employment_start_date_form(form, mandatorySequence.head))
          case Left(_) => Redirect(taxAccountSummaryRedirect)
        }
    }
  }

  def submitEmploymentStartDate(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.currentCache map { currentCache =>
        EmploymentAddDateForm(currentCache(AddEmployment_NameKey)).form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              implicit val user: AuthedUser = request.taiUser

              BadRequest(add_employment_start_date_form(formWithErrors, currentCache(AddEmployment_NameKey)))
            },
            date => {
              val startDateBoundary = new LocalDate().minusWeeks(6)
              val data = currentCache + (AddEmployment_StartDateKey -> date.toString)
              if (date.isAfter(startDateBoundary)) {
                val firstPayChoiceCacheData = data + (AddEmployment_StartDateWithinSixWeeks -> YesValue)
                journeyCacheService.cache(firstPayChoiceCacheData)
                Redirect(controllers.employments.routes.AddEmploymentController.receivedFirstPay())
              } else {
                val firstPayChoiceCacheData = data + (AddEmployment_StartDateWithinSixWeeks -> NoValue)
                journeyCacheService.cache(firstPayChoiceCacheData)
                Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber())
              }
            }
          )
      }
  }

  def receivedFirstPay(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService
      .collectedValues(Seq(AddEmployment_NameKey), Seq(AddEmployment_RecewivedFirstPayKey)) map tupled {
      (mandSeq, optSeq) =>
        implicit val user: AuthedUser = request.taiUser
        Ok(add_employment_first_pay_form(AddEmploymentFirstPayForm.form.fill(optSeq.head), mandSeq.head))
    }
  }

  def submitFirstPay(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    AddEmploymentFirstPayForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          journeyCacheService.mandatoryValue(AddEmployment_NameKey).map { employmentName =>
            implicit val user: AuthedUser = request.taiUser
            BadRequest(add_employment_first_pay_form(formWithErrors, employmentName))
          }
        },
        firstPayYesNo => {
          journeyCacheService.cache(AddEmployment_RecewivedFirstPayKey, firstPayYesNo.getOrElse("")) map { _ =>
            firstPayYesNo match {
              case Some(YesValue) =>
                Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber())
              case _ => Redirect(controllers.employments.routes.AddEmploymentController.sixWeeksError())
            }
          }
        }
      )
  }

  def sixWeeksError(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.mandatoryValue(AddEmployment_NameKey) map { employmentName =>
      implicit val user: AuthedUser = request.taiUser
      auditService.createAndSendAuditEvent(AddEmployment_CantAddEmployer, Map("nino" -> user.nino.toString()))
      Ok(add_employment_error_page(employmentName))
    }
  }

  def addEmploymentPayrollNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.currentCache map { cache =>
        val viewModel = PayrollNumberViewModel(cache)
        val payrollChoice = cache.get(AddEmployment_PayrollNumberQuestionKey)
        val payroll = payrollChoice match {
          case Some(YesValue) => cache.get(AddEmployment_PayrollNumberKey)
          case _              => None
        }
        implicit val user: AuthedUser = request.taiUser

        Ok(
          add_employment_payroll_number_form(
            AddEmploymentPayrollNumberForm.form.fill(AddEmploymentPayrollNumberForm(payrollChoice, payroll)),
            viewModel))
      }
  }

  def submitEmploymentPayrollNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      AddEmploymentPayrollNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            journeyCacheService.currentCache map { cache =>
              val viewModel = PayrollNumberViewModel(cache)
              implicit val user: AuthedUser = request.taiUser

              BadRequest(add_employment_payroll_number_form(formWithErrors, viewModel))
            }
          },
          form => {
            val payrollNumberToCache = Map(
              AddEmployment_PayrollNumberQuestionKey -> form.payrollNumberChoice.getOrElse(""),
              AddEmployment_PayrollNumberKey -> form.payrollNumberEntry.getOrElse(
                Messages("tai.addEmployment.employmentPayrollNumber.notKnown"))
            )
            journeyCacheService
              .cache(payrollNumberToCache)
              .map(_ => Redirect(controllers.employments.routes.AddEmploymentController.addTelephoneNumber()))
          }
        )
  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.optionalValues(AddEmployment_TelephoneQuestionKey, AddEmployment_TelephoneNumberKey) map {
      optSeq =>
        val telNoToDisplay = optSeq.head match {
          case Some(YesValue) => optSeq(1)
          case _              => None
        }
        implicit val user: AuthedUser = request.taiUser

        Ok(
          can_we_contact_by_phone(
            Some(user),
            telephoneNumberViewModel,
            YesNoTextEntryForm.form().fill(YesNoTextEntryForm(optSeq.head, telNoToDisplay))))
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
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

          Future.successful(BadRequest(can_we_contact_by_phone(Some(user), telephoneNumberViewModel, formWithErrors)))
        },
        form => {
          val mandatoryData = Map(
            AddEmployment_TelephoneQuestionKey -> Messages(
              s"tai.label.${form.yesNoChoice.getOrElse(NoValue).toLowerCase}"))
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == YesValue =>
              mandatoryData ++ Map(AddEmployment_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(AddEmployment_TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentCheckYourAnswers())
          }
        }
      )
  }

  def addEmploymentCheckYourAnswers: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.collectedJourneyValues(
        Seq(
          AddEmployment_NameKey,
          AddEmployment_StartDateKey,
          AddEmployment_PayrollNumberKey,
          AddEmployment_TelephoneQuestionKey),
        Seq(AddEmployment_TelephoneNumberKey)
      ) map tupled { (mandatoryVals, optionalVals) =>
        mandatoryVals match {
          case Right(mandatoryValues) => {
            val model =
              IncomeCheckYourAnswersViewModel(
                Messages("add.missing.employment"),
                mandatoryValues.head,
                mandatoryValues(1),
                mandatoryValues(2),
                mandatoryValues(3),
                optionalVals.head,
                controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url,
                controllers.employments.routes.AddEmploymentController.submitYourAnswers().url,
                controllers.employments.routes.AddEmploymentController.cancel().url
              )
            implicit val user: AuthedUser = request.taiUser
            Ok(addIncomeCheckYourAnswers(model))
          }
          case Left(_) => Redirect(taxAccountSummaryRedirect)
        }
      }
  }

  def submitYourAnswers: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    for {
      (mandatoryVals, optionalVals) <- journeyCacheService.collectedValues(
                                        Seq(
                                          AddEmployment_NameKey,
                                          AddEmployment_StartDateKey,
                                          AddEmployment_PayrollNumberKey,
                                          AddEmployment_TelephoneQuestionKey),
                                        Seq(AddEmployment_TelephoneNumberKey)
                                      )
      model = AddEmployment(
        mandatoryVals.head,
        LocalDate.parse(mandatoryVals(1)),
        mandatoryVals(2),
        mandatoryVals(3),
        optionalVals.head)
      _ <- employmentService.addEmployment(user.nino, model)
      _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourney_AddEmploymentKey, "true")
      _ <- journeyCacheService.flush()
    } yield {
      Redirect(controllers.employments.routes.AddEmploymentController.confirmation())
    }
  }

  def confirmation: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    Future.successful(Ok(confirmationView()))

  }

}
