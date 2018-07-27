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

package views.html.taxCodeChange

import controllers.routes
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class YourTaxFreeAmountViewSpec extends TaiViewSpec{

  "your tax free amount" should {
    behave like pageWithBackLink
  }

  "have a 'check what happens next' button" in {

    doc must haveLinkElement(
      "checkWhatHappensNext",
      routes.TaxCodeChangeController.whatHappensNext.url,
      messages("taxCode.change.yourTaxFreeAmount.whatHappensNext.link"))
  }


  override def view = views.html.taxCodeChange.yourTaxFreeAmount()
}
