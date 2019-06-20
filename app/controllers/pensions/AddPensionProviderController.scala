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

package controllers.pensions

import javax.inject.{Inject, Named}
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint._
import uk.gov.hmrc.tai.forms.pensions.{AddPensionProviderFirstPayForm, AddPensionProviderNumberForm, PensionAddDateForm, PensionProviderNameForm}
import uk.gov.hmrc.tai.model.domain.AddPensionProvider
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{PensionProviderService, _}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.{CheckYourAnswersViewModel, PensionNumberViewModel}

import scala.Function.tupled
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal

class AddPensionProviderController @Inject()(pensionProviderService: PensionProviderService,
                                             auditService: AuditService,
                                             val auditConnector: AuditConnector,
                                             authenticate: AuthAction,
                                             validatePerson: ValidatePerson,
                                             @Named("Add Pension Provider") journeyCacheService: JourneyCacheService,
                                             @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
                                             override implicit val partialRetriever: FormPartialRetriever,
                                             override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with JourneyCacheConstants
  with AuditConstants
  with FormValuesConstants {

  private def contactPhonePensionProvider(implicit messages: Messages): CanWeContactByPhoneViewModel = {
    CanWeContactByPhoneViewModel(
      messages("add.missing.pension"),
      messages("tai.canWeContactByPhone.title"),
      controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url,
      controllers.pensions.routes.AddPensionProviderController.submitTelephoneNumber().url,
      controllers.pensions.routes.AddPensionProviderController.cancel().url
    )
  }

  def cancel(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.flush() map { _ =>
          Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
      }
  }

  def addPensionProviderName(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.currentValue(AddPensionProvider_NameKey) map {
        pensionName =>
          implicit val user = request.taiUser

          Ok(views.html.pensions.addPensionName(PensionProviderNameForm.form.fill(pensionName.getOrElse(""))))
      }
  }

  def submitPensionProviderName(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      PensionProviderNameForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.pensions.addPensionName(formWithErrors)))
        },
        pensionProviderName => {
          journeyCacheService.cache(Map(AddPensionProvider_NameKey -> pensionProviderName))
            .map(_ => Redirect(controllers.pensions.routes.AddPensionProviderController.receivedFirstPay()))
        }
      )
  }

  def receivedFirstPay(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.collectedValues(Seq(AddPensionProvider_NameKey), Seq(AddPensionProvider_FirstPaymentKey)) map tupled { (mandatoryVals, optionalVals) =>
        Ok(views.html.pensions.addPensionReceivedFirstPay(AddPensionProviderFirstPayForm.form.fill(optionalVals(0)), mandatoryVals(0)))
      }
  }


  def submitFirstPay(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      AddPensionProviderFirstPayForm.form.bindFromRequest().fold(
        formWithErrors => {
          journeyCacheService.mandatoryValue(AddPensionProvider_NameKey).map { pensionProviderName =>
            BadRequest(views.html.pensions.addPensionReceivedFirstPay(formWithErrors, pensionProviderName))
          }
        },
        yesNo => {
          journeyCacheService.cache(AddPensionProvider_FirstPaymentKey, yesNo.getOrElse("")) map { _ =>
            yesNo match {
              case Some(YesValue) => {
                journeyCacheService.cache("", "")
                Redirect(controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate())
              }
              case _ => Redirect(controllers.pensions.routes.AddPensionProviderController.cantAddPension())
            }
          }
        }
      )
  }

  def cantAddPension(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.mandatoryValue(AddPensionProvider_NameKey) map { pensionProviderName =>
        auditService.createAndSendAuditEvent(AddPension_CantAddPensionProvider, Map("nino" -> request.taiUser.getNino))
        implicit val user = request.taiUser

        Ok(views.html.pensions.addPensionErrorPage(pensionProviderName))
      }
  }

  def addPensionProviderStartDate(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      (journeyCacheService.collectedValues(Seq(AddPensionProvider_NameKey), Seq(AddPensionProvider_StartDateKey)) map tupled { (mandatoryVals, optionalVals) =>

        val form = optionalVals(0) match {
          case Some(userDateString) => PensionAddDateForm(mandatoryVals(0)).form.fill(new LocalDate(userDateString))
          case _ => PensionAddDateForm(mandatoryVals(0)).form
        }

        Ok(views.html.pensions.addPensionStartDate(form, mandatoryVals(0)))
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  def submitPensionProviderStartDate(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.currentCache flatMap {
        currentCache =>
          PensionAddDateForm(currentCache(AddPensionProvider_NameKey)).form.bindFromRequest().fold(
            formWithErrors => {
              Future.successful(BadRequest(views.html.pensions.addPensionStartDate(formWithErrors, currentCache(AddPensionProvider_NameKey))))
            },
            date => {
              journeyCacheService.cache(AddPensionProvider_StartDateKey, date.toString) map { _ =>
                Redirect(controllers.pensions.routes.AddPensionProviderController.addPensionNumber())
              }
            }
          )
      }
  }

  def addPensionNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.currentCache map { cache =>
        val viewModel = PensionNumberViewModel(cache)

        val payrollNo = cache.get(AddPensionProvider_PayrollNumberChoice) match {
          case Some(YesValue) => cache.get(AddPensionProvider_PayrollNumberKey)
          case _ => None
        }

        Ok(views.html.pensions.addPensionNumber(AddPensionProviderNumberForm.form.fill(
          AddPensionProviderNumberForm(cache.get(AddPensionProvider_PayrollNumberChoice), payrollNo)),
          viewModel)
        )
      }
  }

  def submitPensionNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      AddPensionProviderNumberForm.form.bindFromRequest().fold(
        formWithErrors => {
          journeyCacheService.currentCache map { cache =>
            val viewModel = PensionNumberViewModel(cache)
            BadRequest(views.html.pensions.addPensionNumber(formWithErrors, viewModel))
          }
        },
        form => {
          val payrollNumberToCache = Map(
            AddPensionProvider_PayrollNumberChoice -> form.payrollNumberChoice.getOrElse(Messages("tai.label.no")),
            AddPensionProvider_PayrollNumberKey -> form.payrollNumberEntry.getOrElse(Messages("tai.notKnown.response")))
          journeyCacheService.cache(payrollNumberToCache).map(_ =>
            Redirect(controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber())
          )
        }
      )
  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      journeyCacheService.optionalValues(AddPensionProvider_TelephoneQuestionKey, AddPensionProvider_TelephoneNumberKey) map { seq =>
        val telephoneNo = seq(0) match {
          case Some(YesValue) => seq(1)
          case _ => None
        }

        val user = Some(request.taiUser)

        Ok(views.html.can_we_contact_by_phone(user, contactPhonePensionProvider, YesNoTextEntryForm.form().fill(YesNoTextEntryForm(seq(0), telephoneNo))))
      }
  }


  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      YesNoTextEntryForm.form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
        formWithErrors => {
          val user = Some(request.taiUser)
          Future.successful(BadRequest(views.html.can_we_contact_by_phone(user, contactPhonePensionProvider, formWithErrors)))
        },
        form => {
          val mandatoryData = Map(AddPensionProvider_TelephoneQuestionKey -> Messages(s"tai.label.${form.yesNoChoice.getOrElse(NoValue).toLowerCase}"))
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == YesValue => mandatoryData ++ Map(AddPensionProvider_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(AddPensionProvider_TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.pensions.routes.AddPensionProviderController.checkYourAnswers())
          }
        }
      )
  }

  def checkYourAnswers: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      try {
        journeyCacheService.collectedValues(
          Seq(AddPensionProvider_NameKey, AddPensionProvider_StartDateKey, AddPensionProvider_PayrollNumberKey, AddPensionProvider_TelephoneQuestionKey),
          Seq(AddPensionProvider_TelephoneNumberKey)
        ) map tupled { (mandatoryVals, optionalVals) =>

          val model = CheckYourAnswersViewModel(
            mandatoryVals.head,
            mandatoryVals(1),
            mandatoryVals(2),
            mandatoryVals(3),
            optionalVals.head
          )
          Ok(views.html.pensions.addPensionCheckYourAnswers(model))
        }
      } catch {
        case NonFatal(e) => Future.successful(internalServerError(e.getMessage))
      }
  }


  def submitYourAnswers: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      for {
        (mandatoryVals, optionalVals) <- journeyCacheService.collectedValues(
          Seq(AddPensionProvider_NameKey, AddPensionProvider_StartDateKey, AddPensionProvider_PayrollNumberKey, AddPensionProvider_TelephoneQuestionKey),
          Seq(AddPensionProvider_TelephoneNumberKey))
        model = AddPensionProvider(mandatoryVals.head, LocalDate.parse(mandatoryVals(1)), mandatoryVals(2), mandatoryVals.last, optionalVals.head)
        _ <- pensionProviderService.addPensionProvider(Nino(user.getNino), model)
        _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourney_AddPensionProviderKey, "true")
        _ <- journeyCacheService.flush()
      } yield {
        Redirect(controllers.pensions.routes.AddPensionProviderController.confirmation())
      }
  }

  def confirmation: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      Future.successful(Ok(views.html.pensions.addPensionConfirmation()))
  }

}
