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
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary

class PreviousYearUnderpaymentViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "PreviousYearUnderpaymentViewModel apply method" must {

    "return an instance with a shouldHavePaid drawn from the totalEstimatedTax value of the supplied TaxAccountSummary" in {
      PreviousYearUnderpaymentViewModel(taxAccountSummary).shouldHavePaid mustEqual 1000.00
      PreviousYearUnderpaymentViewModel(taxAccountSummary).actuallyPaid mustEqual 900.00
      PreviousYearUnderpaymentViewModel(taxAccountSummary).allowanceReducedBy mustEqual 500.00
      PreviousYearUnderpaymentViewModel(taxAccountSummary).amountDue mustEqual 100.00  // from totalin year adjustment
    }
  }






  val taxAccountSummary = TaxAccountSummary(1000.00, 11850.00, 140.00, 0,0)

}
