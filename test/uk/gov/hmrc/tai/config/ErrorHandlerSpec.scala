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

package uk.gov.hmrc.tai.config

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Configuration
import play.api.test.FakeRequest
import utils.BaseSpec
import views.html.{ErrorTemplateNoauth, InternalServerErrorView}

class ErrorHandlerSpec extends BaseSpec {
  implicit val request = FakeRequest()

  lazy val errorHandler: ErrorHandler = new ErrorHandler(
    appConfig,
    inject[ErrorTemplateNoauth],
    messagesApi,
    inject[Configuration],
    inject[InternalServerErrorView]
  )

  "standardTemplate" in {
    def doc: Document =
      Jsoup.parse(
        errorHandler
          .standardErrorTemplate(
            "Sorry, there is a problem with the service",
            "Sorry, there is a problem with the service",
            "Try again later.")(request)
          .toString())
    doc.getElementsByTag("h1").toString must include(messages("tai.technical.error.heading"))
    doc.getElementsByTag("p").toString must include(messages("tai.technical.error.message"))
  }

  "badRequestTemplate" in {
    def doc: Document = Jsoup.parse(errorHandler.badRequestTemplate(request).toString())
    doc.getElementsByTag("h1").toString must include(messages("tai.errorMessage.heading"))
    doc.getElementsByTag("p").toString must include(messages("tai.errorMessage.frontend400.message1"))
    doc.getElementsByTag("p").get(1).toString must include(
      messages(
        "tai.errorMessage.frontend400.message2",
        "<a href=\"#report-name\" class=\"report-error__toggle\"> " + messages("tai.errorMessage.reportAProblem") + " </a> "
      ))
  }

  "notFoundTemplate" in {
    def doc: Document = Jsoup.parse(errorHandler.notFoundTemplate(request).toString())
    doc.getElementsByTag("h1").toString must include(messages("tai.errorMessage.pageNotFound.heading"))
    doc.getElementsByTag("p").get(0).toString must include(messages("tai.errorMessage.pageNotFound.ifYouTyped"))
    doc.getElementsByTag("p").get(1).toString must include(messages("tai.errorMessage.pageNotFound.ifYouPasted"))
    doc.getElementsByTag("p").get(2).toString must include(
      messages(
        "tai.errorMessage.pageNotFound.contactHelpline.text",
        "<a href=\"https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees\"> " + messages(
          "tai.errorMessage.pageNotFound.contactHelpline.link") + " </a>"
      ))
  }

  "internalServerErrorTemplate" in {
    def doc: Document = Jsoup.parse(errorHandler.internalServerErrorTemplate(request).toString())

    doc.getElementsByTag("h1").toString must include(messages("global.error.InternalServerError500.tai.title"))
    doc.getElementsByTag("p").toString must include(
      messages("global.error.InternalServerError500.tai.message.you.can") + " <a href=\"https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees\">" + messages(
        "global.error.InternalServerError500.tai.message.contact.hmrc") + "</a> " + messages(
        "global.error.InternalServerError500.tai.message.by.phone.post"))

  }
}
