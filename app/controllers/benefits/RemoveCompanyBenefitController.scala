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
import pages.benefits._
import play.api.i18n.Messages
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheNewRepository
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
  journeyCacheNewRepository: JourneyCacheNewRepository
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def stopDate: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheNewRepository.get(request.userAnswers.sessionId, user.nino.nino).flatMap { userAnswersOption =>
      userAnswersOption
        .map { userAnswers =>
          val currentBenefitName = userAnswers.get(EndCompanyBenefitsNamePage).get
          val currentEmploymentName = userAnswers.get(EndCompanyBenefitsEmploymentNamePage).get
          val removeCompanyBenefitForm = RemoveCompanyBenefitStopDateForm(currentBenefitName, currentEmploymentName)

          val form = userAnswers
            .get(EndCompanyBenefitsStopDatePage)
            .map(dateString => removeCompanyBenefitForm.form.fill(LocalDate.parse(dateString)))
            .getOrElse(removeCompanyBenefitForm.form)

          Future.successful(
            Ok(
              removeCompanyBenefitStopDate(
                form,
                request.userAnswers.get(EndCompanyBenefitsNamePage).get,
                request.userAnswers.get(EndCompanyBenefitsEmploymentNamePage).get
              )
            )
          )
        }
        .getOrElse(Future.successful(InternalServerError("UserAnswers not found")))
    }
  }

  // scalastyle:off method.length
  def submitStopDate: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val taxYear = TaxYear()

    journeyCacheNewRepository.get(request.userAnswers.sessionId, user.nino.nino).flatMap { case Some(userAnswers) =>
      val currentBenefitName = userAnswers.get(EndCompanyBenefitsNamePage).get
      val currentEmploymentName = userAnswers.get(EndCompanyBenefitsEmploymentNamePage).get

      RemoveCompanyBenefitStopDateForm(currentBenefitName, currentEmploymentName).form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val mandatoryJourneyValues = Seq(
              request.userAnswers.get(EndCompanyBenefitsNamePage).get,
              request.userAnswers.get(EndCompanyBenefitsEmploymentNamePage).get
            )
            Future.successful(
              BadRequest(
                removeCompanyBenefitStopDate(formWithErrors, mandatoryJourneyValues.head, mandatoryJourneyValues(1))
              )
            )
          },
          { date =>
            val dateString = date.toString
            if (date isBefore taxYear.start) {
              // BeforeTaxYearEnd
              for {
                userAnswersOption <- journeyCacheNewRepository.get(request.userAnswers.sessionId, user.nino.nino)
                _                 <- journeyCacheNewRepository.clear(request.userAnswers.sessionId, user.nino.nino)
                _ <-
                  userAnswersOption
                    .map { userAnswers =>
                      val current = userAnswers.data.as[Map[String, JsValue]]
                      val filtered = current.filterNot(_._1 == EndCompanyBenefitsValuePage.toString)
                      val updatedData = filtered ++ Map(EndCompanyBenefitsStopDatePage.toString -> JsString(dateString))

                      Json.toJson(updatedData) match {
                        case jsObject: JsObject => journeyCacheNewRepository.set(userAnswers.copy(data = jsObject))
                        case _                  => Future.failed(new Exception("Updated data is not a JSON object"))
                      }
                    }
                    .getOrElse(Future.failed(new Exception("UserAnswers not found")))
              } yield Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber())
            } else {
              // OnOrAfterTaxYearEnd
              journeyCacheNewRepository
                .set(
                  request.userAnswers
                    .setOrException(EndCompanyBenefitsStopDatePage, dateString)
                )
                .map { _ =>
                  Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit())
                }
            }
          }
        )

    }
  }

  def totalValueOfBenefit(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheNewRepository.get(request.userAnswers.sessionId, user.nino.nino).flatMap { userAnswersOption =>
      userAnswersOption
        .map { userAnswers =>
          val mandatoryJourneyValues = Seq(
            userAnswers.get(EndCompanyBenefitsEmploymentNamePage).get,
            userAnswers.get(EndCompanyBenefitsNamePage).get
          )
          val optionalValue = userAnswers.get(EndCompanyBenefitsValuePage)

          val form = CompanyBenefitTotalValueForm.form.fill(optionalValue.getOrElse(""))
          Future.successful(
            Ok(removeBenefitTotalValue(BenefitViewModel(mandatoryJourneyValues.head, mandatoryJourneyValues(1)), form))
          )
        }
        .getOrElse(Future.failed(new Exception("UserAnswers not found")))
    }
  }

  def submitBenefitValue(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    CompanyBenefitTotalValueForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          journeyCacheNewRepository.get(request.userAnswers.sessionId, user.nino.nino).flatMap { userAnswersOption =>
            userAnswersOption
              .map { userAnswers =>
                val mandatoryJourneyValues = Seq(
                  userAnswers.get(EndCompanyBenefitsEmploymentNamePage).get,
                  userAnswers.get(EndCompanyBenefitsNamePage).get
                )
                Future.successful(
                  BadRequest(
                    removeBenefitTotalValue(
                      BenefitViewModel(mandatoryJourneyValues.head, mandatoryJourneyValues(1)),
                      formWithErrors
                    )
                  )
                )
              }
              .getOrElse(Future.failed(new Exception("UserAnswers not found")))
          },
        totalValue => {
          val rounded = BigDecimal(FormHelper.stripNumber(totalValue)).setScale(0, RoundingMode.UP)
          val updatedUserAnswers = request.userAnswers.setOrException(EndCompanyBenefitsValuePage, rounded.toString)
          journeyCacheNewRepository
            .set(updatedUserAnswers)
            .map(_ => Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber()))
        }
      )
  }

  def telephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheNewRepository.get(request.userAnswers.sessionId, user.nino.nino) flatMap {
      case Some(userAnswers) =>
        val cache: Map[String, String] = Map(
          EndCompanyBenefitsValuePage.toString -> userAnswers.get(EndCompanyBenefitsValuePage).getOrElse(""),
          EndCompanyBenefitsTelephoneQuestionPage.toString ->
            userAnswers.get(EndCompanyBenefitsTelephoneQuestionPage).getOrElse(""),
          EndCompanyBenefitsTelephoneNumberPage.toString -> userAnswers
            .get(EndCompanyBenefitsTelephoneNumberPage)
            .getOrElse("")
        )

        val telephoneNumberViewModel = extractViewModelFromCache(cache)
        val form = YesNoTextEntryForm
          .form()
          .fill(
            YesNoTextEntryForm(
              request.userAnswers.get(EndCompanyBenefitsTelephoneQuestionPage),
              request.userAnswers.get(EndCompanyBenefitsTelephoneNumberPage)
            )
          )
        Future.successful(Ok(canWeContactByPhone(Some(user), telephoneNumberViewModel, form)))

      case None =>
        Future.successful(InternalServerError("No user answers found"))
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val user = request.taiUser
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors =>
          journeyCacheNewRepository.get(request.userAnswers.sessionId, user.nino.nino) map { userAnswersOption =>
            val cache: Map[String, String] = userAnswersOption.map(_.data.as[Map[String, String]]).getOrElse(Map.empty)
            val telephoneNumberViewModel = extractViewModelFromCache(cache)
            BadRequest(canWeContactByPhone(Some(user), telephoneNumberViewModel, formWithErrors))
          },
        form => {
          val mandatoryData =
            Map(
              EndCompanyBenefitConstants.TelephoneQuestionKey -> form.yesNoChoice.getOrElse(FormValuesConstants.NoValue)
            )

          val dataForCache = form.yesNoChoice match {
            case Some(FormValuesConstants.YesValue) =>
              mandatoryData ++ Map(EndCompanyBenefitConstants.TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(EndCompanyBenefitConstants.TelephoneNumberKey -> "")
          }

          Json.toJson(dataForCache) match {
            case jsObject: JsObject =>
              val userAnswers = UserAnswers(request.userAnswers.sessionId, user.nino.nino, data = jsObject)
              journeyCacheNewRepository.set(userAnswers) map { _ =>
                Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers())
              }
            case _ =>
              Future.failed(new Exception("Updated data is not a JSON object"))
          }
        }
      )
  }

  def checkYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheNewRepository
      .get(request.userAnswers.sessionId, user.nino.nino)
      .map {
        case None =>
          Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
        case Some(userAnswers) =>
          val mandatoryJourneyValues = Seq(
            userAnswers.get(EndCompanyBenefitsEmploymentNamePage),
            userAnswers.get(EndCompanyBenefitsNamePage),
            userAnswers.get(EndCompanyBenefitsStopDatePage),
            userAnswers.get(EndCompanyBenefitsTelephoneQuestionPage),
            userAnswers.get(EndCompanyBenefitsRefererPage)
          )
          val optionalSeq = Seq(
            userAnswers.get(EndCompanyBenefitsValuePage),
            userAnswers.get(EndCompanyBenefitsTelephoneNumberPage)
          )

          val stopDate = LocalDate.parse(mandatoryJourneyValues(2).getOrElse(""))

          Ok(
            removeCompanyBenefitCheckYourAnswers(
              RemoveCompanyBenefitsCheckYourAnswersViewModel(
                mandatoryJourneyValues.head.getOrElse(""),
                mandatoryJourneyValues(1).getOrElse(""),
                stopDate,
                optionalSeq.head,
                mandatoryJourneyValues(3).getOrElse(""),
                optionalSeq(1)
              )
            )
          )
      }
  }

  def submitYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    for {
      (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheNewRepository
                                                 .get(request.userAnswers.sessionId, user.nino.nino)
                                                 .map {
                                                   case Some(userAnswers) =>
                                                     (
                                                       Seq(
                                                         userAnswers.get(EndCompanyBenefitsIdPage),
                                                         userAnswers.get(EndCompanyBenefitsEmploymentNamePage),
                                                         userAnswers.get(EndCompanyBenefitsTypePage),
                                                         userAnswers.get(EndCompanyBenefitsStopDatePage),
                                                         userAnswers.get(EndCompanyBenefitsTelephoneQuestionPage)
                                                       ).flatten,
                                                       Seq(
                                                         userAnswers.get(EndCompanyBenefitsValuePage),
                                                         userAnswers.get(EndCompanyBenefitsTelephoneNumberPage)
                                                       ).flatten
                                                     )
                                                   case None =>
                                                     throw new RuntimeException("Failed to retrieve data from cache")
                                                 }

      stopDate =
        LocalDate.parse(mandatoryCacheSeq(3).toString).format(DateTimeFormatter.ofPattern(TaxDateWordMonthFormat))

      model = EndedCompanyBenefit(
                benefitType = mandatoryCacheSeq(2).toString,
                stopDate = stopDate,
                valueOfBenefit = Some(optionalCacheSeq.head),
                contactByPhone = mandatoryCacheSeq(4).toString,
                phoneNumber = Some(optionalCacheSeq(1))
              )

      _ <- benefitsService.endedCompanyBenefit(user.nino, mandatoryCacheSeq.head.toString.toInt, model)
      _ <- journeyCacheNewRepository.set(
             UserAnswers(request.userAnswers.sessionId, user.nino.nino)
               .setOrException(EndCompanyBenefitsEndEmploymentBenefitsPage, true.toString)
           )
      _ <- journeyCacheNewRepository.clear(request.userAnswers.sessionId, user.nino.nino)
    } yield Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.confirmation())
  }

  def cancel: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    for {
      mandatoryJourneyValues <- journeyCacheNewRepository
                                  .get(request.userAnswers.sessionId, request.userAnswers.nino)
                                  .map(_.flatMap(_.get(EndCompanyBenefitsRefererPage)))
      _ <- journeyCacheNewRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino)
    } yield Redirect(mandatoryJourneyValues.head)
  }

  def confirmation(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
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
