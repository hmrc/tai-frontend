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

package controllers

import com.google.inject.name.Named
import javax.inject.Inject
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util._
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.viewModels.{GoogleAnalyticsSettings, SameEstimatedPayViewModel}

import scala.Function.tupled
import scala.concurrent.Future
import scala.util.control.NonFatal

class IncomeController @Inject()(@Named("Update Income") journeyCacheService: JourneyCacheService,
                                 taxAccountService: TaxAccountService,
                                 employmentService: EmploymentService,
                                 incomeService: IncomeService,
                                 estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
                                 authenticate: AuthAction,
                                 validatePerson: ValidatePerson,
                                 override implicit val partialRetriever: FormPartialRetriever,
                                 override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with JourneyCacheConstants
  with FormValuesConstants
  with FeatureTogglesConfig {

  def cancel(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.flush() map { _ =>
        Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
      }
  }

  def regularIncome(): Action[AnyContent] = (authenticate andThen validatePerson).async {
      implicit request =>
        implicit val user = request.taiUser
        val nino = user.nino

        (for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employmentAmount <- incomeService.employmentAmount(nino, id)
          latestPayment <- incomeService.latestPayment(nino, id)
          cacheData = incomeService.cachePaymentForRegularIncome(latestPayment)
          _ <- journeyCacheService.cache(cacheData)
        } yield {
          val amountYearToDate: BigDecimal = latestPayment.map(_.amountYearToDate).getOrElse(0)

          Ok(views.html.incomes.editIncome(EditIncomeForm.create(employmentAmount), false,
            employmentAmount.employmentId, amountYearToDate.toString))
        }).recover {
          case NonFatal(e) => internalServerError(e.getMessage)
        }
  }

  def sameEstimatedPayInCache(): Action[AnyContent] = (authenticate andThen validatePerson).async {
      implicit request =>
        (for {
          cachedData <- journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IdKey, UpdateIncome_ConfirmedNewAmountKey)
        } yield {
          val model = SameEstimatedPayViewModel(cachedData(0), cachedData(1).toInt, cachedData(2).toInt, false)
          Ok(views.html.incomes.sameEstimatedPay(model))
        }).recover {
          case NonFatal(e) => internalServerError(e.getMessage)
        }
  }

  def sameAnnualEstimatedPay(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      val cachedDataFuture = journeyCacheService.mandatoryValues(UpdateIncome_NameKey)
      val idFuture = journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
      val nino = request.taiUser.nino

      (for {
        cachedData <- cachedDataFuture
        id <- idFuture
        income <- incomeService.employmentAmount(nino, id)
      } yield {
        val model = SameEstimatedPayViewModel(cachedData(0), id, income.oldAmount, income.isOccupationalPension)
        Ok(views.html.incomes.sameEstimatedPay(model))
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }


  def editRegularIncome(): Action[AnyContent] = (authenticate andThen validatePerson).async {
      implicit request =>
        implicit val user = request.taiUser
          journeyCacheService.collectedValues(Seq(UpdateIncome_PayToDateKey, UpdateIncome_IdKey, UpdateIncome_NameKey), Seq(UpdateIncome_DateKey)) flatMap tupled {
            (mandatorySeq, optionalSeq) => {
              val date = optionalSeq.head.map(date => LocalDate.parse(date))
              val employerName = mandatorySeq(2)
              val payToDate = BigDecimal(mandatorySeq.head)

              EditIncomeForm.bind(employerName, payToDate, date).fold(
                (formWithErrors: Form[EditIncomeForm]) => {
                  val webChat = true
                  Future.successful(BadRequest(views.html.incomes.editIncome(formWithErrors,
                    false,
                    mandatorySeq(1).toInt,
                    mandatorySeq.head, webChat = webChat)))
                },
                (income: EditIncomeForm) => determineEditRedirect(income, routes.IncomeController.confirmRegularIncome)
              )
            }
        }
  }

  private def isCachedIncomeTheSame(currentCache: Map[String, String], newAmount: Option[String]): Boolean = {
    FormHelper.areEqual(currentCache.get(UpdateIncome_ConfirmedNewAmountKey), newAmount)
  }

  private def isIncomeTheSame(income: EditIncomeForm): Boolean = {
    FormHelper.areEqual(Some(income.oldAmount.toString), income.newAmount)
  }

  def confirmRegularIncome(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      val nino = user.nino

      (for {
        cachedData <- journeyCacheService.mandatoryValues(UpdateIncome_IdKey, UpdateIncome_NewAmountKey)
        id = cachedData.head.toInt
        taxCodeIncomeDetails <- taxAccountService.taxCodeIncomes(nino, TaxYear())
        employmentDetails <- employmentService.employment(nino, id)
      } yield {
        (taxCodeIncomeDetails, employmentDetails) match {
          case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
            taxCodeIncomes.find(_.employmentId.contains(cachedData.head.toInt)) match {
              case Some(taxCodeIncome) =>
                val employmentAmount = EmploymentAmount(taxCodeIncome, employment)
                val (_, date) = retrieveAmountAndDate(employment)
                val form = EditIncomeForm(employmentAmount, cachedData(1), date.map(_.toString()))

                val gaSetting = gaSettings(GoogleAnalyticsConstants.taiCYEstimatedIncome, form.oldAmount, form.newAmount)
                Ok(views.html.incomes.confirm_save_Income(form, gaSetting))

              case _ => throw new RuntimeException(s"Not able to found employment with id $id")
            }
          case _ => throw new RuntimeException("Exception while reading employment and tax code details")
        }
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  private def gaSettings(gaKey: String, currentAmount: Int, newAmount: Option[String]): GoogleAnalyticsSettings = {
    val poundedCurrentAmount = MonetaryUtil.withPoundPrefix(currentAmount)
    val poundedNewAmount = MonetaryUtil.withPoundPrefix(FormHelper.stripNumber(newAmount).getOrElse("0").toInt)

    val amounts = Map("currentAmount" -> poundedCurrentAmount, "newAmount" -> poundedNewAmount)

    val dimensions: Option[Map[String, String]] = Some(Map(gaKey -> MapForGoogleAnalytics.format(amounts)))
    GoogleAnalyticsSettings(dimensions = dimensions)
  }

  def updateEstimatedIncome(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      def respondWithSuccess(employerName: String, employerId: Int, incomeType: String, newAmount: String)
                            (implicit user: AuthedUser, request: Request[AnyContent]): Result = {
        journeyCacheService.cache(UpdateIncome_ConfirmedNewAmountKey, newAmount)
        incomeType match {
          case TaiConstants.IncomeTypePension => Ok(views.html.incomes.editPensionSuccess(employerName, employerId))
          case _ => Ok(views.html.incomes.editSuccess(employerName, employerId))
        }
      }

      val updateJourneyCompletion: String => Future[Map[String, String]] = (incomeId: String) => {
        estimatedPayJourneyCompletionService.journeyCompleted(incomeId)
      }

      journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_NewAmountKey, UpdateIncome_IdKey, UpdateIncome_IncomeTypeKey)
        .flatMap(cache => {

          val incomeName :: newAmount :: incomeId :: incomeType :: Nil = cache.toList

          taxAccountService.updateEstimatedIncome(user.nino, FormHelper.stripNumber(newAmount).toInt, TaxYear(), incomeId.toInt) flatMap {
            case TaiSuccessResponse => {
              updateJourneyCompletion(incomeId) map { _ =>
                respondWithSuccess(incomeName, incomeId.toInt, incomeType, newAmount)
              }
            }
            case _ => throw new RuntimeException("Failed to update estimated income")
          }
        }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  def pensionIncome(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser
      val nino = user.nino

      (for {
        id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
        employmentAmount <- incomeService.employmentAmount(nino, id)
        latestPayment <- incomeService.latestPayment(nino, id)
        cacheData = incomeService.cachePaymentForRegularIncome(latestPayment)
        _ <- journeyCacheService.cache(cacheData)
      } yield {
        val amountYearToDate: BigDecimal = latestPayment.map(_.amountYearToDate).getOrElse(0)
        Ok(views.html.incomes.editPension(EditIncomeForm.create(employmentAmount), false,
          employmentAmount.employmentId, amountYearToDate.toString()))
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  private def determineEditRedirect(income: EditIncomeForm, confirmationCallback: Call)(implicit hc: HeaderCarrier): Future[Result] = {
    for {
      currentCache <- journeyCacheService.currentCache
    } yield {
      if (isCachedIncomeTheSame(currentCache, income.newAmount)) {
        Redirect(routes.IncomeController.sameEstimatedPayInCache())
      }
      else if (isIncomeTheSame(income)) {
        Redirect(routes.IncomeController.sameAnnualEstimatedPay())
      } else {
        journeyCacheService.cache(UpdateIncome_NewAmountKey, income.newAmount.getOrElse("0"))
        Redirect(confirmationCallback)
      }
    }
  }

  def editPensionIncome(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      journeyCacheService.collectedValues(Seq(UpdateIncome_PayToDateKey, UpdateIncome_IdKey, UpdateIncome_NameKey), Seq(UpdateIncome_DateKey)) flatMap tupled {
        (mandatorySeq, optionalSeq) => {
          val date = optionalSeq.head.map(date => LocalDate.parse(date))
          EditIncomeForm.bind(mandatorySeq(2), BigDecimal(mandatorySeq.head), date).fold(
            formWithErrors => {
              val webChat = true
              Future.successful(BadRequest(views.html.incomes.editPension(formWithErrors,
                false,
                mandatorySeq(1).toInt,
                mandatorySeq.head, webChat = webChat)))
            },
            (income: EditIncomeForm) => determineEditRedirect(income, routes.IncomeController.confirmPensionIncome)
          )
        }
      }

  }

  def confirmPensionIncome(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser
      val nino = user.nino

      (for {
        cachedData <- journeyCacheService.mandatoryValues(UpdateIncome_IdKey, UpdateIncome_NewAmountKey)
        id = cachedData.head.toInt
        taxCodeIncomeDetails <- taxAccountService.taxCodeIncomes(nino, TaxYear())
        employmentDetails <- employmentService.employment(nino, id)
      } yield {

        (taxCodeIncomeDetails, employmentDetails) match {
          case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
            taxCodeIncomes.find(_.employmentId.contains(cachedData.head.toInt)) match {
              case Some(taxCodeIncome) =>
                val employmentAmount = EmploymentAmount(taxCodeIncome, employment)
                val (_, date) = retrieveAmountAndDate(employment)
                val form = EditIncomeForm(employmentAmount, cachedData(1), date.map(_.toString()))

                val gaSetting = gaSettings(GoogleAnalyticsConstants.taiCYEstimatedIncome, form.oldAmount, form.newAmount)

                Ok(views.html.incomes.confirm_save_Income(form, gaSetting))
              case _ => throw new RuntimeException(s"Not able to found employment with id $id")
            }
          case _ => throw new RuntimeException("Exception while reading employment and tax code details")
        }
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  def viewIncomeForEdit: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      for {
        id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
        employmentAmount <- incomeService.employmentAmount(request.taiUser.nino, id)
      } yield {
        (employmentAmount.isLive, employmentAmount.isOccupationalPension) match {
          case (true, false) => Redirect(routes.IncomeController.regularIncome())
          case (false, false) => Redirect(routes.TaxAccountSummaryController.onPageLoad())
          case _ => Redirect(routes.IncomeController.pensionIncome())
        }
      }
  }

  private def retrieveAmountAndDate(employment: Employment): (BigDecimal, Option[LocalDate]) = {
    val amountAndDate = for {
      latestAnnualAccount <- employment.latestAnnualAccount
      latestPayment <- latestAnnualAccount.latestPayment
    } yield Tuple2(latestPayment.amountYearToDate, Some(latestPayment.date))
    amountAndDate.getOrElse(0, None)
  }
}
