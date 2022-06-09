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

import cats.data.EitherT
import cats.implicits._
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.TaiResponse
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.incomeTaxHistory.{IncomeTaxHistoryViewModel, IncomeTaxYear}
import views.html.incomeTaxHistory.IncomeTaxHistoryView

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
  employmentService: EmploymentService,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext, templateRenderer: TemplateRenderer)
    extends TaiBaseController(mcc) {

  private def getIncomeTaxHistorySeq(nino: Nino, taxYear: TaxYear)(
    implicit hc: HeaderCarrier): Future[Either[TaiResponse, Seq[IncomeTaxHistoryViewModel]]] = {
    val futureTaxCodes = taxAccountService.taxCodeIncomesV2(nino, taxYear)
    val futureEmployments = employmentService.employments(nino, taxYear)
    for {
      taxCodeIncomeDetails <- EitherT(futureTaxCodes)
      employmentDetails    <- EitherT.right[TaiResponse](futureEmployments)
    } yield {
      val taxCodes = taxCodeIncomeDetails.groupBy(_.employmentId)
      employmentDetails.flatMap { employment => //TODO ask what to do if there are no taxCodes for a given user
        val maybeTaxCode = taxCodes.get(Some(employment.sequenceNumber)).flatMap(_.headOption)
        maybeTaxCode.map { taxCode =>
          IncomeTaxHistoryViewModel(
            employment.name,
            employment.payeNumber,
            employment.startDate,
            employment.endDate.getOrElse(LocalDate.now()),
            employment.latestAnnualAccount.get.totalIncomeYearToDate.toString(),
            taxCode.amount.toString(),
            taxCode.taxCode
          )
        }
      }
    }
  }.value

  def onPageLoad(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino
    val taxYears = (TaxYear().year to (TaxYear().year - 5) by -1).map(TaxYear(_)).toList

    val x: Future[(List[TaiResponse], List[IncomeTaxYear])] = taxYears
      .traverse { taxYear =>
        EitherT(getIncomeTaxHistorySeq(nino, taxYear)).map { seq =>
          IncomeTaxYear(taxYear, seq.toList)
        }.value
      }
      .map(_.separate)

    for {
      person            <- personService.personDetails(nino)
      incomeTaxYearList <- x
    } yield Ok(incomeTaxHistoryView(config, person, incomeTaxYearList._2))

  }
}
