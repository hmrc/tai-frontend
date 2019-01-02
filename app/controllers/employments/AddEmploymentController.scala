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
import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import controllers.{ServiceCheckLite, TaiBaseController}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.employments.{AddEmploymentFirstPayForm, AddEmploymentPayrollNumberForm, EmploymentAddDateForm, EmploymentNameForm}
import uk.gov.hmrc.tai.model.domain.AddEmployment
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService, PersonService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.PayrollNumberViewModel
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel

import scala.Function.tupled
import scala.concurrent.Future

class AddEmploymentController @Inject()(personService: PersonService,
                                        auditService: AuditService,
                                        employmentService: EmploymentService,
                                        @Named("Add Employment") val journeyCacheService: JourneyCacheService,
                                        @Named("Track Successful Journey") val successfulJourneyCacheService: JourneyCacheService,
                                        val delegationConnector: DelegationConnector,
                                        val auditConnector: AuditConnector,
                                        val authConnector: AuthConnector,
                                        override implicit val partialRetriever: FormPartialRetriever,
                                        override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants
  with AuditConstants
  with FormValuesConstants {



  def telephoneNumberViewModel(implicit messages: Messages): CanWeContactByPhoneViewModel = CanWeContactByPhoneViewModel(
    messages("add.missing.employment"),
    messages("tai.canWeContactByPhone.title"),
    controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber().url,
    controllers.employments.routes.AddEmploymentController.submitTelephoneNumber().url,
    controllers.routes.TaxAccountSummaryController.onPageLoad().url
  )

  def telephoneNumberSizeConstraint(implicit messages: Messages): Constraint[String] =
    Constraint[String]((textContent: String) => textContent match {
      case txt if txt.length < 8 || txt.length > 30 => Invalid(messages("tai.canWeContactByPhone.telephone.invalid"))
      case _ => Valid
    })

  def addEmploymentName(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.currentValue(AddEmployment_NameKey) map { providedName =>
            Ok(views.html.employments.add_employment_name_form(EmploymentNameForm.form.fill(providedName.getOrElse(""))))
          }
        }
  }

  def submitEmploymentName(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        EmploymentNameForm.form.bindFromRequest.fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.employments.add_employment_name_form(formWithErrors)))
          },
          employmentName => {
            journeyCacheService.cache(Map(AddEmployment_NameKey -> employmentName))
              .map(_ => Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentStartDate()))
          }
        )
  }

  def addEmploymentStartDate(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.collectedValues(Seq(AddEmployment_NameKey), Seq(AddEmployment_StartDateKey)) map tupled { (mandSeq, optSeq) =>

            val form = optSeq(0) match {
              case Some(dateString) => EmploymentAddDateForm(mandSeq(0)).form.fill(new LocalDate(dateString))
              case _ => EmploymentAddDateForm(mandSeq(0)).form
            }
            Ok(views.html.employments.add_employment_start_date_form(form, mandSeq(0)))
          }
        }
  }

  def submitEmploymentStartDate(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        journeyCacheService.currentCache map {
          currentCache =>
            EmploymentAddDateForm(currentCache(AddEmployment_NameKey)).form.bindFromRequest().fold(
              formWithErrors => {
                BadRequest(views.html.employments.add_employment_start_date_form(formWithErrors, currentCache(AddEmployment_NameKey)))
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

  def receivedFirstPay(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.collectedValues(Seq(AddEmployment_NameKey), Seq(AddEmployment_RecewivedFirstPayKey)) map tupled { (mandSeq, optSeq)  =>
            Ok(views.html.employments.add_employment_first_pay_form(AddEmploymentFirstPayForm.form.fill(optSeq(0)), mandSeq(0)))
          }
        }
  }

  def submitFirstPay(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        AddEmploymentFirstPayForm.form.bindFromRequest().fold(
          formWithErrors => {
            journeyCacheService.mandatoryValue(AddEmployment_NameKey).map { employmentName =>
              BadRequest(views.html.employments.add_employment_first_pay_form(formWithErrors, employmentName))
            }
          },
          firstPayYesNo => {
            journeyCacheService.cache(AddEmployment_RecewivedFirstPayKey, firstPayYesNo.getOrElse("")) map { _ =>
              firstPayYesNo match {
                case Some(YesValue) => Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber())
                case _ => Redirect(controllers.employments.routes.AddEmploymentController.sixWeeksError())
              }
            }
          }
        )
  }

  def sixWeeksError(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.mandatoryValue(AddEmployment_NameKey) map { employmentName =>
            auditService.createAndSendAuditEvent(AddEmployment_CantAddEmployer, Map("nino" -> user.getNino))
            Ok(views.html.employments.add_employment_error_page(employmentName))
          }
        }
  }

  def addEmploymentPayrollNumber(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.currentCache map { cache =>

            val viewModel = PayrollNumberViewModel(cache)
            val payrollChoice = cache.get(AddEmployment_PayrollNumberQuestionKey)
            val payroll = payrollChoice match {
              case Some(YesValue) => cache.get(AddEmployment_PayrollNumberKey)
              case _ => None
            }

            Ok(views.html.employments.add_employment_payroll_number_form(
              AddEmploymentPayrollNumberForm.form.fill(AddEmploymentPayrollNumberForm(payrollChoice, payroll)),
              viewModel))
          }
        }
  }

  def submitEmploymentPayrollNumber(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        AddEmploymentPayrollNumberForm.form.bindFromRequest().fold(
          formWithErrors => {
            journeyCacheService.currentCache map { cache =>
              val viewModel = PayrollNumberViewModel(cache)
              BadRequest(views.html.employments.add_employment_payroll_number_form(formWithErrors, viewModel))
            }
          },
          form => {
            val payrollNumberToCache = Map(
              AddEmployment_PayrollNumberQuestionKey -> form.payrollNumberChoice.getOrElse(""),
              AddEmployment_PayrollNumberKey -> form.payrollNumberEntry.getOrElse(Messages("tai.addEmployment.employmentPayrollNumber.notKnown"))
            )
            journeyCacheService.cache(payrollNumberToCache).map(_ =>
              Redirect(controllers.employments.routes.AddEmploymentController.addTelephoneNumber())
            )
          }
        )
  }

  def addTelephoneNumber(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.optionalValues(AddEmployment_TelephoneQuestionKey, AddEmployment_TelephoneNumberKey) map { optSeq =>

            val telNoToDisplay = optSeq(0) match {
              case Some(YesValue) => optSeq(1)
              case _ => None
            }
            Ok(views.html.can_we_contact_by_phone(
              telephoneNumberViewModel,
              YesNoTextEntryForm.form().fill(YesNoTextEntryForm(optSeq(0), telNoToDisplay))
            ))
          }
        }
  }

  def submitTelephoneNumber(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        YesNoTextEntryForm.form(
          Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
          Messages("tai.canWeContactByPhone.telephone.empty"),
          Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
          formWithErrors => {
              Future.successful(BadRequest(views.html.can_we_contact_by_phone(telephoneNumberViewModel, formWithErrors)))
          },
          form => {
            val mandatoryData = Map(AddEmployment_TelephoneQuestionKey -> Messages(s"tai.label.${form.yesNoChoice.getOrElse(NoValue).toLowerCase}"))
            val dataForCache = form.yesNoChoice match {
              case Some(yn) if yn==YesValue => mandatoryData ++ Map(AddEmployment_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
              case _ => mandatoryData ++ Map(AddEmployment_TelephoneNumberKey -> "")
            }
            journeyCacheService.cache(dataForCache) map { _ =>
              Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentCheckYourAnswers())
            }
          }
        )
  }

  def addEmploymentCheckYourAnswers: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {

            journeyCacheService.collectedValues(
              Seq(AddEmployment_NameKey,AddEmployment_StartDateKey,AddEmployment_PayrollNumberKey, AddEmployment_TelephoneQuestionKey),
              Seq(AddEmployment_TelephoneNumberKey)
            ) map tupled { (mandatoryVals, optionalVals) =>
              val model =
                IncomeCheckYourAnswersViewModel(Messages("add.missing.employment"), mandatoryVals.head, mandatoryVals(1), mandatoryVals(2), mandatoryVals(3), optionalVals.head,
                  controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url,
                  controllers.employments.routes.AddEmploymentController.submitYourAnswers().url,
                  controllers.routes.TaxAccountSummaryController.onPageLoad().url)
              Ok(views.html.incomes.addIncomeCheckYourAnswers(model))
            }
          }
  }

  def submitYourAnswers: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          for {
            (mandatoryVals, optionalVals) <- journeyCacheService.collectedValues(Seq(AddEmployment_NameKey, AddEmployment_StartDateKey, AddEmployment_PayrollNumberKey, AddEmployment_TelephoneQuestionKey), Seq(AddEmployment_TelephoneNumberKey))
            model = AddEmployment(mandatoryVals.head, LocalDate.parse(mandatoryVals(1)), mandatoryVals(2), mandatoryVals(3), optionalVals.head)
            _ <- employmentService.addEmployment(Nino(user.getNino), model)
            _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourney_AddEmploymentKey, "true")
            _ <- journeyCacheService.flush()
          } yield {
            Redirect(controllers.employments.routes.AddEmploymentController.confirmation())
          }
  }

  def confirmation: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.employments.confirmation()))
          }
  }
}