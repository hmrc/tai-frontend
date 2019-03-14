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

package uk.gov.hmrc.tai.service.yourTaxFreeAmount

import com.google.inject.Inject
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.TaxCodeChange
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{AllowancesAndDeductionPairs, TaxCodeChangeReasons, IabdTaxCodeChangeReasons}

class TaxCodeChangeReasonsService @Inject()(iabdTaxCodeChangeReasons: IabdTaxCodeChangeReasons,
                                            employmentTaxCodeChangeReasons: TaxCodeChangeReasons) {
  def combineTaxCodeChangeReasons(iabdPairs: AllowancesAndDeductionPairs, taxCodeChange: TaxCodeChange)
                                 (implicit messages: Messages): Seq[String] = {
    val employmentReasons = employmentTaxCodeChangeReasons.reasons(taxCodeChange)
    val benefitReasons = iabdTaxCodeChangeReasons.reasons(iabdPairs)

    val combinedReasons = employmentReasons ++ benefitReasons
    combinedReasons.distinct
  }

  def isAGenericReason(reasons: Seq[String])(implicit messages: Messages): Boolean = {
    val genericTaxCodeReasonMessage = messages("taxCode.change.yourTaxCodeChanged.paragraph")

    val genericReasonsForTaxCodeChange = reasons filter (_ == genericTaxCodeReasonMessage)
    val maxReasonsAllowed = 4

    genericReasonsForTaxCodeChange.nonEmpty ||
      (reasons.size > maxReasonsAllowed || reasons.isEmpty)
  }
}
