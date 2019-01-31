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
import controllers.audit.Auditable
import controllers.auth.{AuthedUser, WithAuthorisedForTaiLite}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.employments.{DuplicateSubmissionWarningForm, EmploymentEndDateForm, IrregularPayForm, UpdateRemoveEmploymentForm}
import uk.gov.hmrc.tai.model.domain.EndEmployment
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService, PersonService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, IrregularPayConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, WithinSixWeeksViewModel}
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel

import scala.Function.tupled
import scala.concurrent.Future

class EndEmploymentController @Inject()(personService: PersonService,
                                        auditService: AuditService,
                                        employmentService: EmploymentService,
                                        @Named("End Employment") journeyCacheService: JourneyCacheService,
                                        @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
                                        val delegationConnector: DelegationConnector,
                                        val authConnector: AuthConnector,
                                        val auditConnector: AuditConnector,
                                        implicit val templateRenderer: TemplateRenderer,
                                        implicit val partialRetriever: FormPartialRetriever
                                       ) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants
  with FormValuesConstants
  with IrregularPayConstants
  with AuditConstants {

  private def telephoneNumberViewModel(employmentId: Int)(implicit messages: Messages) = CanWeContactByPhoneViewModel(
    messages("tai.endEmployment.preHeadingText"),
    messages("tai.canWeContactByPhone.title"),
    controllers.employments.routes.EndEmploymentController.endEmploymentPage(employmentId).url,
    controllers.employments.routes.EndEmploymentController.submitTelephoneNumber().url,
    controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId).url)

  private def telephoneNumberSizeConstraint(implicit messages: Messages): Constraint[String] =
    Constraint[String]((textContent: String) => textContent match {
      case txt if txt.length < 8 || txt.length > 30 => Invalid(messages("tai.canWeContactByPhone.telephone.invalid"))
      case _ => Valid
    })

  def employmentUpdateRemove(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.getNino)
            employmentService.employment(nino, empId).map {
              case Some(employment) =>
                Ok(views.html.employments.update_remove_employment_decision(
                  updateRemoveForm = UpdateRemoveEmploymentForm.form,
                  employmentName = employment.name,
                  empId = empId))
              case _ => throw new RuntimeException("No employment found")
            }
          }
  }

  def handleEmploymentUpdateRemove(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.getNino)
            employmentService.employment(nino, empId) flatMap  {
              case Some(employment) =>
                UpdateRemoveEmploymentForm.form.bindFromRequest.fold(
                  formWithErrors => {
                    Future(BadRequest(views.html.employments.update_remove_employment_decision(formWithErrors, employment.name, empId)))
                  },
                  {
                    case Some(YesValue) => Future(Redirect(controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(empId)))

                    case _ => {
                      val today = new LocalDate()
                      val latestPaymentDate: Option[LocalDate] = for {
                        latestAnnualAccount <- employment.latestAnnualAccount
                        latestPayment <- latestAnnualAccount.latestPayment
                      } yield latestPayment.date

                      val hasIrregularPayment = employment.latestAnnualAccount.exists(_.isIrregularPayment)
                      if (latestPaymentDate.isDefined && latestPaymentDate.get.isAfter(today.minusWeeks(6).minusDays(1))) {

                        val errorPagecache = Map(EndEmployment_LatestPaymentDateKey -> latestPaymentDate.get.toString,
                          EndEmployment_NameKey -> employment.name, EndEmployment_EmploymentIdKey -> empId.toString)
                        journeyCacheService.cache(errorPagecache) map { _ =>
                          auditService.createAndSendAuditEvent(EndEmployment_WithinSixWeeksError, Map("nino" -> nino.nino))
                          Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentError())
                        }
                      } else if (hasIrregularPayment) {
                        val errorPagecache = Map(EndEmployment_NameKey -> employment.name)
                        journeyCacheService.cache(errorPagecache) map { _ =>
                          auditService.createAndSendAuditEvent(EndEmployment_IrregularPayment, Map("nino" -> nino.nino))
                          Redirect(controllers.employments.routes.EndEmploymentController.irregularPaymentError(empId))
                        }
                      } else {
                        Future(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage(empId)))
                      }
                    }
                  }
                )
              case _ => throw new RuntimeException("No employment found")
            }
          }

  }

  def endEmploymentError(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.mandatoryValues(EndEmployment_LatestPaymentDateKey, EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map { data =>
              val date = new LocalDate(data.head)
              Ok(views.html.employments.endEmploymentWithinSixWeeksError(WithinSixWeeksViewModel(date.plusWeeks(6).plusDays(1), data(1), date, data(2).toInt)))
            }
          }

  }

  def irregularPaymentError(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.mandatoryValue(EndEmployment_NameKey) map { name =>
              Ok(views.html.employments.EndEmploymentIrregularPaymentError(IrregularPayForm.createForm, EmploymentViewModel(name, empId)))
            }
          }

  }


  def handleIrregularPaymentError(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            IrregularPayForm.createForm.bindFromRequest.fold(
              formWithErrors => {
                journeyCacheService.mandatoryValue(EndEmployment_NameKey) map { name =>
                  BadRequest(views.html.employments.EndEmploymentIrregularPaymentError(formWithErrors, EmploymentViewModel(name, empId)))
                }
              },
              formData => {
                formData.irregularPayDecision match {
                  case Some(ContactEmployer) =>
                    Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
                  case _ =>
                    Future.successful(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage(empId)))
                }
              }
            )
          }
  }

  def endEmploymentPage(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          val nino = Nino(user.getNino)
          ServiceCheckLite.personDetailsCheck {
            for{
              employment <- employmentService.employment(nino, employmentId)
              cache <- journeyCacheService.currentValueAsDate(EndEmployment_EndDateKey)
            }yield (employment, cache) match{
              case (Some(employment),Some(cache)) =>
                Ok(views.html.employments.endEmployment(EmploymentEndDateForm(employment.name).form.fill(cache), EmploymentViewModel(employment.name, employmentId)))
              case (Some(employment),None) =>
                Ok(views.html.employments.endEmployment(EmploymentEndDateForm(employment.name).form, EmploymentViewModel(employment.name, employmentId)))
              case _ => throw new RuntimeException("No employment found")
            }


          }
  }

  def handleEndEmploymentPage(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          val nino = Nino(user.getNino)
          ServiceCheckLite.personDetailsCheck {
            employmentService.employment(nino, employmentId) flatMap {
              case Some(employment) =>
                EmploymentEndDateForm(employment.name).form.bindFromRequest.fold(
                  formWithErrors => {
                    Future.successful(BadRequest(views.html.employments.endEmployment(formWithErrors, EmploymentViewModel(employment.name, employmentId))))
                  },
                  date => {
                    val employmentJourneyCacheData = Map(EndEmployment_EmploymentIdKey -> employmentId.toString,
                      EndEmployment_NameKey -> employment.name,
                      EndEmployment_EndDateKey -> date.toString)
                    journeyCacheService.cache(employmentJourneyCacheData) map { _ =>
                      Redirect(controllers.employments.routes.EndEmploymentController.addTelephoneNumber())
                    }
                  }
                )
              case _ =>
                throw new RuntimeException("No employment found")
            }
          }
  }

  def addTelephoneNumber(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            for {
              employmentId <- journeyCacheService.mandatoryValueAsInt(EndEmployment_EmploymentIdKey)
              telephoneCache <- journeyCacheService.optionalValues(EndEmployment_TelephoneQuestionKey,EndEmployment_TelephoneNumberKey)
            } yield {
              Ok(views.html.can_we_contact_by_phone(None, Some(user), telephoneNumberViewModel(employmentId),
                YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache(0), telephoneCache(1)))))
            }
          }
  }

  def submitTelephoneNumber(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            YesNoTextEntryForm.form(
              Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
              Messages("tai.canWeContactByPhone.telephone.empty"),
              Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
              formWithErrors => {
                journeyCacheService.mandatoryValueAsInt(EndEmployment_EmploymentIdKey) map { employmentId =>
                  BadRequest(views.html.can_we_contact_by_phone(None, Some(user), telephoneNumberViewModel(employmentId), formWithErrors))
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
  }

  def endEmploymentCheckYourAnswers: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.collectedValues(Seq(EndEmployment_EmploymentIdKey,
              EndEmployment_EndDateKey, EndEmployment_TelephoneQuestionKey),
              Seq(EndEmployment_TelephoneNumberKey)) map tupled { (mandatorySeq, optionalSeq) =>
              val model = IncomeCheckYourAnswersViewModel(mandatorySeq(0).toInt, Messages("tai.endEmployment.preHeadingText"),
                mandatorySeq(1), mandatorySeq(2), optionalSeq(0),
                controllers.employments.routes.EndEmploymentController.addTelephoneNumber.url,
                controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment.url,
                controllers.routes.IncomeSourceSummaryController.onPageLoad(mandatorySeq(0).toInt).url)
              Ok(views.html.incomes.addIncomeCheckYourAnswers(model))
            }
          }
  }


  def confirmAndSendEndEmployment: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          val nino = Nino(user.getNino)
          ServiceCheckLite.personDetailsCheck {
            for {
              (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService.collectedValues(Seq(EndEmployment_EmploymentIdKey, EndEmployment_EndDateKey,
                EndEmployment_TelephoneQuestionKey), Seq(EndEmployment_TelephoneNumberKey))
              model = EndEmployment(LocalDate.parse(mandatoryCacheSeq(1)),mandatoryCacheSeq(2),optionalCacheSeq(0))
              _ <- employmentService.endEmployment(nino, mandatoryCacheSeq(0).toInt, model)
              _ <- successfulJourneyCacheService.cache(Map(s"EndEmploymentID-${mandatoryCacheSeq.head}" -> "true"))
              _ <- journeyCacheService.flush
            } yield Redirect(routes.EndEmploymentController.showConfirmationPage())
          }
  }

  def redirectUpdateEmployment(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.getNino)
            employmentService.employment(nino, empId) flatMap {
              case Some(employment) => {
                val journeyCacheFuture = journeyCacheService.
                  cache(Map(EndEmployment_EmploymentIdKey -> empId.toString, EndEmployment_NameKey -> employment.name))
                val successfullJourneyCacheFuture = successfulJourneyCacheService.currentValue(s"EndEmploymentID-${empId}")
                for {
                  _ <- journeyCacheFuture
                  successfulJourneyCache <- successfullJourneyCacheFuture
                } yield {
                  successfulJourneyCache match {
                    case Some(_) => Redirect(routes.EndEmploymentController.duplicateSubmissionWarning)
                    case _ => Redirect(routes.EndEmploymentController.employmentUpdateRemove(empId))
                  }
                }
              }
              case _ => throw new RuntimeException("No employment found")
            }
          }
  }

  def duplicateSubmissionWarning: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) map { mandatoryValues =>
              Ok(views.html.employments.duplicateSubmissionWarning(DuplicateSubmissionWarningForm.createForm, mandatoryValues(0), mandatoryValues(1).toInt))
            }
          }
  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.mandatoryValues(EndEmployment_NameKey, EndEmployment_EmploymentIdKey) flatMap { mandatoryValues =>
              DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(views.html.employments.
                    duplicateSubmissionWarning(formWithErrors, mandatoryValues(0), mandatoryValues(1).toInt)))
                },
                success => {
                  success.yesNoChoice match {
                    case Some(YesValue) => Future.successful(Redirect(controllers.employments.routes.EndEmploymentController.
                      employmentUpdateRemove(mandatoryValues(1).toInt)))
                    case Some(NoValue) => Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.
                      onPageLoad(mandatoryValues(1).toInt)))
                  }
                }
              )
            }
          }
  }

  def showConfirmationPage: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request => Future.successful(Ok(views.html.employments.confirmation()))
  }
}