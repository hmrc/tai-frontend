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

import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import controllers.{AuthenticationConnectors, ServiceCheckLite, TaiBaseController}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint._
import uk.gov.hmrc.tai.forms.pensions.{AddPensionProviderFirstPayForm, AddPensionProviderNumberForm, PensionAddDateForm, PensionProviderNameForm}
import uk.gov.hmrc.tai.model.domain.AddPensionProvider
import uk.gov.hmrc.tai.service.{PensionProviderService, _}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.{CheckYourAnswersViewModel, PensionNumberViewModel}

import scala.Function.tupled
import scala.concurrent.Future
import scala.language.postfixOps

trait AddPensionProviderController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants
  with AuditConstants
  with FormValuesConstants {

  def personService: PersonService
  def auditService: AuditService
  def journeyCacheService: JourneyCacheService
  def successfulJourneyCacheService: JourneyCacheService
  def pensionProviderService: PensionProviderService

  private def contactPhonePensionProvider(implicit messages: Messages): CanWeContactByPhoneViewModel = {
    CanWeContactByPhoneViewModel(
      messages("add.missing.pension"),
      messages("tai.canWeContactByPhone.title"),
      controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url,
      controllers.pensions.routes.AddPensionProviderController.submitTelephoneNumber().url,
      controllers.routes.TaxAccountSummaryController.onPageLoad().url
    )
  }

  def addPensionProviderName(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.currentValue(AddPensionProvider_NameKey) map {
            pensionName =>
              Ok(views.html.pensions.addPensionName(PensionProviderNameForm.form.fill(pensionName.getOrElse(""))))
          }
        }
  }

  def submitPensionProviderName(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
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

  def receivedFirstPay(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {

          journeyCacheService.collectedValues(Seq(AddPensionProvider_NameKey), Seq(AddPensionProvider_FirstPaymentKey)) map tupled { (mandatoryVals, optionalVals) =>

            Ok(views.html.pensions.addPensionReceivedFirstPay(AddPensionProviderFirstPayForm.form.fill(optionalVals(0)), mandatoryVals(0)))
          }
          }
        }


  def submitFirstPay(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        AddPensionProviderFirstPayForm.form.bindFromRequest().fold(
          formWithErrors => {
            journeyCacheService.mandatoryValue(AddPensionProvider_NameKey).map { pensionProviderName =>
              BadRequest(views.html.pensions.addPensionReceivedFirstPay(formWithErrors, pensionProviderName))
            }
          },
          yesNo => {
            journeyCacheService.cache(AddPensionProvider_FirstPaymentKey,yesNo.getOrElse("")) map { _ =>
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

  def cantAddPension(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.mandatoryValue(AddPensionProvider_NameKey) map { pensionProviderName =>
            auditService.createAndSendAuditEvent(AddPension_CantAddPensionProvider, Map("nino" -> user.getNino))
            Ok(views.html.pensions.addPensionErrorPage(pensionProviderName))
          }
        }
  }

  def addPensionProviderStartDate(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {

          journeyCacheService.collectedValues(Seq(AddPensionProvider_NameKey), Seq(AddPensionProvider_StartDateKey)) map tupled { (mandatoryVals, optionalVals) =>
            val form = optionalVals(0) match {
              case Some(userDateString) => PensionAddDateForm(mandatoryVals(0)).form.fill(new LocalDate(userDateString))
              case _ => PensionAddDateForm(mandatoryVals(0)).form
            }
            Ok(views.html.pensions.addPensionStartDate(form, mandatoryVals(0)))
          }
        }
  }

  def submitPensionProviderStartDate(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
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

  def addPensionNumber(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
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
  }

  def submitPensionNumber(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
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

    def addTelephoneNumber(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {

            journeyCacheService.optionalValues(AddPensionProvider_TelephoneQuestionKey,AddPensionProvider_TelephoneNumberKey) map { seq =>


              val telephoneNo = seq(0) match {
                case Some(YesValue) => seq(1)
                case _ => None
              }

              Ok(views.html.can_we_contact_by_phone(contactPhonePensionProvider, YesNoTextEntryForm.form().fill(YesNoTextEntryForm(seq(0), telephoneNo))))
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
            Future.successful(BadRequest(views.html.can_we_contact_by_phone(contactPhonePensionProvider, formWithErrors)))
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

  def checkYourAnswers: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {

            journeyCacheService.collectedValues(
              Seq(AddPensionProvider_NameKey,AddPensionProvider_StartDateKey,AddPensionProvider_PayrollNumberKey, AddPensionProvider_TelephoneQuestionKey),
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
          }
  }


  def submitYourAnswers: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          for {
            (mandatoryVals, optionalVals) <- journeyCacheService.collectedValues(
              Seq(AddPensionProvider_NameKey,AddPensionProvider_StartDateKey,AddPensionProvider_PayrollNumberKey, AddPensionProvider_TelephoneQuestionKey),
              Seq(AddPensionProvider_TelephoneNumberKey))
            model = AddPensionProvider(mandatoryVals.head, LocalDate.parse(mandatoryVals(1)), mandatoryVals(2), mandatoryVals.last, optionalVals.head)
            _ <- pensionProviderService.addPensionProvider(Nino(user.getNino), model)
            _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourney_AddPensionProviderKey, "true")
            _ <- journeyCacheService.flush()
          } yield {
            Redirect(controllers.pensions.routes.AddPensionProviderController.confirmation())
          }
  }

  def confirmation: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.pensions.addPensionConfirmation()))
          }
  }

}

// $COVERAGE-OFF$
object AddPensionProviderController extends AddPensionProviderController with AuthenticationConnectors {
  override val personService: PersonService = PersonService
  override val auditService: AuditService = AuditService
  override implicit val templateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
  override val pensionProviderService: PensionProviderService = PensionProviderService
  override val journeyCacheService = JourneyCacheService(AddPensionProvider_JourneyKey)
  override val successfulJourneyCacheService = JourneyCacheService(TrackSuccessfulJourney_JourneyKey)
}
// $COVERAGE-ON$
