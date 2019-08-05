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

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.urls.Link

trait ReturnLink {

  def createReturnLink(referer: String, resourceName: String)(implicit messages: Messages): Html = {

    def createLink(message: String, defaultReferer: String)(implicit messages: Messages): Html =
      Link.toInternalPage(url = defaultReferer, value = Some(message)).toHtml

    resourceName match {
      case "tax-free-allowance"           => createLink(messages("tai.iya.tax.free.amount.return.link"), referer)
      case "detailed-income-tax-estimate" => createLink(messages("tai.iya.detailed.paye.return.link"), referer)
      case "your-tax-free-amount"         => createLink(messages("tai.iya.tax.code.change.return.link"), referer)
      case _ =>
        createLink(messages("return.to.your.income.tax.summary"), routes.TaxAccountSummaryController.onPageLoad.url)
    }
  }
}
