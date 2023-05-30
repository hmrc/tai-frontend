/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.implicits._
import com.google.inject.Inject
import controllers.TaiBaseController
import play.api.i18n.Messages
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, NotLive}
import uk.gov.hmrc.tai.model.{IncomeSources, TaxYear}
import uk.gov.hmrc.tai.viewModels.TaxAccountSummaryViewModel

import scala.concurrent.{ExecutionContext, Future}

class TaxAccountSummaryService @Inject() (
  trackingService: TrackingService,
  employmentService: EmploymentService,
  taxAccountService: TaxAccountService,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def taxAccountSummaryViewModel(nino: Nino, taxAccountSummary: TaxAccountSummary)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[TaxAccountSummaryViewModel] =
    (
      taxAccountService.incomeSources(nino, TaxYear(), PensionIncome, Live),
      taxAccountService.incomeSources(nino, TaxYear(), EmploymentIncome, Live),
      taxAccountService.incomeSources(nino, TaxYear(), EmploymentIncome, NotLive),
      employmentService.ceasedEmployments(nino, TaxYear()).getOrElse(Seq.empty[Employment]),
      taxAccountService.nonTaxCodeIncomes(nino, TaxYear()),
      trackingService.isAnyIFormInProgress(nino.nino)
    ).mapN {
      case (
            livePensionIncomeSources,
            liveEmploymentIncomeSources,
            ceasedEmploymentIncomeSources,
            nonMatchingCeasedEmployments,
            nonTaxCodeIncome,
            isAnyFormInProgress
          ) =>
        TaxAccountSummaryViewModel(
          taxAccountSummary,
          isAnyFormInProgress,
          nonTaxCodeIncome,
          IncomeSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
          nonMatchingCeasedEmployments
        )
    } recover {
      case e: UnauthorizedException => throw e
      case _                        => throw new RuntimeException("Failed to fetch income details")
    }
}
