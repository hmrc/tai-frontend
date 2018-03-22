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

import controllers.routes
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.{ExtensionRelief, ExtensionReliefs, IncreasesTax, TaxSummaryDetails}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
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
      doc must haveThWithText("Payment")
      doc must haveThWithText("Amount paid (£)")
      doc must haveThWithText("Tax saving (£)")
    }
    "not display the gift aid section" when {
      "there are no gift aid payments" in {
        doc must not(haveSummaryWithText("Gift Aid"))
      }
    }
    "display the gift aid section" when {
      "there are gift aid payments" in {
        val viewWithGiftAid: Html = views.html.reliefs(taxSummaryDetails.copy(extensionReliefs = Some(extensionRelief)))
        val docWithGiftAid: Document = Jsoup.parse(viewWithGiftAid.toString)
        docWithGiftAid must haveSummaryWithText("Gift Aid")
      }
    }
    "display the appropriate tax relief information" when {
      "the user is a basic rate tax payer" in {
        val higherRateTaxSummaryDetails = taxSummaryDetails.copy(
          increasesTax = Some(IncreasesTax(total = 1)),
          extensionReliefs = Some(ExtensionReliefs(Some(ExtensionRelief(
            sourceAmount = BigDecimal(1), reliefAmount = BigDecimal(0))))))
        val viewHigherRate = views.html.reliefs(higherRateTaxSummaryDetails)
        val docHigherRate = Jsoup.parse(viewHigherRate.toString)
        docHigherRate must haveParagraphWithText("As a basic rate tax payer you don’t get tax relief on donations" +
          " to charity or to community amateur sports clubs (CASCs). If you start to pay higher rate tax you may get " +
          "tax relief on them.")
      }
      "the user is a higher rate tax payer" in {
        val higherRateTaxSummaryDetails = taxSummaryDetails.copy(
          increasesTax = Some(IncreasesTax(total = 1)),
          extensionReliefs = Some(extensionRelief))
        val viewHigherRate = views.html.reliefs(higherRateTaxSummaryDetails)
        val docHigherRate = Jsoup.parse(viewHigherRate.toString)
        docHigherRate must haveParagraphWithText("Your donations to charity or to community amateur sports clubs" +
          " (CASCs) are tax-free. As we expect you to pay some higher rate tax you’ll be entitled to relief on your" +
          " donations to charity.")
      }
      "the user's income is too low to pay tax" in {
        val viewWithLowIncome: Html = views.html.reliefs(taxSummaryDetails.copy(extensionReliefs = Some(extensionRelief)))
        val docWithLowIncome: Document = Jsoup.parse(viewWithLowIncome.toString)
        docWithLowIncome must haveParagraphWithText("You can’t get tax relief on these payments as your income is too " +
          "low to pay Income Tax.")
      }
    }
    "display the correct gift aid source amount" in {
      val viewWithGiftAid: Html = views.html.reliefs(taxSummaryDetails.copy(extensionReliefs = Some(extensionRelief)))
      val docWithGiftAid: Document = Jsoup.parse(viewWithGiftAid.toString)
      docWithGiftAid must haveTdWithText("1")
    }
    "display the correct gift aid relief amount" in {
      val viewWithGiftAid: Html = views.html.reliefs(taxSummaryDetails.copy(extensionReliefs = Some(extensionRelief)))
      val docWithGiftAid: Document = Jsoup.parse(viewWithGiftAid.toString)
      docWithGiftAid must haveTdWithText("1.00")
    }
    "not display PPR messages" when {
      "user has no personal pension payments" in {
        val viewWithGiftAid: Html = views.html.reliefs(taxSummaryDetails.copy(extensionReliefs = Some(extensionRelief)))
        val docWithGiftAid: Document = Jsoup.parse(viewWithGiftAid.toString)
        docWithGiftAid must not(haveSummaryWithText("Personal Pension payments"))
      }
    }
    "display PPR messages" when {
      "user has personal pension payments" in {
        val viewWithPPR = views.html.reliefs(taxSummaryDetails.copy(extensionReliefs = Some(ExtensionReliefs(
          personalPension = Some(ExtensionRelief(sourceAmount = BigDecimal(1), reliefAmount = BigDecimal(1)))))))
        val docWithPPR = Jsoup.parse(viewWithPPR.toString)
        docWithPPR must haveSummaryWithText("Personal Pension payments")
        docWithPPR must haveParagraphWithText("This is the tax relief you’ve claimed for your Personal Pension payments" +
          " if you pay higher rate tax. Basic rate tax relief is given at source by your pension provider.")
        docWithPPR must haveTdWithText("1")
        docWithPPR must haveTdWithText("1.00")
      }
    }
  }

  def view: Html = views.html.reliefs(taxSummaryDetails)
  val nino = new Generator().nextNino
  val taxSummaryDetails = TaxSummaryDetails(nino.nino, 1)
  val extensionRelief = ExtensionReliefs(Some(ExtensionRelief(sourceAmount = BigDecimal(1), reliefAmount = BigDecimal(1))))

}