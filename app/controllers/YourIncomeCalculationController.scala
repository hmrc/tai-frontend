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

import com.google.inject.Inject
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
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
                                                val taxAccountService: TaxAccountService,
                                                employmentService: EmploymentService,
                                                val delegationConnector: DelegationConnector,
                                                val authConnector: AuthConnector,
                                                override implicit val partialRetriever: FormPartialRetriever,
                                                override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite {

  def yourIncomeCalculationPage(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            implicit val messages = Messages.Implicits.applicationMessages
            incomeCalculationPage(empId, false)
          }
  }

  def printYourIncomeCalculationPage(empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            implicit val messages = Messages.Implicits.applicationMessages
            incomeCalculationPage(empId, true)
          }
  }

  private def incomeCalculationPage(empId: Int, printPage: Boolean)(implicit request: Request[AnyContent], user: TaiUser, person: Person, messages: Messages) = {
    val taxCodeIncomesFuture = taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear())
    val employmentFuture = employmentService.employment(Nino(user.getNino), empId)

    for {
      taxCodeIncomeDetails <- taxCodeIncomesFuture
      employmentDetails <- employmentFuture
    } yield {
      (taxCodeIncomeDetails, employmentDetails) match {
        case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
          val model = YourIncomeCalculationViewModel(taxCodeIncomes.find(_.employmentId.contains(empId)), employment)(messages)
          if (printPage) {
            Ok(views.html.print.yourIncomeCalculation(model))
          } else {
            Ok(views.html.incomes.yourIncomeCalculation(model))
          }
        case _ => throw new RuntimeException("Error while fetching RTI details")
      }
    }
  }

  def yourIncomeCalculationHistoricYears(year: TaxYear, empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request => {
          ServiceCheckLite.personDetailsCheck {
            implicit val messages = Messages.Implicits.applicationMessages
            if (year <= TaxYear().prev) {
              showHistoricIncomeCalculation(Nino(user.getNino), empId, year = year)
            } else {
              Future.failed(throw new BadRequestException(s"Doesn't support year $year"))
            }
          }
        }
  }

  def printYourIncomeCalculationHistoricYears(year: TaxYear, empId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request => {
          ServiceCheckLite.personDetailsCheck {
            implicit val messages = Messages.Implicits.applicationMessages
            if (year <= TaxYear().prev) {
              showHistoricIncomeCalculation(Nino(user.getNino), empId, printPage = true, year = year)
            } else {
              Future.failed(throw new BadRequestException(s"Doesn't support year $year"))
            }
          }
        }
  }

  private def showHistoricIncomeCalculation(nino: Nino, empId: Int, printPage: Boolean = false, year: TaxYear)
                                           (implicit request: Request[AnyContent], user: TaiUser, person: Person, messages: Messages): Future[Result] = {
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
