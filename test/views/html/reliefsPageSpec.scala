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

package views.html

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.TaxReliefViewModel
import uk.gov.hmrc.time.TaxYearResolver

class reliefsPageSpec extends TaiViewSpec {

  "Releifs" should {
    behave like pageWithTitle("Your tax relief payments")
    behave like pageWithHeader("Your tax relief payments")
    behave like pageWithBackLink
  }
  "show correct header content" in {

    val accessiblePreHeading = doc.select("""header span[class="visuallyhidden"]""")
    accessiblePreHeading.text mustBe Messages("tai.estimatedIncome.accessiblePreHeading")

    val expectedTaxYearString =  Messages("tai.taxYear",
      nonBreakable(Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear)),
      nonBreakable(Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)) )

    val preHeading = doc.select("header p")
    preHeading.text mustBe s"${Messages("tai.estimatedIncome.accessiblePreHeading")} ${expectedTaxYearString}"
  }
  "Current tax year section" should {
    "explain tax relief" in {
      doc must haveParagraphWithText("Tax relief means you pay less tax to take account of money you’ve spent on " +
        "Gift Aid donations or personal pension payments.")
    }
  }
  "Tax reliefs payments table" should {
    "have the correct headings" in {
      doc must haveThWithText(Messages("tai.extendedTaxReliefs.payments"))
      doc.select("#now-pay-tax-on thead th").get(1).html() mustBe Html(Messages("tai.extendedTaxReliefs.source")).toString()
      doc.select("#now-pay-tax-on thead th").get(2).html() mustBe Html(Messages("tai.extendedTaxReliefs.relief")).toString()
    }
    "not display the gift aid section" when {
      "there are no gift aid payments" in {
        doc must not(haveTdWithText("Gift Aid"))
      }
    }
    "display the gift aid section" when {
      "there are gift aid payments" in {
        val viewWithGiftAid: Html = views.html.reliefsNew(model.copy(hasGiftAid = true))
        val docWithGiftAid: Document = Jsoup.parse(viewWithGiftAid.toString)
        docWithGiftAid must haveTdWithText(Messages("tai.extendedTaxReliefs.giftAid.title") + " " + Messages("tai.extendedTaxReliefs.giftAid.NoTax.description"))
      }
    }
    "display the appropriate tax relief information" when {
      "the user is a basic rate tax payer" in {
        val basicRate = TaxReliefViewModel(true, false, 1, 0, 0, 0, true)
        val viewBasicRate = views.html.reliefsNew(basicRate)
        val docHigherRate = Jsoup.parse(viewBasicRate.toString)
        docHigherRate must haveParagraphWithText("As a basic rate tax payer you don’t get tax relief on donations" +
          " to charity or to community amateur sports clubs (CASCs). If you start to pay higher rate tax you may get " +
          "tax relief on them.")
      }
      "the user is a higher rate tax payer" in {
        val higherRate = TaxReliefViewModel(true, false, 1, 0, 1, 0, true)
        val viewHigherRate = views.html.reliefsNew(higherRate)
        val docHigherRate = Jsoup.parse(viewHigherRate.toString)
        docHigherRate must haveParagraphWithText("Your donations to charity or to community amateur sports clubs" +
          " (CASCs) are tax-free. As we expect you to pay some higher rate tax you’ll be entitled to relief on your" +
          " donations to charity.")
      }
      "the user's income is too low to pay tax" in {
        val higherRate = TaxReliefViewModel(true, false, 1, 0, 1, 0, false)
        val viewWithLowIncome: Html = views.html.reliefsNew(higherRate)
        val docWithLowIncome: Document = Jsoup.parse(viewWithLowIncome.toString)
        docWithLowIncome must haveParagraphWithText("You can’t get tax relief on these payments as your income is too " +
          "low to pay Income Tax.")
      }
    }
    "display the correct gift aid source amount" in {
      val viewWithGiftAid: Html = views.html.reliefsNew(TaxReliefViewModel(true, false, 1, 0, 1, 0, false))
      val docWithGiftAid: Document = Jsoup.parse(viewWithGiftAid.toString)
      docWithGiftAid must haveTdWithText("1")
    }
    "display the correct gift aid relief amount" in {
      val viewWithGiftAid: Html = views.html.reliefsNew(TaxReliefViewModel(true, false, 1, 0, 1, 0, false))
      val docWithGiftAid: Document = Jsoup.parse(viewWithGiftAid.toString)
      docWithGiftAid must haveTdWithText("1.00")
    }
    "not display PPR messages" when {
      "user has no personal pension payments" in {
        val viewWithGiftAid: Html = views.html.reliefsNew(TaxReliefViewModel(true, false, 1, 0, 1, 0, false))
        val docWithGiftAid: Document = Jsoup.parse(viewWithGiftAid.toString)
        docWithGiftAid must not(haveTdWithText("Personal Pension payments"))
      }
    }
    "display PPR messages" when {
      "user has personal pension payments" in {
        val viewWithPPR = views.html.reliefsNew(TaxReliefViewModel(false, true, 1, 1, 1, 1, false))
        val docWithPPR = Jsoup.parse(viewWithPPR.toString)
        docWithPPR must haveTdWithText(Messages("tai.extendedTaxReliefs.ppr.title") + " " + Messages("tai.extendedTaxReliefs.ppr.description"))
        docWithPPR must haveTdWithText("1")
        docWithPPR must haveTdWithText("1.00")
      }
    }
  }

  val model = TaxReliefViewModel(false, false, 0, 0, 0, 0, false)
  def view: Html = views.html.reliefsNew(model)
}