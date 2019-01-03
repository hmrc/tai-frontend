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

package views.html.includes

import controllers.routes
import org.jsoup.Jsoup
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.TaxPeriodLabelService
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class previousYearsSideNavSpec extends TaiViewSpec {
  "PreviousYearsSideNav" should {

    "display Tax year title" in {
      val doc = Jsoup.parse(view.toString)

      doc.select("#previousYearsSideNav").size() mustBe 1
      doc.select("#heading").text() mustBe messages("tai.taxYearHeading")
    }

    "does not display the current selected year" in {
      val doc = Jsoup.parse(view.toString)

      val menu = s"#${currentYear - 1}NavItem"
      doc.select(menu).size() mustBe 0
    }

    "display cy-2 side menu as link" in {
      val doc = Jsoup.parse(view.toString)

      val menu = s"#${currentYear - 2}NavItem"
      doc.select(menu).size() mustBe 1
      doc.select(menu).text() mustBe TaxPeriodLabelService.taxPeriodLabel(currentYear - 2)
      doc.select(s"$menu a").hasAttr("href") mustBe true
      doc.select(s"$menu a").attr("href") mustBe routes.PayeControllerHistoric.payePage(TaxYear(currentYear - 2)).toString
    }

    "display cy-3 side menu as link" in {
      val doc = Jsoup.parse(view.toString)

      val menu = s"#${currentYear - 3}NavItem"
      doc.select(menu).size() mustBe 1
      doc.select(menu).text() mustBe TaxPeriodLabelService.taxPeriodLabel(currentYear - 3)
      doc.select(s"$menu a").hasAttr("href") mustBe true
      doc.select(s"$menu a").attr("href") mustBe routes.PayeControllerHistoric.payePage(TaxYear(currentYear - 3)).toString
    }

  }

  private val currentYear = TaxYearResolver.currentTaxYear

  override def view: Html = views.html.includes.previousYearsSideNav(TaxYear(currentYear - 1), 3)

}
