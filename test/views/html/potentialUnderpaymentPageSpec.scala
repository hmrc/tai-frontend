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

import controllers.ViewModelFactory
import controllers.viewModels.PotentialUnderpaymentPageVM
import data.TaiData
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.i18n.Messages
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class potentialUnderpaymentPageSpec extends TaiViewSpec
  with ScalaFutures {


  implicit val hc = HeaderCarrier()

  "Calling the Potential Underpayment Page method" should {

    "show a back button" when {
      behave like pageWithBackLink

    }

    "display the potential underpayment page even if we have no underpayment" in {

      val testSummary = TaiData.getBasicRateTaxSummary
      val pModel = ViewModelFactory.create(PotentialUnderpaymentPageVM, nino, testSummary)

      val html = views.html.potentialUnderpayment(pModel)

      val doc = Jsoup.parseBodyFragment(html.toString)

      doc.title() must include(Messages("tai.iya.tax.you.owe.cy-plus-one.title"))

      val potentialUnderpayment = doc.select("#potential-underpayment-amount")
      potentialUnderpayment.size() must be(0)

    }


    "not display the potential underpayment page with an underpayment using old field name potentialUnderpayment" in {
      val testTaxSummary = TaiData.getPotentialUnderpaymentTaxSummary

      val pModel = ViewModelFactory.create(PotentialUnderpaymentPageVM, nino, testTaxSummary)

      val html = views.html.potentialUnderpayment(pModel)

      val doc = Jsoup.parseBodyFragment(html.toString)

      doc.title() must include(Messages("tai.iya.tax.you.owe.cy-plus-one.title"))

      val potentialUnderpayment = doc.select("#potential-underpayment-amount")
      potentialUnderpayment.size() must be(0)
    }

    "display the potential underpayment page with an iya for cy and iya for cy+1" in {
      val testTaxSummary = TaiData.getInYearAdjustmentJsonTaxSummary

      val pModel = ViewModelFactory.create(PotentialUnderpaymentPageVM, nino, testTaxSummary)

      val html = views.html.potentialUnderpayment(pModel)

      val doc = Jsoup.parseBodyFragment(html.toString)

      doc.title() must include(Messages("tai.iya.tax.you.owe.cy-plus-one.title"))

      val inYearAdjustmentIntoCYAndCYPlusOne = doc.select("#iya-cy-and-cy-plus-one-how-much")
      inYearAdjustmentIntoCYAndCYPlusOne.size() must be(1)

      val inYearAdjustmentIntoCYPlusOne = doc.select("#iya-cy-plus-one-how-much")
      inYearAdjustmentIntoCYPlusOne.size() must be(0)

      val inYearAdjustmentIntoCY = doc.select("#iya-cy-how-much")
      inYearAdjustmentIntoCY.size() must be(0)

      doc.getElementsMatchingOwnText(Messages("tai.iya.paidTooLittle.get.help.linkText")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("tai.iya.paidTooLittle.get.help.linkText")).attr("href") must be(controllers.routes.HelpController.helpPage.toString)

    }

    "display the potential underpayment page with an iya for cy" in {
      val testTaxSummary = TaiData.getInYearAdjustmentJsonTaxSummaryCYOnly

      val pModel = ViewModelFactory.create(PotentialUnderpaymentPageVM, nino, testTaxSummary)

      val html = views.html.potentialUnderpayment(pModel)

      val doc = Jsoup.parseBodyFragment(html.toString)

      doc.title() must include(Messages("tai.iya.tax.you.owe.title"))

      val inYearAdjustmentIntoCYAndCYPlusOne = doc.select("#iya-cy-and-cy-plus-one-how-much")
      inYearAdjustmentIntoCYAndCYPlusOne.size() must be(0)

      val inYearAdjustmentIntoCYPlusOne = doc.select("#iya-cy-plus-one-how-much")
      inYearAdjustmentIntoCYPlusOne.size() must be(0)

      val inYearAdjustmentIntoCY = doc.select("#iya-cy-how-much")
      inYearAdjustmentIntoCY.size() must be(1)

      doc.getElementsMatchingOwnText(Messages("tai.iya.paidTooLittle.get.help.linkText")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("tai.iya.paidTooLittle.get.help.linkText")).attr("href") must be(controllers.routes.HelpController.helpPage.toString)
    }

    "display the potential underpayment page with an iya for cy plus one" in {
      val testTaxSummary = TaiData.getInYearAdjustmentJsonTaxSummaryCYPlusOneOnly

      val pModel = ViewModelFactory.create(PotentialUnderpaymentPageVM, nino, testTaxSummary)

      val html = views.html.potentialUnderpayment(pModel)

      val doc = Jsoup.parseBodyFragment(html.toString)

      doc.title() must include(Messages("tai.iya.tax.you.owe.cy-plus-one.title"))

      val inYearAdjustmentIntoCYAndCYPlusOne = doc.select("#iya-cy-and-cy-plus-one-how-much")
      inYearAdjustmentIntoCYAndCYPlusOne.size() must be(0)

      val inYearAdjustmentIntoCYPlusOne = doc.select("#iya-cy-plus-one-how-much")
      inYearAdjustmentIntoCYPlusOne.size() must be(1)

      val inYearAdjustmentIntoCY = doc.select("#iya-cy-how-much")
      inYearAdjustmentIntoCY.size() must be(0)

      doc.getElementsMatchingOwnText(Messages("tai.iya.paidTooLittle.get.help.linkText")).hasAttr("href") must be(false)

    }
  }
  val testTaxSummary = TaiData.getInYearAdjustmentJsonTaxSummaryCYPlusOneOnly

  val nino = new Generator().nextNino

  val pModel = ViewModelFactory.create(PotentialUnderpaymentPageVM, nino, testTaxSummary)

  override def view = views.html.potentialUnderpayment(pModel)

}
