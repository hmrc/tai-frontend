/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.domain.{Employment, UnderPaymentFromPreviousYear}
import uk.gov.hmrc.tai.util.constants.BandTypesConstants
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.TaxAmountDueFromUnderpayment
import uk.gov.hmrc.tai.util.{MonetaryUtil, ViewModelHelper}

case class PreviousYearUnderpaymentViewModel(
  allowanceReducedBy: BigDecimal,
  poundedAmountDue: String,
  returnLink: Html) {}

object PreviousYearUnderpaymentViewModel extends ViewModelHelper with BandTypesConstants with ReturnLink {

  def apply(
    codingComponents: Seq[CodingComponent],
    employments: Seq[Employment],
    totalTax: TotalTax,
    referer: String,
    resourceName: String)(implicit messages: Messages): PreviousYearUnderpaymentViewModel = {

    val allowanceReducedBy = codingComponents
      .collectFirst {
        case CodingComponent(UnderPaymentFromPreviousYear, _, amount, _, _) => amount
      }
      .getOrElse(BigDecimal(0))

    val amountDue = TaxAmountDueFromUnderpayment.amountDue(allowanceReducedBy, totalTax)

    val poundedAmountDue = MonetaryUtil.withPoundPrefix(amountDue.toInt, 2)

    PreviousYearUnderpaymentViewModel(allowanceReducedBy, poundedAmountDue, createReturnLink(referer, resourceName))
  }
}
