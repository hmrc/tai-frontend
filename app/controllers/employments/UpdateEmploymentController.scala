/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import controllers.{AuthenticationConnectors, ServiceCheckLite, TaiBaseController}
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.employments.UpdateEmploymentDetailsForm
import uk.gov.hmrc.tai.model.domain.IncorrectIncome
import uk.gov.hmrc.tai.service.{EmploymentService, JourneyCacheService, PersonService}
import uk.gov.hmrc.tai.util.{AuditConstants, FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, UpdateEmploymentCheckYourAnswersViewModel}

import scala.Function.tupled
import scala.concurrent.Future

trait UpdateEmploymentController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants
  with AuditConstants
  with FormValuesConstants {

  def telephoneNumberSizeConstraint(implicit messages: Messages): Constraint[String] =
    Constraint[String]((textContent: String) => textContent match {
      case txt if txt.length < 8 || txt.length > 30 => Invalid(messages("tai.canWeContactByPhone.telephone.invalid"))
      case _ => Valid
    })

  def personService: PersonService

  def employmentService: EmploymentService

  def journeyCacheService: JourneyCacheService

  def successfulJourneyCacheService: JourneyCacheService

  def telephoneNumberViewModel(id: Int)(implicit messages: Messages): CanWeContactByPhoneViewModel = CanWeContactByPhoneViewModel(
    messages("tai.updateEmployment.whatDoYouWantToTellUs.preHeading"),
    messages("tai.canWeContactByPhone.title"),
    controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(id).url,
    controllers.employments.routes.UpdateEmploymentController.submitTelephoneNumber().url,
    controllers.routes.IncomeSourceSummaryController.onPageLoad(id).url
  )

  def updateEmploymentDetails(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {

            for {
              userSuppliedDetails <- journeyCacheService.currentValue(UpdateEmployment_EmploymentDetailsKey)
              employment <- employmentService.employment(Nino(user.getNino), empId)
              futureResult =
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
              result <- futureResult
            } yield result
  }

  def submitUpdateEmploymentDetails(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          UpdateEmploymentDetailsForm.form.bindFromRequest.fold(
            formWithErrors => {
              journeyCacheService.currentCache map { currentCache =>
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

  def addTelephoneNumber(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {

          for {
            employmentId <- journeyCacheService.mandatoryValueAsInt(EndEmployment_EmploymentIdKey)
            telephoneCache <- journeyCacheService.collectedOptionalValues(Seq(UpdateEmployment_TelephoneQuestionKey, UpdateEmployment_TelephoneNumberKey))

          } yield{Ok(views.html.can_we_contact_by_phone(telephoneNumberViewModel((employmentId)),
              YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache(0), telephoneCache(1)))))
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
            journeyCacheService.currentCache map { currentCache =>
              BadRequest(views.html.can_we_contact_by_phone(telephoneNumberViewModel(currentCache(UpdateEmployment_EmploymentIdKey).toInt), formWithErrors))
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

  def updateEmploymentCheckYourAnswers(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
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
  }

  def submitYourAnswers(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            for {
              (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService.collectedValues(Seq(UpdateEmployment_EmploymentIdKey,
                UpdateEmployment_EmploymentDetailsKey, UpdateEmployment_TelephoneQuestionKey),
                Seq(UpdateEmployment_TelephoneNumberKey))
              model = IncorrectIncome(mandatoryCacheSeq(1), mandatoryCacheSeq(2), optionalCacheSeq.head)
              _ <- employmentService.incorrectEmployment(Nino(user.getNino), mandatoryCacheSeq.head.toInt, model)
              _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourney_UpdateEmploymentKey, true.toString)
              _ <- journeyCacheService.flush
            } yield Redirect(controllers.employments.routes.UpdateEmploymentController.confirmation())
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
// $COVERAGE-OFF$
object UpdateEmploymentController extends UpdateEmploymentController with AuthenticationConnectors {
  override val personService: PersonService = PersonService
  override implicit val templateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
  override val employmentService = EmploymentService
  override val journeyCacheService = JourneyCacheService(UpdateEmployment_JourneyKey)
  override val successfulJourneyCacheService = JourneyCacheService(TrackSuccessfulJourney_JourneyKey)
}
// $COVERAGE-ON$
