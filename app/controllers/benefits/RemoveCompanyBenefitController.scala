/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.benefits

import controllers.TaiBaseController
import controllers.auth.{AuthJourney, AuthedUser}
import pages.BenefitDecisionPage
import pages.benefits._
import play.api.i18n.Messages
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.benefits.{CompanyBenefitTotalValueForm, RemoveCompanyBenefitStopDateForm}
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.model.domain.benefits.EndedCompanyBenefit
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.{ThreeWeeks, TrackingService}
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.constants.TaiConstants.TaxDateWordMonthFormat
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.benefit.{BenefitViewModel, RemoveCompanyBenefitsCheckYourAnswersViewModel}
import views.html.CanWeContactByPhoneView
import views.html.benefits._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

class RemoveCompanyBenefitController @Inject() (
  benefitsService: BenefitsService,
  trackingService: TrackingService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  removeCompanyBenefitCheckYourAnswers: RemoveCompanyBenefitCheckYourAnswersView,
  removeCompanyBenefitStopDate: RemoveCompanyBenefitStopDateView,
  removeBenefitTotalValue: RemoveBenefitTotalValueView,
  canWeContactByPhone: CanWeContactByPhoneView,
  removeCompanyBenefitConfirmation: RemoveCompanyBenefitConfirmationView,
  journeyCacheRepository: JourneyCacheRepository
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def stopDate: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val userAnswers               = request.userAnswers
    val benefitNameOption         = userAnswers.get(EndCompanyBenefitsNamePage)
    val employmentNameOption      = userAnswers.get(EndCompanyBenefitsEmploymentNamePage)

    (benefitNameOption, employmentNameOption) match {
      case (Some(benefitName), Some(employmentName)) =>
        val currentBenefitName       = benefitName
        val currentEmploymentName    = employmentName
        val removeCompanyBenefitForm = RemoveCompanyBenefitStopDateForm(currentBenefitName, currentEmploymentName)

        val form = userAnswers
          .get(EndCompanyBenefitsStopDatePage)
          .map(dateString => removeCompanyBenefitForm.form.fill(LocalDate.parse(dateString)))
          .getOrElse(removeCompanyBenefitForm.form)

        Future.successful {
          Ok(
            removeCompanyBenefitStopDate(
              form,
              benefitName,
              employmentName
            )
          )
        }
      case _                                         => throw new Exception("Benefit name or employment name not found")
    }
  }

  private def checkDate(
    date: LocalDate,
    userAnswers: UserAnswers,
    user: AuthedUser,
    taxYear: TaxYear
  ): Future[Result] = {
    val dateString = date.toString
    if (date isBefore taxYear.start) {
      for {
        _ <- journeyCacheRepository.clear(userAnswers.sessionId, user.nino.nino)
        _ <- {
          val updatedData        = userAnswers.data - EndCompanyBenefitsValuePage.toString
          val updatedUserAnswers = userAnswers
            .copy(data = updatedData)
            .setOrException(EndCompanyBenefitsStopDatePage, dateString)
          journeyCacheRepository.set(updatedUserAnswers)
        }
      } yield Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber())
    } else {
      journeyCacheRepository
        .set {
          val updatedUserAnswers = userAnswers.setOrException(EndCompanyBenefitsStopDatePage, dateString)
          updatedUserAnswers
        }
        .map { _ =>
          Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit())
        }
    }
  }

  def submitStopDate: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val taxYear                   = TaxYear()
    val userAnswers               = request.userAnswers

    val currentBenefitName    = userAnswers.get(EndCompanyBenefitsNamePage).toString
    val currentEmploymentName = userAnswers.get(EndCompanyBenefitsEmploymentNamePage).toString

    RemoveCompanyBenefitStopDateForm(currentBenefitName, currentEmploymentName).form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val mandatoryJourneyValues = Seq(
            userAnswers.get(EndCompanyBenefitsNamePage).toString,
            userAnswers.get(EndCompanyBenefitsEmploymentNamePage).toString
          )
          Future.successful(
            BadRequest(
              removeCompanyBenefitStopDate(formWithErrors, mandatoryJourneyValues.head, mandatoryJourneyValues(1))
            )
          )
        },
        date => checkDate(date, userAnswers, user, taxYear)
      )
  }

  def totalValueOfBenefit(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers = request.userAnswers

    val mandatoryJourneyValues = Seq(
      userAnswers.get(EndCompanyBenefitsEmploymentNamePage),
      userAnswers.get(EndCompanyBenefitsNamePage)
    )
    val optionalValue          = userAnswers.get(EndCompanyBenefitsValuePage)

    (mandatoryJourneyValues, optionalValue) match {
      case (mandatory, _) if mandatory.forall(_.isDefined) =>
        val form = CompanyBenefitTotalValueForm.form.fill(optionalValue.getOrElse(""))
        Future.successful(
          Ok(
            removeBenefitTotalValue(
              BenefitViewModel(mandatoryJourneyValues.head.get, mandatoryJourneyValues(1).get),
              form
            )
          )
        )
      case _                                               =>
        Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
    }
  }

  def submitBenefitValue(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    CompanyBenefitTotalValueForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val mandatoryJourneyValues = Seq(
            request.userAnswers.get(EndCompanyBenefitsEmploymentNamePage).toString,
            request.userAnswers.get(EndCompanyBenefitsNamePage).toString
          )
          Future.successful(
            BadRequest(
              removeBenefitTotalValue(
                BenefitViewModel(mandatoryJourneyValues.head, mandatoryJourneyValues(1)),
                formWithErrors
              )
            )
          )
        },
        totalValue => {
          val rounded            = BigDecimal(FormHelper.stripNumber(totalValue)).setScale(0, RoundingMode.UP)
          val updatedUserAnswers = request.userAnswers.setOrException(EndCompanyBenefitsValuePage, rounded.toString)
          journeyCacheRepository
            .set(updatedUserAnswers)
            .map(_ => Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber()))
        }
      )
  }

  def telephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val userAnswers               = request.userAnswers

    val cache: Map[String, String] = Map(
      BenefitDecisionPage.toString                     -> userAnswers.get(BenefitDecisionPage).toString,
      EndCompanyBenefitsStopDatePage.toString          -> userAnswers.get(EndCompanyBenefitsStopDatePage).toString,
      EndCompanyBenefitsTypePage.toString              -> userAnswers.get(EndCompanyBenefitsTypePage).toString,
      EndCompanyBenefitsNamePage.toString              -> userAnswers.get(EndCompanyBenefitsNamePage).toString,
      EndCompanyBenefitsValuePage.toString             -> userAnswers.get(EndCompanyBenefitsValuePage).toString,
      EndCompanyBenefitsTelephoneQuestionPage.toString -> userAnswers
        .get(EndCompanyBenefitsTelephoneQuestionPage)
        .toString,
      EndCompanyBenefitsTelephoneNumberPage.toString   -> userAnswers.get(EndCompanyBenefitsTelephoneNumberPage).toString
    )

    val telephoneNumberViewModel = extractViewModelFromCache(cache)

    val form = YesNoTextEntryForm
      .form()
      .fill(
        YesNoTextEntryForm(
          userAnswers.get(EndCompanyBenefitsTelephoneQuestionPage),
          userAnswers.get(EndCompanyBenefitsTelephoneNumberPage)
        )
      )
    Future.successful(Ok(canWeContactByPhone(Some(user), telephoneNumberViewModel, form)))
  }

  def submitTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val user        = request.taiUser
    val userAnswers = request.userAnswers

    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors => {

          val cache: Map[String, String] =
            userAnswers.data.as[Map[String, JsValue]].view.mapValues(_.toString).toMap
          val telephoneNumberViewModel   = extractViewModelFromCache(cache)

          Future.successful(BadRequest(canWeContactByPhone(Some(user), telephoneNumberViewModel, formWithErrors)))
        },
        form => {

          val mandatoryData =
            Map(
              EndCompanyBenefitConstants.TelephoneQuestionKey -> form.yesNoChoice.getOrElse(FormValuesConstants.NoValue)
            )

          val dataForCache = form.yesNoChoice match {
            case Some(FormValuesConstants.YesValue) =>
              mandatoryData ++ Map(EndCompanyBenefitConstants.TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _                                  =>
              mandatoryData ++ Map(EndCompanyBenefitConstants.TelephoneNumberKey -> "")
          }

          val updatedData: JsObject = Json.toJson(dataForCache).as[JsObject]
          val updatedUserAnswers    = userAnswers.copy(data = userAnswers.data ++ updatedData)

          journeyCacheRepository.set(updatedUserAnswers) map { _ =>
            Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers())
          }
        }
      )
  }

  def checkYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val userAnswers               = request.userAnswers

    val mandatoryValues = for {
      employmentName    <- userAnswers.get(EndCompanyBenefitsEmploymentNamePage)
      benefitName       <- userAnswers.get(EndCompanyBenefitsNamePage)
      stopDate          <- userAnswers.get(EndCompanyBenefitsStopDatePage)
      telephoneQuestion <- userAnswers.get(EndCompanyBenefitsTelephoneQuestionPage)
    } yield (employmentName, benefitName, stopDate, telephoneQuestion)

    val benefitValue    = userAnswers.get(EndCompanyBenefitsValuePage)
    val telephoneNumber = userAnswers.get(EndCompanyBenefitsTelephoneNumberPage)

    mandatoryValues match {
      case Some((employmentName, benefitName, stopDateRaw, telephoneQuestion)) =>
        Ok(
          removeCompanyBenefitCheckYourAnswers(
            RemoveCompanyBenefitsCheckYourAnswersViewModel(
              employmentName,
              benefitName,
              LocalDate.parse(stopDateRaw),
              benefitValue,
              telephoneQuestion,
              telephoneNumber
            )
          )
        )
      case _                                                                   => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def submitYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers = request.userAnswers

    val mandatoryValues = for {
      id                <- userAnswers.get(EndCompanyBenefitsIdPage)
      benefitType       <- userAnswers.get(EndCompanyBenefitsTypePage)
      stopDateRaw       <- userAnswers.get(EndCompanyBenefitsStopDatePage)
      telephoneQuestion <- userAnswers.get(EndCompanyBenefitsTelephoneQuestionPage)
    } yield (id, benefitType, stopDateRaw, telephoneQuestion)

    mandatoryValues match {
      case Some((id, benefitType, stopDateRaw, telephoneQuestion)) =>
        val stopDate        = LocalDate.parse(stopDateRaw).format(DateTimeFormatter.ofPattern(TaxDateWordMonthFormat))
        val benefitValue    = userAnswers.get(EndCompanyBenefitsValuePage)
        val telephoneNumber = userAnswers.get(EndCompanyBenefitsTelephoneNumberPage)

        val model = EndedCompanyBenefit(
          benefitType = benefitType,
          stopDate = stopDate,
          valueOfBenefit = benefitValue,
          contactByPhone = telephoneQuestion,
          phoneNumber = telephoneNumber
        )

        for {
          _ <- benefitsService.endedCompanyBenefit(user.nino, id, model)
          _ <- journeyCacheRepository.set(
                 UserAnswers(request.userAnswers.sessionId, user.nino.nino)
                   .setOrException(EndCompanyBenefitsEndEmploymentBenefitsPage, true)
               )
          _ <- journeyCacheRepository.clear(request.userAnswers.sessionId, user.nino.nino)
        } yield Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.confirmation())
      case _                                                       => Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
    }
  }

  def cancel: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    for {
      mandatoryJourneyValues <- {
        val userAnswers = request.userAnswers

        Future.successful(userAnswers.get(EndCompanyBenefitsRefererPage))
      }
      _                      <- journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino)
    } yield Redirect(mandatoryJourneyValues.head)
  }

  def confirmation(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    trackingService.isAnyIFormInProgress(user.nino.nino).map { timeToProcess =>
      val (title, summary) = timeToProcess match {
        case ThreeWeeks => ("tai.confirmation.threeWeeks", "tai.confirmation.threeWeeks.paraTwo")
        case _          => ("tai.confirmation.subheading", "tai.confirmation.paraTwo")
      }
      Ok(removeCompanyBenefitConfirmation(title, summary))
    }
  }

  private def extractViewModelFromCache(
    cache: Map[String, String]
  )(implicit messages: Messages): CanWeContactByPhoneViewModel = {
    val backUrl =
      if (cache.contains(EndCompanyBenefitConstants.BenefitValueKey)) {
        controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit().url
      } else {
        controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url
      }

    CanWeContactByPhoneViewModel(
      messages("tai.benefits.ended.journey.preHeader"),
      messages("tai.canWeContactByPhone.title"),
      backUrl,
      controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url,
      controllers.benefits.routes.RemoveCompanyBenefitController.cancel().url
    )
  }

}
