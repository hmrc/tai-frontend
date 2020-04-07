/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service

import com.google.inject.Inject
import controllers.TaiBaseController
import play.api.i18n.{Messages, MessagesApi}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, NonTaxCodeIncome, NotLive}
import uk.gov.hmrc.tai.viewModels.{IncomesSources, TaxAccountSummaryViewModel}

import scala.concurrent.{ExecutionContext, Future}

class TaxAccountSummaryService @Inject()(
  trackingService: TrackingService,
  employmentService: EmploymentService,
  taxAccountService: TaxAccountService,
  override val messagesApi: MessagesApi,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController {

  def taxAccountSummaryViewModel(nino: Nino, taxAccountSummary: TaxAccountSummary)(
    implicit hc: HeaderCarrier): Future[TaxAccountSummaryViewModel] =
    for {
      livePensionIncomeSources      <- taxAccountService.incomeSources(nino, TaxYear(), PensionIncome, Live)
      liveEmploymentIncomeSources   <- taxAccountService.incomeSources(nino, TaxYear(), EmploymentIncome, Live)
      ceasedEmploymentIncomeSources <- taxAccountService.incomeSources(nino, TaxYear(), EmploymentIncome, NotLive)
      nonMatchingCeasedEmployments  <- employmentService.ceasedEmployments(nino, TaxYear())
      nonTaxCodeIncome              <- taxAccountService.nonTaxCodeIncomes(nino, TaxYear())
      isAnyFormInProgress           <- trackingService.isAnyIFormInProgress(nino.nino)
    } yield {
      (
        livePensionIncomeSources,
        liveEmploymentIncomeSources,
        ceasedEmploymentIncomeSources,
        nonMatchingCeasedEmployments,
        nonTaxCodeIncome) match {
        case (
            TaiSuccessResponseWithPayload(livePensionIncomeSources: Seq[TaxedIncome]),
            TaiSuccessResponseWithPayload(liveEmploymentIncomeSources: Seq[TaxedIncome]),
            TaiSuccessResponseWithPayload(ceasedEmploymentIncomeSources: Seq[TaxedIncome]),
            nonMatchingCeasedEmployments: Seq[Employment],
            TaiSuccessResponseWithPayload(nonTaxCodeIncome: NonTaxCodeIncome)) =>
          TaxAccountSummaryViewModel(
            taxAccountSummary,
            isAnyFormInProgress,
            nonTaxCodeIncome,
            IncomesSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
            nonMatchingCeasedEmployments
          )
        case _ => throw new RuntimeException("Failed to fetch income details")
      }
    }

}
