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

import controllers.ServiceChecks.CustomRule
import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTai}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model.{IncomeCalculation, SessionData}
import uk.gov.hmrc.tai.service.TaiService.IncomeIDPage
import uk.gov.hmrc.tai.service.{ActivityLoggerService, EmploymentService, JourneyCacheService, TaiService}
import uk.gov.hmrc.tai.util.{FormHelper, JourneyCacheConstants, TaxSummaryHelper}
import views.html.incomes.howToUpdate

import scala.concurrent.Future

trait IncomeUpdateCalculatorController extends TaiBaseController
    with DelegationAwareActions
    with WithAuthorisedForTai
    with Auditable
    with JourneyCacheConstants {

  def taiService: TaiService
  def journeyCacheService: JourneyCacheService
  def employmentService: EmploymentService
  def activityLoggerService: ActivityLoggerService

  def howToUpdatePage(id: Int) = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val rule: CustomRule = details => {
      employmentService.employment(Nino(user.getNino), id) flatMap {
        case Some(employment) =>
          for {
            _ <- taiService.updateTaiSession(sessionData.copy(editIncomeForm = Some(EditIncomeForm(employment.name, "", id))))
            _ <- journeyCacheService.cache(Map(UpdateIncome_NameKey -> employment.name, UpdateIncome_IdKey -> id.toString))
            result <- getChooseHowToUpdatePage(Nino(user.getNino), id, employment.name)
          } yield result

        case None => throw new RuntimeException("Not able to find employment")
      }
    }

    ServiceChecks.executeWithServiceChecks(Nino(user.getNino), SimpleServiceCheck, sessionData) {
      Some(rule)
    }
  }


  def chooseHowToUpdatePage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id2, name) => getChooseHowToUpdatePage(Nino(user.getNino), id2, name)
    moveToPageWithIncomeID(page)
  }

  def getChooseHowToUpdatePage(nino: Nino, id: Int, employerName: String)(implicit request: Request[AnyContent], user: TaiUser, sessionData: SessionData): Future[Result] = {
    sendActingAttorneyAuditEvent("getChooseHowToUpdatePage")
    val rule: CustomRule = details => {
      val incomeToEdit = taiService.incomeForEdit(details, id)

      incomeToEdit match {
        case Some(employmentAmount) => {
          (employmentAmount.isLive, employmentAmount.isOccupationalPension) match {
            case (true, false) => {
              if (!TaxSummaryHelper.hasMultipleIncomes(details)) {
                TaxSummaryHelper.getSingularIncomeId(details) match {
                  case Some(incomeId) => Future.successful(Ok(views.html.incomes.howToUpdate(HowToUpdateForm.createForm(), TaxSummaryHelper.hasMultipleIncomes(details), incomeId)))
                  case None => Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
                }
              } else {
                Future.successful(Ok(howToUpdate(HowToUpdateForm.createForm(), TaxSummaryHelper.hasMultipleIncomes(details), id, Some(employerName))))
              }
            }
            case (false, false) => Future.successful(Redirect(routes.TaxAccountSummaryController.onPageLoad()))
            case _ => Future.successful(Redirect(routes.IncomeControllerNew.pensionIncome()))
          }
        }
        case _ => {
          Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
        }
      }

    }

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("getChooseHowToUpdatePage", nino)

  def handleChooseHowToUpdate() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => processChooseHowToUpdate(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def processChooseHowToUpdate(nino: Nino, id: Int, name: String)(
    implicit request: Request[AnyContent], user: TaiUser, sessionData: SessionData ): Future[Result] = {
    sendActingAttorneyAuditEvent("processChooseHowToUpdate")

    val rule: CustomRule = details => {
      HowToUpdateForm.createForm().bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.incomes.howToUpdate(formWithErrors, TaxSummaryHelper.hasMultipleIncomes(details), id, Some(name))))
        },
        formData => {
          formData.howToUpdate match {
            case Some("incomeCalculator") => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.workingHoursPage()))
            case _ => Future.successful(Redirect(routes.IncomeController.viewIncomeForEdit()))
          }
        }
      )
    }

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("processChooseHowToUpdate", nino)

  def workingHoursPage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => getWorkingHoursPage(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def getWorkingHoursPage(nino: Nino, id: Int, name: String)(
    implicit request: Request[AnyContent], user: TaiUser, sessionData: SessionData ): Future[Result] = {
    sendActingAttorneyAuditEvent("getWorkingHours")

    val rule: CustomRule = details => {
      Future.successful(Ok(views.html.incomes.workingHours(HoursWorkedForm.createForm(), id: Int, employerName = Some(name))))
    }

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("getWorkingHours", nino)

  def handleWorkingHours() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => processWorkingHours(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def processWorkingHours(nino: Nino, id: Int, name: String)(
    implicit request: Request[AnyContent], user: TaiUser, sessionData: SessionData): Future[Result] = {

    sendActingAttorneyAuditEvent("processWorkedHours")
    val rule: CustomRule = details => {
      HoursWorkedForm.createForm().bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.incomes.workingHours(formWithErrors, id: Int, employerName = Some(name))))
        },
        formData => {
          formData.workingHours match {
            case Some("same") => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.payPeriodPage()))
            case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.calcUnavailablePage()))
          }
        }
      )
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("processWorkingHours", nino)

  def payPeriodPage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => getPayPeriodPage(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def getPayPeriodPage(nino: Nino, id: Int, employerName: String)(
    implicit request: Request[AnyContent], user: TaiUser, sessionData: SessionData ): Future[Result] = {
    sendActingAttorneyAuditEvent("getPayPeriodPage")

    val rule: CustomRule = details => {
      Future.successful(Ok(views.html.incomes.payPeriod(PayPeriodForm.createForm(None), id: Int, employerName = Some(employerName))))
    }

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("getPayPeriodPage", nino)

  def handlePayPeriod() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => processPayPeriod(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def processPayPeriod(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("processPayPeriod")
    val rule: CustomRule = details => {
      val payPeriod: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("payPeriod").flatMap(_.headOption))

      PayPeriodForm.createForm(None, payPeriod).bindFromRequest().fold(
        formWithErrors => {
          val isNotDaysError = formWithErrors.errors.filter { error => error.key == "otherInDays" }.isEmpty
          Future.successful(BadRequest(views.html.incomes.payPeriod(formWithErrors, id: Int, isNotDaysError, employerName = Some(employerName))))
        },
        formData => {
          taiService.updateTaiSession(
            sessionData.copy(
              incomeCalculation = Some(
                IncomeCalculation(incomeId = Some(id), payPeriodForm = Some(formData))
              )
            )
          ).map { x =>
              Redirect(routes.IncomeUpdateCalculatorController.payslipAmountPage())
            }
        }
      )
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("processPayPeriod", nino)

  //Payslip Amount
  def payslipAmountPage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => getPayslipAmountPage(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def getPayslipAmountPage(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("getPayslipAmountPage")

    val rule: CustomRule = details => {
      sessionData.incomeCalculation.map { data =>
        Future.successful(Ok(views.html.incomes.payslipAmount(
          PayslipForm.createForm(),
          data.payPeriodForm.map(_.payPeriod.getOrElse("")).getOrElse(""),
          id: Int, employerName = Some(employerName)
        )))
      }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("getPayslipAmountPage", nino)

  def handlePayslipAmount() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => processPayslipAmount(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def processPayslipAmount(nino: Nino, id: Int, employerName: String)(implicit
    request: Request[AnyContent],
    user: TaiUser, sessionData: SessionData): Future[Result] = {

    sendActingAttorneyAuditEvent("processPayslipAmount")
    val rule: CustomRule = details => {
      PayslipForm.createForm().bindFromRequest().fold(
        formWithErrors => {
          sessionData.incomeCalculation.map { data =>
            Future.successful(BadRequest(views.html.incomes.payslipAmount(
              formWithErrors,
              data.payPeriodForm.map(_.payPeriod.getOrElse("")).getOrElse(""),
              id: Int, employerName = Some(employerName)
            )))
          }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
        },
        formData => {
          sessionData.incomeCalculation.map { data =>
            if (data.incomeId.isDefined && data.incomeId.getOrElse(0) == id) {
              taiService.updateTaiSession(sessionData.copy(incomeCalculation = Some(data.copy(payslipForm = Some(formData))))).map { x =>
                Redirect(routes.IncomeUpdateCalculatorController.payslipDeductionsPage())
              }
            } else {
              Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
            }
          }
        }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
      )
    }

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("processPayslipAmount", nino)

  //Taxable Payslip Amount
  def taxablePayslipAmountPage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => getTaxablePayslipAmountPage(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def getTaxablePayslipAmountPage(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("getTaxablePayslipAmountPage")

    val rule: CustomRule = details => {
      sessionData.incomeCalculation.map { data =>
        Future.successful(Ok(views.html.incomes.taxablePayslipAmount(
          TaxablePayslipForm.createForm(),
          data.payPeriodForm.map(_.payPeriod.getOrElse("")).getOrElse(""),
          id: Int, employerName = Some(employerName)
        )))
      }
    }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("getTaxablePayslipAmountPage", nino)

  def handleTaxablePayslipAmount() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => processTaxablePayslipAmount(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def processTaxablePayslipAmount(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {
    sendActingAttorneyAuditEvent("processTaxablePayslipAmount")

    val rule: CustomRule = details => {

      val totalSalary = FormHelper.stripNumber(sessionData.incomeCalculation.flatMap { _.payslipForm.flatMap { _.totalSalary } })
      TaxablePayslipForm.createForm(totalSalary).bindFromRequest().fold(
        formWithErrors => {
          sessionData.incomeCalculation.map { data =>
            Future.successful(BadRequest(views.html.incomes.taxablePayslipAmount(
              formWithErrors,
              data.payPeriodForm.map(_.payPeriod.getOrElse("")).getOrElse(""),
              id: Int, employerName = Some(employerName)
            )))
          }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
        },
        formData => {
          sessionData.incomeCalculation.map { data =>
            if (data.incomeId.isDefined && data.incomeId.getOrElse(0) == id) {
              taiService.updateTaiSession(sessionData.copy(incomeCalculation = Some(data.copy(taxablePayslipForm = Some(formData))))).map { x =>
                Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage())
              }
            } else {
              Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
            }
          }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
        }
      )
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("processTaxablePayslipAmount", nino)

  def payslipDeductionsPage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => getPayslipDeductionsPage(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def getPayslipDeductionsPage(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("getPayslipDeductionsPage")

    val rule: CustomRule = details => {
      Future.successful(Ok(views.html.incomes.payslipDeductions(PayslipDeductionsForm.createForm(), id: Int, employerName = Some(employerName))))
    }

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("getPaySlipDeductionsPage", nino)

  def handlePayslipDeductions() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => processPayslipDeductions(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def processPayslipDeductions(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("processPayslipDeductions")

    val rule: CustomRule = details => {

      PayslipDeductionsForm.createForm().bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.incomes.payslipDeductions(formWithErrors, id: Int, employerName = Some(employerName))))
        },
        formData => {
          sessionData.incomeCalculation.map { data =>
            if (data.incomeId.isDefined && data.incomeId.getOrElse(0) == id) {
              taiService.updateTaiSession(sessionData.copy(incomeCalculation = Some(data.copy(payslipDeductionsForm = Some(formData))))).map { x =>

                formData.payslipDeductions match {
                  case Some("Yes") => Redirect(routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage())
                  case _ => Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage())
                }

              }
            } else {
              Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
            }
          }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
        }
      )
    }

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("processPayslipDeductions", nino)

  def bonusPaymentsPage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => getBonusPaymentsPage(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def getBonusPaymentsPage(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("getBonusPaymentsPage")

    val rule: CustomRule = details => {

      sessionData.incomeCalculation.map { data =>
        val payslipDeductions = (data.payslipDeductionsForm.map(_.payslipDeductions).getOrElse("") == "Yes")
        if (data.incomeId.isDefined && data.incomeId.getOrElse(0) == id) {
          Future.successful(Ok(views.html.incomes.bonusPayments(BonusPaymentsForm.createForm(), id: Int, payslipDeductions, employerName = Some(employerName))))
        } else {
          Future.successful(Ok(views.html.incomes.bonusPayments(BonusPaymentsForm.createForm(), id: Int, payslipDeductions, employerName = Some(employerName))))
        }
      }
    }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("getBonusPaymentsPage", nino)

  def handleBonusPayments() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => processBonusPayments(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def processBonusPayments(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("processBonusPayments")

    val rule: CustomRule = details => {

      val bonusPayments: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("bonusPayments").flatMap(_.headOption))
      val bonusPaymentsSelected = (bonusPayments == Some("Yes"))
      BonusPaymentsForm.createForm(bonusPayments = bonusPayments).bindFromRequest().fold(
        formWithErrors => {
          sessionData.incomeCalculation.map { data =>
            val payslipDeductions = (data.payslipDeductionsForm.map(_.payslipDeductions).getOrElse("") == "Yes")
            Future.successful(
              BadRequest(
                views.html.incomes.bonusPayments(
                  formWithErrors,
                  id: Int,
                  payslipDeductions,
                  bonusPaymentsSelected,
                  employerName = Some(employerName)
                )
              )
            )
          }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
        },
        formData => {
          sessionData.incomeCalculation.map { data =>
            if (data.incomeId.isDefined && data.incomeId.getOrElse(0) == id) {
              taiService.updateTaiSession(sessionData.copy(incomeCalculation = Some(data.copy(bonusPaymentsForm = Some(formData))))).map { x =>
                formData.bonusPayments match {
                  case Some("Yes") => {
                    Redirect(routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage())
                  }
                  case _ => {
                    Redirect(routes.IncomeUpdateCalculatorController.estimatedPayPage)
                  }
                }
              }
            } else {
              Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
            }
          }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
        }
      )
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }
  } recoverWith handleErrorResponse("processBonusPayments", nino)

  //BONUS OVERTIME AMOUNT PAGE
  def bonusOvertimeAmountPage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => getBonusOvertimeAmountPage(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }
  def getBonusOvertimeAmountPage(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("getBonusOvertimeAmountPage")

    val rule: CustomRule = details => {
      sessionData.incomeCalculation.map { form =>
        if (form.incomeId.isDefined && form.incomeId.getOrElse(0) == id) {

          val moreThisYear = form.bonusPaymentsForm.map(form => form.bonusPaymentsMoreThisYear.getOrElse("")).getOrElse("")
          if (moreThisYear == "Yes") {
            Future.successful(Ok(views.html.incomes.bonusPaymentAmount(BonusOvertimeAmountForm.createForm(), "year", id, employerName = Some(employerName))))
          } else {
            Future.successful(
              Ok(
                views.html.incomes.bonusPaymentAmount(
                  BonusOvertimeAmountForm.createForm(),
                  form.payPeriodForm.map(_.payPeriod.getOrElse("")).getOrElse(""),
                  id,
                  employerName = Some(employerName)
                )
              )
            )
          }

        } else {
          Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
        }
      }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }
  } recoverWith handleErrorResponse("getBonusOvertimeAmountPage", nino)

  def handleBonusOvertimeAmount() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => processBonusOvertimeAmount(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def processBonusOvertimeAmount(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent], user: TaiUser, sessionData: SessionData
  ): Future[Result] = {
    sendActingAttorneyAuditEvent("processBonusOvertimeAmount")
    val rule: CustomRule = details => {
      sessionData.incomeCalculation.map { data =>
        val moreThisYear = data.bonusPaymentsForm.map(form => form.bonusPaymentsMoreThisYear.getOrElse("")).getOrElse("")
        def fetchNonEmptyMessage = moreThisYear match {
          case "Yes" => Messages("tai.bonusPaymentsAmount.year.error")
          case _ => data.payPeriodForm.flatMap(_.payPeriod) match {
            case Some("monthly") => "tai.bonusPaymentsAmount.month.error"
            case Some("fortnightly") => "tai.bonusPaymentsAmount.fortnightly.error"
            case Some("weekly") => "tai.bonusPaymentsAmount.week.error"
            case _ => "tai.bonusPaymentsAmount.period.error"
          }
        }

        val nonEmptyMessage = fetchNonEmptyMessage
        val notAnAmountMessage = moreThisYear match {
          case "Yes" => "tai.bonusPaymentsAmount.error.form.notAnAmountAnnual"
          case _ => "tai.bonusPaymentsAmount.error.form.notAnAmount"
        }
        BonusOvertimeAmountForm.createForm(Some(nonEmptyMessage), Some(notAnAmountMessage)).bindFromRequest().fold(
          formWithErrors => {
            if (moreThisYear == "Yes") {
              Future.successful(BadRequest(views.html.incomes.bonusPaymentAmount(formWithErrors, "year", id, employerName = Some(employerName))))
            } else {
              Future.successful(
                BadRequest(
                  views.html.incomes.bonusPaymentAmount(
                    formWithErrors,
                    data.payPeriodForm.map(_.payPeriod.getOrElse("")).getOrElse(""),
                    id, employerName = Some(employerName)
                  )
                )
              )
            }
          },
          formData => {
            if (data.incomeId.isDefined && data.incomeId.getOrElse(0) == id) {
              taiService.updateTaiSession(sessionData.copy(incomeCalculation = Some(data.copy(bonusOvertimeAmountForm = Some(formData))))).map { x =>
                Redirect(routes.IncomeUpdateCalculatorController.estimatedPayPage)
              }
            } else {
              Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
            }
          }
        )
      }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }
  } recoverWith handleErrorResponse("processTaxablePayslipAmount", nino)

  def estimatedPayPage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => getEstimatedPayPage(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def getEstimatedPayPage(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("getEstimatedPayPage")

    val incomeCalculationForm = sessionData.incomeCalculation
    val rule: CustomRule = details => {
      incomeCalculationForm.map { form =>

        val bonusOvertime = form.bonusPaymentsForm.flatMap(_.bonusPayments).getOrElse("") == "Yes"

        if (form.incomeId.isDefined && form.incomeId.getOrElse(0) == id) {

          val income = taiService.incomeForEdit(details, id)
          val startDate = income.map(_.startDate).flatten

          taiService.calculateEstimatedPay(form, startDate).flatMap { CalculatedPay =>
            val newAmount = CalculatedPay.grossAnnualPay
            val employmentId = income.get.employmentId
            val payYTD = TaxSummaryHelper.getTaxablePayYTD(details, income.get.employmentId)
            val paymentDate = details.incomeData.map(_.incomeExplanations).getOrElse(Nil).find(_.incomeId == id).flatMap(_.paymentDate)

            if (CalculatedPay.grossAnnualPay.get > payYTD) {

              taiService.updateTaiSession(sessionData.copy(incomeCalculation = Some(form.copy(
                grossAmount = CalculatedPay.grossAnnualPay,
                netAmount = CalculatedPay.netAnnualPay
              )))).map { x =>

                Ok(views.html.incomes.estimatedPay(CalculatedPay.grossAnnualPay, CalculatedPay.netAnnualPay, id, bonusOvertime,
                  CalculatedPay.annualAmount, CalculatedPay.startDate, (CalculatedPay.grossAnnualPay == CalculatedPay.netAnnualPay),
                  employerName = Some(employerName)))
              }
            } else {

              Future.successful(Ok(views.html.incomes.incorrectTaxableIncome(payYTD, paymentDate.getOrElse(new LocalDate))))
            }
          }

        } else {
          Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
        }

      }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
    }

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("getEstimatedPayPage", nino)

  def handleCalculationResult() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => processCalculationResult(Nino(user.taiRoot.nino), id, name)
    moveToPageWithIncomeID(page)
  }

  def processCalculationResult(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("processCalculationResult")

    val rule: CustomRule = details => {
      val incomeToEdit = taiService.incomeForEdit(details, id)
      val readIncomeCalculationForm = sessionData.incomeCalculation

      readIncomeCalculationForm.map { incomeCalculationForm =>
        if (incomeCalculationForm.incomeId.isDefined && incomeCalculationForm.incomeId.getOrElse(0) == id) {
          incomeToEdit.map { income =>
            {
              val newAmount = income.copy(newAmount = if (incomeCalculationForm.netAmount.isDefined) {
                incomeCalculationForm.netAmount.getOrElse(BigDecimal(0)).intValue()
              } else {
                income.oldAmount
              })

              Future.successful(Ok(views.html.incomes.confirm_save_Income(
                EditIncomeForm.create(preFillData = newAmount).get,
                TaxSummaryHelper.hasMultipleIncomes(details), employerName = Some(employerName), true
              )))
            }
          }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
        } else {
          Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None)))
        }
      }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }
  } recoverWith handleErrorResponse("processCalculationResult", nino)

  def calcUnavailablePage() = authorisedForTai(redirectToOrigin = true)(taiService).async { implicit user => implicit sessionData => implicit request =>
    val page: IncomeIDPage = (id, name) => getCalcUnavailablePage(Nino(user.getNino), id, name)
    moveToPageWithIncomeID(page)
  }

  def getCalcUnavailablePage(nino: Nino, id: Int, employerName: String)(
    implicit
    request: Request[AnyContent],
    user: TaiUser,
    sessionData: SessionData
  ): Future[Result] = {

    sendActingAttorneyAuditEvent("getCalcUnavailable")
    val rule: CustomRule = details => {
      Future.successful(Ok(views.html.incomes.calcUnavailable(id, Some(employerName))))
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }
  } recoverWith handleErrorResponse("getCalcUnavailable", nino)

  def moveToPageWithIncomeID(page: IncomeIDPage)(implicit user: TaiUser, request: Request[AnyContent], sessionData: SessionData): Future[Result] = {
    sessionData.editIncomeForm.map(x => (x.employmentId, x.name)).map { income =>
      for{
        _ <- journeyCacheService.cache(Map(UpdateIncome_IdKey -> income._1.toString, UpdateIncome_NameKey -> income._2))
        result <- page(income._1, income._2)
      } yield result
    }
  }.getOrElse(Future.successful(Redirect(routes.YourIncomeCalculationController.yourIncomeCalculationPage(None))))
}

object IncomeUpdateCalculatorController extends IncomeUpdateCalculatorController with AuthenticationConnectors {
  override val taiService = TaiService
  override val activityLoggerService = ActivityLoggerService
  override val journeyCacheService = JourneyCacheService(UpdateIncome_JourneyKey)
  override val employmentService = EmploymentService
  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: PartialRetriever = TaiHtmlPartialRetriever

}
