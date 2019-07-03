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

import javax.inject.Inject
import controllers.actions.ValidatePerson
import controllers.auth._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.{HistoricIncomeCalculationViewModel, YourIncomeCalculationViewModel}

import scala.concurrent.Future

class YourIncomeCalculationController @Inject()(personService: PersonService,
                                                taxAccountService: TaxAccountService,
                                                employmentService: EmploymentService,
                                                authenticate: AuthAction,
                                                validatePerson: ValidatePerson,
                                                override implicit val partialRetriever: FormPartialRetriever,
                                                override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController {

  def yourIncomeCalculationPage(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      incomeCalculationPage(empId, false)
  }

  def printYourIncomeCalculationPage(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      incomeCalculationPage(empId, true)

  }

  private def incomeCalculationPage(empId: Int, printPage: Boolean)(implicit request: AuthenticatedRequest[AnyContent]) = {
    val nino = request.taiUser.nino

    val taxCodeIncomesFuture = taxAccountService.taxCodeIncomes(nino, TaxYear())
    val employmentFuture = employmentService.employment(nino, empId)

    for {
      taxCodeIncomeDetails <- taxCodeIncomesFuture
      employmentDetails <- employmentFuture
    } yield {
      (taxCodeIncomeDetails, employmentDetails) match {
        case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
          val model = YourIncomeCalculationViewModel(taxCodeIncomes.find(_.employmentId.contains(empId)), employment)
          implicit val user = request.taiUser
          if (printPage) {
            Ok(views.html.print.yourIncomeCalculation(model))
          } else {
            Ok(views.html.incomes.yourIncomeCalculation(model))
          }
        case _ => internalServerError("Error while fetching RTI details")
      }
    }
  }

  def yourIncomeCalculationHistoricYears(year: TaxYear, empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request => {
      if (year <= TaxYear().prev) {
        val nino = request.taiUser.nino
        implicit val user = request.taiUser
        showHistoricIncomeCalculation(nino, empId, year = year)
      } else {
        Future.successful(internalServerError(s"yourIncomeCalculationHistoricYears: Doesn't support year $year"))
      }
    }
  }

  def printYourIncomeCalculationHistoricYears(year: TaxYear, empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request => {
      if (year <= TaxYear().prev) {
        val nino = request.taiUser.nino
        implicit val user = request.taiUser

        showHistoricIncomeCalculation(nino, empId, printPage = true, year = year)
      } else {
        Future.successful(internalServerError(s"printYourIncomeCalculationHistoricYears: Doesn't support year $year"))
      }
    }
  }

  private def showHistoricIncomeCalculation(nino: Nino, empId: Int, printPage: Boolean = false, year: TaxYear)
                                           (implicit request: Request[AnyContent], user: AuthedUser): Future[Result] = {
    for {
      employment <- employmentService.employments(nino, year)
    } yield {
      val historicIncomeCalculationViewModel = HistoricIncomeCalculationViewModel(employment, empId, year)
      if (printPage) {
        Ok(views.html.print.historicIncomeCalculation(historicIncomeCalculationViewModel))
      } else {
        Ok(views.html.incomes.historicIncomeCalculation(historicIncomeCalculationViewModel))
      }
    }
  }

}
