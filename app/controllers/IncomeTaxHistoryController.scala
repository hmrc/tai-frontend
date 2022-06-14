/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.implicits._
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService, TaxCodeChangeService}
import uk.gov.hmrc.tai.viewModels.incomeTaxHistory.{IncomeTaxHistoryViewModel, IncomeTaxYear}
import views.html.incomeTaxHistory.IncomeTaxHistoryView
import uk.gov.hmrc.tai.util.ViewModelHelper._
import uk.gov.hmrc.play.views.helpers.MoneyPounds

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxHistoryController @Inject()(
  val config: ApplicationConfig,
  personService: PersonService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  incomeTaxHistoryView: IncomeTaxHistoryView,
  mcc: MessagesControllerComponents,
  taxAccountService: TaxAccountService,
  taxCodeChangeService: TaxCodeChangeService,
  employmentService: EmploymentService,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext, templateRenderer: TemplateRenderer)
    extends TaiBaseController(mcc) {

  private def getIncomeTaxHistoryPreviousYearsSeq(nino: Nino, taxYear: TaxYear)(
    implicit hc: HeaderCarrier): Future[Seq[IncomeTaxHistoryViewModel]] = {
    //YourTaxCodeController.prevTaxCodes
    val futureMaybeTaxCodes = taxCodeChangeService.lastTaxCodeRecordsInYearPerEmployment(nino, taxYear)
    val futureEmployments = employmentService.employments(nino, taxYear)
    for {
      taxCodeIncomeDetails <- futureMaybeTaxCodes
      employmentDetails    <- futureEmployments
    } yield {
      val taxCodesMap = taxCodeIncomeDetails.groupBy(_.payrollNumber)
      employmentDetails.map { employment =>
        val maybeTaxCode = for {
          incomes <- taxCodesMap.get(employment.payrollNumber)
          taxCode <- incomes.headOption
        } yield taxCode

        val maybeLastPayment = fetchLastPayment(employment, taxYear)
        IncomeTaxHistoryViewModel(
          employment.name,
          employment.payeNumber,
          employment.startDate,
          employment.endDate.getOrElse(LocalDate.now()),
          maybeLastPayment.map { payment =>
            withPoundPrefix(MoneyPounds(payment.amountYearToDate, 2, roundUp = false))
          },
          maybeLastPayment.map { payment =>
            withPoundPrefix(MoneyPounds(payment.taxAmountYearToDate, 2, roundUp = false))
          },
          maybeTaxCode.map(_.taxCode)
        )
      }
    }
  }

  private def getIncomeTaxCurrentYear(nino: Nino)(implicit hc: HeaderCarrier): Future[IncomeTaxYear] = {
    val taxYear = TaxYear()
    val futureMaybeTaxCodes = taxAccountService.taxCodeIncomesV2(nino, taxYear).map(_.toOption)
    val futureEmployments = employmentService.employments(nino, taxYear)
    for {
      maybeTaxCodeIncomeDetails <- futureMaybeTaxCodes
      employmentDetails         <- futureEmployments
    } yield {
      val maybeTaxCodesMap = maybeTaxCodeIncomeDetails.map(_.groupBy(_.employmentId))
      val incomeTaxHistory = employmentDetails.map { employment =>
        val maybeTaxCode = for {
          taxCodesMap <- maybeTaxCodesMap
          incomes     <- taxCodesMap.get(Some(employment.sequenceNumber))
          taxCode     <- incomes.headOption
        } yield taxCode

        val maybeLastPayment = fetchLastPayment(employment, taxYear)
        IncomeTaxHistoryViewModel(
          employment.name,
          employment.payeNumber,
          employment.startDate,
          employment.endDate.getOrElse(LocalDate.now()),
          maybeLastPayment.map { payment =>
            withPoundPrefix(MoneyPounds(payment.amountYearToDate, 2, roundUp = false))
          },
          maybeLastPayment.map { payment =>
            withPoundPrefix(MoneyPounds(payment.taxAmountYearToDate, 2, roundUp = false))
          },
          maybeTaxCode.map(_.taxCode)
        )
      }.toList
      IncomeTaxYear(taxYear, incomeTaxHistory)
    }
  }

  //This method follows the pattern set at HistoricIncomeCalculationViewModel.fetchEmploymentAndAnnualAccount
  private def fetchLastPayment(employment: Employment, taxYear: TaxYear) =
    employment.annualAccounts.find(_.taxYear.year == taxYear.year).flatMap(_.payments.lastOption)

  def onPageLoad(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino
    val prevTaxYears = ((TaxYear().year - 1) to (TaxYear().year - config.numberOfPreviousYearsToShow) by -1)
      .map(TaxYear(_))
      .toList

    val futurePreviousTaxYears = prevTaxYears.traverse { taxYear =>
      getIncomeTaxHistoryPreviousYearsSeq(nino, taxYear).map { seq =>
        IncomeTaxYear(taxYear, seq.toList)
      }
    }

    for {
      person            <- personService.personDetails(nino)
      currentTaxYear    <- getIncomeTaxCurrentYear(nino)
      incomeTaxYearList <- futurePreviousTaxYears
    } yield Ok(incomeTaxHistoryView(config, person, currentTaxYear :: incomeTaxYearList))
  }
}
