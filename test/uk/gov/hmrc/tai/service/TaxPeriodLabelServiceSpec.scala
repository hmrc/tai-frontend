/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service

import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.HtmlFormatter
import utils.BaseSpec

class TaxPeriodLabelServiceSpec extends BaseSpec {

  "TaxPeriodLabelService " should {

    "generate tax period label" in {
      implicit lazy val lang: Lang         = Lang("en")
      implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

      TaxPeriodLabelService.taxPeriodLabel(2017) mustBe s"${HtmlFormatter.htmlNonBroken("6 April 2017")} " +
        s"${messagesApi("language.to")} ${HtmlFormatter.htmlNonBroken("5 April 2018")}"
      TaxPeriodLabelService.taxPeriodLabel(2016) mustBe s"${HtmlFormatter.htmlNonBroken("6 April 2016")} " +
        s"${messagesApi("language.to")} ${HtmlFormatter.htmlNonBroken("5 April 2017")}"
    }

    "generate tax period label in welsh" in {
      implicit lazy val lang: Lang         = Lang("cy")
      implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

      TaxPeriodLabelService.taxPeriodLabel(2017) mustBe s"${HtmlFormatter.htmlNonBroken("6 Ebrill 2017")} " +
        s"${messagesApi("language.to")} ${HtmlFormatter.htmlNonBroken("5 Ebrill 2018")}"
      TaxPeriodLabelService.taxPeriodLabel(2016) mustBe s"${HtmlFormatter.htmlNonBroken("6 Ebrill 2016")} " +
        s"${messagesApi("language.to")} ${HtmlFormatter.htmlNonBroken("5 Ebrill 2017")}"
    }

    "generate single line tax period label" in {
      implicit lazy val lang: Lang         = Lang("en")
      implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

      val year          = TaxYear()
      val message       = s"${langUtils.Dates.formatDate(year.start)} to ${langUtils.Dates.formatDate(year.end)}"
      val expectedLabel = s"${HtmlFormatter.htmlNonBroken(message)}"
      TaxPeriodLabelService.taxPeriodLabelSingleLine(TaxYear().year) mustBe expectedLabel
    }

    "generate single line tax period label in welsh" in {
      implicit lazy val lang: Lang         = Lang("cy")
      implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

      val year          = TaxYear()
      val message       = s"${langUtils.Dates.formatDate(year.start)} i ${langUtils.Dates.formatDate(year.end)}"
      val expectedLabel = s"${HtmlFormatter.htmlNonBroken(message)}"
      TaxPeriodLabelService.taxPeriodLabelSingleLine(TaxYear().year) mustBe expectedLabel
    }
  }

}
