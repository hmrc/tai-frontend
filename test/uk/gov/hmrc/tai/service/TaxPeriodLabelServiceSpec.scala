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

package uk.gov.hmrc.tai.service

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.util.HtmlFormatter

import scala.util.matching.Regex

class TaxPeriodLabelServiceSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport{

  "TaxPeriodLabelService " should {

    "generate tax period label" in {
      TaxPeriodLabelService.taxPeriodLabel(2017) mustBe HtmlFormatter.htmlNonBroken("6 April 2017") + " to " + HtmlFormatter.htmlNonBroken("5 April 2018")
      TaxPeriodLabelService.taxPeriodLabel(2016) mustBe HtmlFormatter.htmlNonBroken("6 April 2016") + " to " + HtmlFormatter.htmlNonBroken("5 April 2017")
    }
  }

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

}
