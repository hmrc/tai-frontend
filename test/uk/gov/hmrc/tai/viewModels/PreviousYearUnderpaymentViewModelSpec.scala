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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain.{EstimatedTaxYouOweThisYear, UnderPaymentFromPreviousYear}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

class PreviousYearUnderpaymentViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "PreviousYearUnderpaymentViewModel apply method" must {

    "return an instance with a shouldHavePaid drawn from the totalEstimatedTax value of the supplied TaxAccountSummary" in {
      PreviousYearUnderpaymentViewModel(codingComponents, actuallyPaid).allowanceReducedBy mustEqual 500.00
      PreviousYearUnderpaymentViewModel(codingComponents, actuallyPaid).shouldHavePaid mustEqual 1000.00
      PreviousYearUnderpaymentViewModel(codingComponents, actuallyPaid).actuallyPaid mustEqual 900.00
      PreviousYearUnderpaymentViewModel(codingComponents, actuallyPaid).amountDue mustEqual 100.00
    }
  }


  val actuallyPaid = 900.00

  val codingComponents = Seq(
    CodingComponent(UnderPaymentFromPreviousYear, Some(1), 500.00, "UnderPaymentFromPreviousYear"),
    CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 33.44, "EstimatedTaxYouOweThisYear")
  )
}
