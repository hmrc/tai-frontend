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

package views.html

import org.jsoup.Jsoup
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{EstimatedTaxYouOweThisYear, MarriageAllowanceTransferred, TaxAccountSummary}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.PotentialUnderpaymentViewModel

class potentialUnderpaymentViewSpec extends TaiViewSpec {

  implicit val hc = HeaderCarrier()

  "the Potential Underpayment view method" should {

    "show a back button" when {
      behave like pageWithBackLink
    }

    "omit underpayment content if no underpayment is present" in {

      val html = views.html.potentialUnderpayment(PotentialUnderpaymentViewModel(tasNoUnderpay, ccs, "", ""))
      val doc = Jsoup.parseBodyFragment(html.toString)

      doc.title() must include(Messages("tai.iya.tax.you.owe.title"))
      doc must not(haveElementWithId("iya-cy-how-much"))
      doc must not(haveElementWithId("iya-cy-plus-one-how-much"))
      doc must not(haveElementWithId("iya-cy-and-cy-plus-one-how-much"))
    }

    "display the potential underpayment page configured for a CY IYA, when only a CY amount is present " in {

      val html = views.html.potentialUnderpayment(PotentialUnderpaymentViewModel(tasCYOnly, ccs, "", ""))
      val doc = Jsoup.parseBodyFragment(html.toString)

      doc.title() must include(Messages("tai.iya.tax.you.owe.title"))
      doc must haveElementWithId("iya-cy-how-much")
      doc must not(haveElementWithId("iya-cy-plus-one-how-much"))
      doc must not(haveElementWithId("iya-cy-and-cy-plus-one-how-much"))

      doc must haveElementAtPathWithText("a[id=getHelpLink]", Messages("tai.iya.paidTooLittle.get.help.linkText"))
      doc must haveElementAtPathWithAttribute("a[id=getHelpLink]", "href", controllers.routes.HelpController.helpPage.toString)
    }

    "display the potential underpayment page configured for a CY IYA, when both CY and CY+1 are present" in {

      val html = views.html.potentialUnderpayment(PotentialUnderpaymentViewModel(tasCYAndCyPlusOne, ccs, "", ""))
      val doc = Jsoup.parseBodyFragment(html.toString)

      doc.title() must include(Messages("tai.iya.tax.you.owe.title"))
      doc must haveElementWithId("iya-cy-how-much")
      doc must not(haveElementWithId("iya-cy-plus-one-how-much"))
      doc must not(haveElementWithId("iya-cy-and-cy-plus-one-how-much"))

      doc must haveElementAtPathWithText("a[id=getHelpLink]", Messages("tai.iya.paidTooLittle.get.help.linkText"))
      doc must haveElementAtPathWithAttribute("a[id=getHelpLink]", "href", controllers.routes.HelpController.helpPage.toString)
    }

    "contain a link to return to the previous page" in {
      val doc = Jsoup.parseBodyFragment(view.toString)

      doc must haveLinkWithText(messages("tai.iya.tax.free.amount.return.link"))
    }

    "display the potential underpayment page configured for a CY+1 IYA, when only a CY+1 amount is present" in {

      val html = views.html.potentialUnderpayment(PotentialUnderpaymentViewModel(tasCyPlusOneOnly, ccs, "", ""))
      val doc = Jsoup.parseBodyFragment(html.toString)

      doc.title() must include(Messages("tai.iya.tax.you.owe.title"))
      doc must not(haveElementWithId("iya-cy-how-much"))
      doc must haveElementWithId("iya-cy-plus-one-how-much")
      doc must not(haveElementWithId("iya-cy-and-cy-plus-one-how-much"))

      doc must not(haveElementWithId("getHelpLink"))
    }
  }

  val nino = new Generator().nextNino

  val tas = TaxAccountSummary(11.11, 22.22, 33.33, 44.44, 55.55)
  val ccs = Seq(
    CodingComponent(MarriageAllowanceTransferred, Some(1), 1400.86, "MarriageAllowanceTransfererd"),
    CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 33.44, "EstimatedTaxYouOweThisYear")
  )

  val tasNoUnderpay = TaxAccountSummary(11.11, 22.22, 0, 44.44, 0)
  val tasCYOnly = TaxAccountSummary(11.11, 22.22, 33.33, 44.44, 0)
  val tasCYAndCyPlusOne = TaxAccountSummary(11.11, 22.22, 33.33, 44.44, 55.55)
  val tasCyPlusOneOnly = TaxAccountSummary(11.11, 22.22, 0, 44.44, 55.55)
  val referalPath = "http://somelocation/tax-free-allowance"
  val resourceName = "tax-free-allowance"

  override def view = views.html.potentialUnderpayment(PotentialUnderpaymentViewModel(tas, ccs, referalPath, resourceName))
}