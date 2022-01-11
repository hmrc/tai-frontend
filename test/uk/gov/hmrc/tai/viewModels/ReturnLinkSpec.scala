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
import utils.BaseSpec
import views.html.includes.link

class ReturnLinkSpec extends BaseSpec {

  class ReturnLinkTest extends ReturnLink {}
  def createReturnLinkTest = new ReturnLinkTest
  val referalPath = "http://somelocation/anotherLocation"

  "createReturnLink" must {
    "create the matching referer's link" when {
      "refer is tax-free-amount" in {
        val returnLink = createReturnLinkTest

        val result = returnLink.createReturnLink(referalPath, resourceName = "tax-free-allowance")

        result mustBe link(url = referalPath, copy = messagesApi("tai.iya.tax.free.amount.return.link"))
      }

      "refer is detailed-income-tax-estimate" in {
        val returnLink = createReturnLinkTest

        val result = returnLink.createReturnLink(referalPath, resourceName = "detailed-income-tax-estimate")

        result mustBe link(url = referalPath, copy = messagesApi("tai.iya.detailed.paye.return.link"))
      }

      "refer is your-tax-free-amount" in {
        val returnLink = createReturnLinkTest

        val result = returnLink.createReturnLink(referalPath, resourceName = "your-tax-free-amount")

        result mustBe link(url = referalPath, copy = messagesApi("tai.iya.tax.code.change.return.link"))
      }

      "refer is anything else" in {
        val returnLink = createReturnLinkTest

        val result = returnLink.createReturnLink(referalPath, resourceName = "anything else")

        result mustBe link(
          url = routes.TaxAccountSummaryController.onPageLoad.url,
          copy = messagesApi("return.to.your.income.tax.summary"))
      }
    }
  }
}
