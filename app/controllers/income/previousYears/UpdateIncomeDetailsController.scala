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

package controllers.income.previousYears

import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.TrackSuccessfulJourneyConstantsUpdatePreviousYearPage
import pages.income.*
import play.api.i18n.Messages
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.forms.income.previousYears.{UpdateIncomeDetailsDecisionForm, UpdateIncomeDetailsForm}
import uk.gov.hmrc.tai.model.domain.IncorrectIncome
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service.PreviousYearsIncomeService
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.income.previousYears.{UpdateHistoricIncomeDetailsViewModel, UpdateIncomeDetailsCheckYourAnswersViewModel}
import views.html.CanWeContactByPhoneView
import views.html.incomes.previousYears.{CheckYourAnswersView, UpdateIncomeDetailsConfirmationView, UpdateIncomeDetailsDecisionView, UpdateIncomeDetailsView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class UpdateIncomeDetailsController @Inject() (
  previousYearsIncomeService: PreviousYearsIncomeService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  canWeContactByPhone: CanWeContactByPhoneView,
  CheckYourAnswers: CheckYourAnswersView,
  UpdateIncomeDetailsDecision: UpdateIncomeDetailsDecisionView,
  UpdateIncomeDetails: UpdateIncomeDetailsView,
  UpdateIncomeDetailsConfirmation: UpdateIncomeDetailsConfirmationView,
  journeyCacheRepository: JourneyCacheRepository,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  private def withTaxYear(userAnswers: UserAnswers)(block: String => Future[Result]): Future[Result] =
    (userAnswers.data \ UpdatePreviousYearsIncomeTaxYearPage.toString).asOpt[JsString] match {
      case Some(JsString(taxYearString)) => block(taxYearString)
      case _                             => Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
    }

  def telephoneNumberViewModel(taxYear: Int)(implicit messages: Messages): CanWeContactByPhoneViewModel =
    CanWeContactByPhoneViewModel(
      messages("tai.income.previousYears.journey.preHeader"),
      messages("tai.canWeContactByPhone.title"),
      controllers.income.previousYears.routes.UpdateIncomeDetailsController.details().url,
      controllers.income.previousYears.routes.UpdateIncomeDetailsController.submitTelephoneNumber().url,
      controllers.routes.PayeControllerHistoric.payePage(TaxYear(taxYear)).url
    )

  def decision(taxYear: TaxYear): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val updatedAnswers            = request.userAnswers.setOrException(UpdatePreviousYearsIncomeTaxYearPage, taxYear.year.toString)

    journeyCacheRepository.set(updatedAnswers).map { _ =>
      Ok(UpdateIncomeDetailsDecision(UpdateIncomeDetailsDecisionForm.form, taxYear))
    }
  }

  def submitDecision(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    UpdateIncomeDetailsDecisionForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(UpdateIncomeDetailsDecision(formWithErrors, TaxYear().prev))),
        {
          case Some(FormValuesConstants.NoValue) =>
            Future.successful(Redirect(controllers.income.previousYears.routes.UpdateIncomeDetailsController.details()))
          case _                                 => Future.successful(Redirect(controllers.routes.PayeControllerHistoric.payePage(TaxYear().prev)))
        }
      )
  }

  def details(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val userAnswers               = request.userAnswers

    withTaxYear(userAnswers) { taxYearString =>
      Future
        .successful(
          Ok(
            UpdateIncomeDetails(
              UpdateHistoricIncomeDetailsViewModel(taxYearString.toInt),
              UpdateIncomeDetailsForm.form.fill(userAnswers.get(UpdatePreviousYearsIncomePage).getOrElse(""))
            )
          )
        )
        .recover { case NonFatal(exception) =>
          errorPagesHandler.internalServerError(exception.getMessage)
        }
    }
  }

  def submitDetails(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    UpdateIncomeDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          withTaxYear(request.userAnswers) { taxYearString =>
            Future.successful(
              BadRequest(
                UpdateIncomeDetails(
                  UpdateHistoricIncomeDetailsViewModel(taxYearString.toInt),
                  formWithErrors
                )
              )
            )
          },
        incomeDetails => {
          val updatedAnswers = request.userAnswers.set(UpdatePreviousYearsIncomePage, incomeDetails.replace("\r", ""))
          updatedAnswers match {
            case Success(answers)   =>
              journeyCacheRepository.set(answers).map { _ =>
                Redirect(controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber())
              }
            case Failure(exception) =>
              Future.failed(exception)
          }
        }
      )
      .recover { case NonFatal(exception) =>
        errorPagesHandler.internalServerError(exception.getMessage)
      }
  }

  def telephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val userAnswers               = request.userAnswers
    withTaxYear(userAnswers) { taxYearString =>
      val isTelephone     = userAnswers.get(UpdatePreviousYearsIncomeTelephoneQuestionPage)
      val telephoneNumber = userAnswers.get(UpdatePreviousYearsIncomeTelephoneNumberPage)
      Future
        .successful(
          Ok(
            canWeContactByPhone(
              Some(user),
              telephoneNumberViewModel(taxYearString.toInt),
              YesNoTextEntryForm.form().fill(YesNoTextEntryForm(isTelephone, telephoneNumber))
            )
          )
        )
        .recover { case NonFatal(exception) =>
          errorPagesHandler.internalServerError(exception.getMessage)
        }
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.invalid"),
        Some(telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors =>
          withTaxYear(request.userAnswers) { taxYearString =>
            Future.successful(
              BadRequest(
                canWeContactByPhone(
                  Some(user),
                  telephoneNumberViewModel(taxYearString.toInt),
                  formWithErrors
                )
              )
            )
          },
        form => {
          val mandatoryData = Map(
            UpdatePreviousYearsIncomeTelephoneQuestionPage -> form.yesNoChoice
              .getOrElse(FormValuesConstants.NoValue)
          )
          val dataForCache  = form.yesNoChoice match {
            case Some(FormValuesConstants.YesValue) =>
              mandatoryData ++ Map(
                UpdatePreviousYearsIncomeTelephoneNumberPage -> form.yesNoTextEntry.getOrElse("")
              )
            case _                                  => mandatoryData ++ Map(UpdatePreviousYearsIncomeTelephoneNumberPage -> "")
          }

          val updatedAnswers = dataForCache.foldLeft(request.userAnswers) { case (answers, (key, value)) =>
            answers.setOrException(key, value)
          }

          journeyCacheRepository.set(updatedAnswers).map { _ =>
            Redirect(controllers.income.previousYears.routes.UpdateIncomeDetailsController.checkYourAnswers())
          }
        }
      )
      .recover { case NonFatal(exception) =>
        errorPagesHandler.internalServerError(exception.getMessage)
      }
  }

  def checkYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val userAnswers               = request.userAnswers
    val taxYearOpt                = userAnswers.get(UpdatePreviousYearsIncomeTaxYearPage)
    val incomeOpt                 = userAnswers.get(UpdatePreviousYearsIncomePage)
    val telephoneQuestionOpt      = userAnswers.get(UpdatePreviousYearsIncomeTelephoneQuestionPage)
    val telephoneNumberOpt        = userAnswers.get(UpdatePreviousYearsIncomeTelephoneNumberPage)

    Future
      .successful(
        (taxYearOpt, incomeOpt, telephoneQuestionOpt, telephoneNumberOpt) match {
          case (Some(taxYear), Some(income), Some(telephoneQuestion), telephoneNumberOpt) =>
            Ok(
              CheckYourAnswers(
                UpdateIncomeDetailsCheckYourAnswersViewModel(
                  tableHeader = TaxYear(taxYear.toInt).toString,
                  whatYouToldUs = income,
                  contactByPhone = telephoneQuestion,
                  phoneNumber = telephoneNumberOpt
                )
              )
            )
          case _                                                                          =>
            Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
        }
      )
      .recover { case NonFatal(exception) =>
        errorPagesHandler.internalServerError(exception.getMessage)
      }
  }

  def submitYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val nino                      = user.nino

    for {
      userAnswers      <- Future.successful(request.userAnswers)
      mandatoryCacheSeq = Seq(
                            userAnswers.get(UpdatePreviousYearsIncomeTaxYearPage).getOrElse(""),
                            userAnswers.get(UpdatePreviousYearsIncomePage).getOrElse(""),
                            userAnswers.get(UpdatePreviousYearsIncomeTelephoneQuestionPage).getOrElse("")
                          )
      optionalCacheSeq  = Seq(userAnswers.get(UpdatePreviousYearsIncomeTelephoneNumberPage))
      model             = IncorrectIncome(mandatoryCacheSeq(1), mandatoryCacheSeq(2), optionalCacheSeq.head)
      _                <- previousYearsIncomeService.incorrectIncome(nino, mandatoryCacheSeq.head.toInt, model)
      _                <- journeyCacheRepository.set(
                            userAnswers.copy(
                              data =
                                userAnswers.data + (TrackSuccessfulJourneyConstantsUpdatePreviousYearPage.toString -> JsBoolean(true))
                            )
                          )
      _                <- journeyCacheRepository.clear(request.userAnswers.sessionId, nino.nino)
    } yield Redirect(controllers.income.previousYears.routes.UpdateIncomeDetailsController.confirmation())
  }

  def confirmation(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    Future.successful(Ok(UpdateIncomeDetailsConfirmation()))
  }

}
