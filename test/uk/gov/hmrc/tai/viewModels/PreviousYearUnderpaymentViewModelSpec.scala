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

package uk.gov.hmrc.tai.viewModels

import controllers.routes
import uk.gov.hmrc.tai.model.domain.{GiftAidAdjustment, UnderPaymentFromPreviousYear}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import utils.BaseSpec
import views.html.includes.link

class PreviousYearUnderpaymentViewModelSpec extends BaseSpec {

  "PreviousYearUnderpaymentViewModel apply method" when {

    "UnderPaymentFromPreviousYear is present should contain the correct values from the CodingComponent" in {

      val codingComponents =
        Seq(CodingComponent(UnderPaymentFromPreviousYear, Some(1), 500.00, "UnderPaymentFromPreviousYear", Some(123)))

      val result = PreviousYearUnderpaymentViewModel(codingComponents, "", "")

      result.allowanceReducedBy mustEqual 500.00
      result.poundedAmountDue mustEqual "£123.00"

      result.returnLink mustBe link(
        url = routes.TaxAccountSummaryController.onPageLoad.url,
        copy = messagesApi("return.to.your.income.tax.summary"))
    }
  }
}
