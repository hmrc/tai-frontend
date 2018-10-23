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

package controllers

import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.{AuditConstants, FormHelper, FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.{EstimatedEmploymentIncomeSuccessViewModel, EstimatedPensionIncomeSuccessViewModel}

import scala.Function.tupled
import scala.concurrent.Future

trait IncomeController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with JourneyCacheConstants
  with AuditConstants
  with FormValuesConstants
  with Auditable {

  def personService: PersonService

  def journeyCacheService: JourneyCacheService

  def taxAccountService: TaxAccountService

  def employmentService: EmploymentService

  def incomeService: IncomeService

  def regularIncome(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          for {
            id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
            employmentAmount <- incomeService.employmentAmount(Nino(user.getNino), id)
            latestPayment <- incomeService.latestPayment(Nino(user.getNino), id)
            cacheData = incomeService.cachePaymentForRegularIncome(latestPayment)
            _ <- journeyCacheService.cache(cacheData)
          } yield {
            val amountYearToDate: BigDecimal = latestPayment.map(_.amountYearToDate).getOrElse(0)
            Ok(views.html.incomes.editIncome(EditIncomeForm.create(employmentAmount), false,
              employmentAmount.employmentId, amountYearToDate.toString))
          }
        }
  }

  def editRegularIncome(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          sendActingAttorneyAuditEvent("handleRegularIncomeUpdateForEdit")

          journeyCacheService.collectedValues(Seq(UpdateIncome_PayToDateKey, UpdateIncome_IdKey, UpdateIncome_NameKey), Seq(UpdateIncome_DateKey)) flatMap tupled {
            (mandatorySeq, optionalSeq) => {
              val date = optionalSeq.head.map(date => LocalDate.parse(date))
              EditIncomeForm.bind(mandatorySeq(2), BigDecimal(mandatorySeq.head), date).fold(
                formWithErrors => {
                  val webChat = true
                  Future.successful(BadRequest(views.html.incomes.editIncome(formWithErrors,
                    false,
                    mandatorySeq(1).toInt,
                    mandatorySeq.head, webChat = webChat)))
                },
                income => {
                  journeyCacheService.cache(UpdateIncome_NewAmountKey, income.newAmount.getOrElse("0")).map { x =>
                    Redirect(routes.IncomeController.confirmRegularIncome())
                  }
                }
              )
            }
          }

        }
  }

  def confirmRegularIncome(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          sendActingAttorneyAuditEvent("confirmRegularIncome")
          for {
            cachedData <- journeyCacheService.mandatoryValues(UpdateIncome_IdKey, UpdateIncome_NewAmountKey)
            id = cachedData.head.toInt
            taxCodeIncomeDetails <- taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear())
            employmentDetails <- employmentService.employment(Nino(user.getNino), id)
          } yield {

            (taxCodeIncomeDetails, employmentDetails) match {
              case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
                taxCodeIncomes.find(_.employmentId.contains(cachedData.head.toInt)) match {
                  case Some(taxCodeIncome) =>
                    val employmentAmount = EmploymentAmount(taxCodeIncome, employment)
                    val (_, date) = retrieveAmountAndDate(employment)
                    val form = EditIncomeForm(employmentAmount, cachedData(1), date.map(_.toString()))
                    Ok(views.html.incomes.confirm_save_Income(form))
                  case _ => throw new RuntimeException(s"Not able to found employment with id $id")
                }
              case _ => throw new RuntimeException("Exception while reading employment and tax code details")
            }
          }

        }
  }

  def updatePensionIncome(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person => {
      implicit request => {

        def onSuccess(employerName: String): Result = {
          val viewModel = EstimatedPensionIncomeSuccessViewModel(employerName)
          Ok(views.html.incomes.editSuccess(viewModel))
        }

        ServiceCheckLite.personDetailsCheck {
          updateEstimatedIncomeSource(onSuccess)
        }
      }
    }
  }

  def updateEstimatedIncome(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person => {
      implicit request => {

        def onSuccess(employerName: String)(implicit user: TaiUser, request: Request[AnyContent]): Result = {
          val viewModel = EstimatedEmploymentIncomeSuccessViewModel(employerName)
          Ok(views.html.incomes.editSuccess(viewModel))
        }

        ServiceCheckLite.personDetailsCheck {
          updateEstimatedIncomeSource(onSuccess)
        }
      }
    }
  }

  def pensionIncome(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          for {
            id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
            employmentAmount <- incomeService.employmentAmount(Nino(user.getNino), id)
            latestPayment <- incomeService.latestPayment(Nino(user.getNino), id)
            cacheData = incomeService.cachePaymentForRegularIncome(latestPayment)
            _ <- journeyCacheService.cache(cacheData)
          } yield {
            val amountYearToDate: BigDecimal = latestPayment.map(_.amountYearToDate).getOrElse(0)
            Ok(views.html.incomes.editPension(EditIncomeForm.create(employmentAmount), false,
              employmentAmount.employmentId, amountYearToDate.toString()))
          }
        }
  }

  def editPensionIncome(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          sendActingAttorneyAuditEvent("handlePensionIncomeUpdateForEdit")

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
                income => {
                  journeyCacheService.cache(UpdateIncome_NewAmountKey, income.newAmount.getOrElse("0")).map { x =>
                    Redirect(routes.IncomeController.confirmPensionIncome())
                  }
                }
              )
            }
          }

        }
  }

  def confirmPensionIncome(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          sendActingAttorneyAuditEvent("confirmIncomeUpdatesForEdit")
          for {
            cachedData <- journeyCacheService.mandatoryValues(UpdateIncome_IdKey, UpdateIncome_NewAmountKey)
            id = cachedData.head.toInt
            taxCodeIncomeDetails <- taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear())
            employmentDetails <- employmentService.employment(Nino(user.getNino), id)
          } yield {

            (taxCodeIncomeDetails, employmentDetails) match {
              case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
                taxCodeIncomes.find(_.employmentId.contains(cachedData.head.toInt)) match {
                  case Some(taxCodeIncome) =>
                    val employmentAmount = EmploymentAmount(taxCodeIncome, employment)
                    val (_, date) = retrieveAmountAndDate(employment)
                    val form = EditIncomeForm(employmentAmount, cachedData(1), date.map(_.toString()))
                    Ok(views.html.incomes.confirm_save_Income(form))
                  case _ => throw new RuntimeException(s"Not able to found employment with id $id")
                }
              case _ => throw new RuntimeException("Exception while reading employment and tax code details")
            }
          }

        }
  }

  def viewIncomeForEdit: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          for {
            id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
            employmentAmount <- incomeService.employmentAmount(Nino(user.getNino), id)
          } yield {
            (employmentAmount.isLive, employmentAmount.isOccupationalPension) match {
              case (true, false) => Redirect(routes.IncomeController.regularIncome())
              case (false, false) => Redirect(routes.TaxAccountSummaryController.onPageLoad())
              case _ => Redirect(routes.IncomeController.pensionIncome())
            }
          }
        }
  }

  private def updateEstimatedIncomeSource(onSuccess: String => Result)
                                         (implicit user: TaiUser, hc: HeaderCarrier, request: Request[AnyContent]) = {
    journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_NewAmountKey, UpdateIncome_IdKey).flatMap(cache => {
      val employerName :: newAmount :: id :: Nil = cache.toList

      taxAccountService.updateEstimatedIncome(Nino(user.getNino), FormHelper.stripNumber(newAmount).toInt, TaxYear(), id.toInt) map {
        case TaiSuccessResponse => onSuccess(employerName)
        case _ => throw new RuntimeException("Failed to update estimated income")
      }
    })
  }

  private def retrieveAmountAndDate(employment: Employment): (BigDecimal, Option[LocalDate]) = {
    val amountAndDate = for {
      latestAnnualAccount <- employment.latestAnnualAccount
      latestPayment <- latestAnnualAccount.latestPayment
    } yield Tuple2(latestPayment.amountYearToDate, Some(latestPayment.date))
    amountAndDate.getOrElse(0, None)
  }
}

object IncomeController extends IncomeController with AuthenticationConnectors {
  override val personService = PersonService
  override val taxAccountService = TaxAccountService
  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever = TaiHtmlPartialRetriever
  override val journeyCacheService: JourneyCacheService = JourneyCacheService(UpdateIncome_JourneyKey)
  override val employmentService: EmploymentService = EmploymentService
  override val incomeService: IncomeService = IncomeService
}

