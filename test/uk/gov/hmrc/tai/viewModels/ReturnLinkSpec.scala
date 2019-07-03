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

package uk.gov.hmrc.tai.viewModels

import controllers.{FakeTaiPlayApplication, routes}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.urls.Link

class ReturnLinkSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  class ReturnLinkTest extends ReturnLink {}
  def createReturnLinkTest = new ReturnLinkTest
  val referalPath = "http://somelocation/anotherLocation"

  "createReturnLink" must {
    "create the matching referer's link" when {
      "refer is tax-free-amount" in {
        val returnLink = createReturnLinkTest

        val result= returnLink.createReturnLink(referalPath, resourceName = "tax-free-allowance")

        result mustBe Link.toInternalPage(referalPath, Some(messagesApi("tai.iya.tax.free.amount.return.link"))).toHtml
      }

      "refer is detailed-income-tax-estimate" in {
        val returnLink = createReturnLinkTest

        val result= returnLink.createReturnLink(referalPath, resourceName = "detailed-income-tax-estimate")

        result mustBe Link.toInternalPage(referalPath, Some(messagesApi("tai.iya.detailed.paye.return.link"))).toHtml
      }

      "refer is your-tax-free-amount" in {
        val returnLink = createReturnLinkTest

        val result= returnLink.createReturnLink(referalPath, resourceName = "your-tax-free-amount")

        result mustBe Link.toInternalPage(referalPath, Some(messagesApi("tai.iya.tax.code.change.return.link"))).toHtml
      }

      "refer is anything else" in {
        val returnLink = createReturnLinkTest

        val result= returnLink.createReturnLink(referalPath, resourceName = "anything else")

        result mustBe Link.toInternalPage(routes.TaxAccountSummaryController.onPageLoad.url, Some(messagesApi("return.to.your.income.tax.summary"))).toHtml
      }
    }
  }
}
