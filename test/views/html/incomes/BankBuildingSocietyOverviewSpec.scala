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

package views.html.incomes

import play.twirl.api.Html
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BankBuildingSocietyOverviewSpec extends TaiViewSpec {

  "BankBuildingSociety Overview page" should {
    behave like pageWithTitle(messages("tai.bbsi.overview.heading"))
    behave like pageWithBackLink
    behave like pageWithHeader(messages("tai.bbsi.overview.heading"))

    "display first section" in {
      page must haveParagraphWithText(messages("tai.bbsi.overview.para1"))
      page must haveParagraphWithText(messages("tai.bbsi.overview.para2"))
      page must haveParagraphWithText(
        "£2,000 " + messages(
          "tai.bbsi.overview.interest.year.desc",
          TaxYear().start.toString(dateFormatPattern),
          TaxYear().end.toString(dateFormatPattern)))
      page must haveParagraphWithText(
        messages(
          "tai.bbsi.overview.interest.estimate.desc",
          TaxYear().prev.start.toString(dateFormatPattern),
          TaxYear().prev.end.toString(dateFormatPattern)))
    }

    "display second section" in {
      page must haveHeadingH2WithText(messages("tai.bbsi.overview.whatYouMustDo.title"))
      page must haveParagraphWithText(messages("tai.bbsi.overview.whatYouMustDo.desc"))
      page must haveBulletPointWithText(
        messages(
          "tai.bbsi.overview.whatYouMustDo.point1",
          TaxYear().start.toString(dateFormatPattern),
          TaxYear().end.toString(dateFormatPattern)))
      page must haveBulletPointWithText(messages("tai.bbsi.overview.whatYouMustDo.point2"))
    }

    "display third section" in {
      page must haveHeadingH2WithText(messages("tai.bbsi.overview.whyThisIsImp.title"))
      page must haveParagraphWithText(messages("tai.bbsi.overview.whyThisIsImp.desc"))
      page must haveBulletPointWithText(messages("tai.bbsi.overview.whyThisIsImp.point1"))
      page must haveBulletPointWithText(messages("tai.bbsi.overview.whyThisIsImp.point2"))
      page must haveBulletPointWithText(messages("tai.bbsi.overview.whyThisIsImp.point3"))
    }

    "display details link" in {
      page must haveLinkWithUrlWithID("checkYourAccounts", controllers.income.bbsi.routes.BbsiController.accounts().url)
    }

  }

  private val dateFormatPattern = "d MMMM yyy"
  private lazy val page = doc(view)
  override def view: Html = views.html.incomes.bbsi.bank_building_society_overview(2000)
}
