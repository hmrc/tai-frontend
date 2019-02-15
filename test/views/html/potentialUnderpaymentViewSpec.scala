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
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{EstimatedTaxYouOweThisYear, MarriageAllowanceTransferred, TaxAccountSummary}
import uk.gov.hmrc.tai.util.MonetaryUtil.withPoundPrefix
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.PotentialUnderpaymentViewModel

class potentialUnderpaymentViewSpec extends TaiViewSpec {

  implicit val hc = HeaderCarrier()
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

  val viewModel = PotentialUnderpaymentViewModel(tas, ccs, referalPath, resourceName)
  def document(viewModel: PotentialUnderpaymentViewModel = viewModel) = {
    Jsoup.parseBodyFragment(views.html.potentialUnderpayment(viewModel).toString)
  }


  override def view = views.html.potentialUnderpayment(viewModel)


  "Potential Underpayment" must {
    behave like pageWithBackLink
    behave like pageWithTitle(viewModel.pageTitle)
    behave like pageWithCombinedHeader(messages("tai.iya.tax.you.owe.preHeading"), viewModel.pageTitle)


    "display text indicating tax is owed " in {
      document() must haveParagraphWithText(messages("tai.iya.paidTooLittle.cy.text"))
    }

    "display a section on what happens next" in {
      document() must haveParagraphWithText(messages("tai.iya.what.next.text1", TaxYearRangeUtil.currentTaxYearRange))
    }

    "display get help link" in {
      document() must haveLinkElement("getHelpLink", controllers.routes.HelpController.helpPage.url,
        messages("tai.iya.paidTooLittle.get.help.linkText"))
    }

    "contain a link to return to the previous page" in {
      document() must haveLinkWithText(messages("tai.iya.tax.free.amount.return.link"))
    }

    "display heading text in " in {
      document() must haveH2HeadingWithText(messages("tai.iya.what.next.heading"))
    }

    "cater for a CY IYA, when only a CY amount is present and " should {

      def cyOnlyViewModel = PotentialUnderpaymentViewModel(tasCYOnly, ccs, "", "")
      def cyOnlyDoc = document(cyOnlyViewModel)

      "display amount owing" in {
        cyOnlyDoc must haveParagraphWithText(withPoundPrefix(MoneyPounds(cyOnlyViewModel.iyaCYAmount, 2)))
      }

      "display heading text in " in {
        cyOnlyDoc must haveH2HeadingWithText(messages("tai.iya.how.collected.heading"))
      }

      "display static text " in {
        cyOnlyDoc must haveParagraphWithText(messages("tai.iya.paidTooLittle.cy.text2"))
        cyOnlyDoc must haveParagraphWithText(messages("tai.iya.paidTooLittle.cy.text4"))
        cyOnlyDoc must haveParagraphWithText(messages("tai.iya.what.next.text2"))
      }

    }

    "cater for a CY IYA, when both CY and CY+1 are present and " should {

      def cyAndCYPlusOneViewModel = PotentialUnderpaymentViewModel(tasCYAndCyPlusOne, ccs, "", "")
      def cyAndCYPlusOneDoc = document(cyAndCYPlusOneViewModel)

      "display amount owing" in {
        cyAndCYPlusOneDoc must haveParagraphWithText(withPoundPrefix(MoneyPounds(cyAndCYPlusOneViewModel.iyaCYAmount, 2)))
      }

      "display heading text in " in {
        cyAndCYPlusOneDoc must haveH2HeadingWithText(messages("tai.iya.how.collected.heading"))
        cyAndCYPlusOneDoc must haveH2HeadingWithText(messages("tai.iya.what.next.heading"))
      }

      "display static text " in {
        cyAndCYPlusOneDoc must haveParagraphWithText(messages("tai.iya.paidTooLittle.cy.text2"))
        cyAndCYPlusOneDoc must haveParagraphWithText(messages("tai.iya.paidTooLittle.cy.text4"))
        cyAndCYPlusOneDoc must haveParagraphWithText(messages("tai.iya.what.next.text2"))
      }

    }

    "cater for a CY+1 IYA, when only a CY+1 amount is present and " should {

      def cyPlusOneOnlyViewModel = PotentialUnderpaymentViewModel(tasCyPlusOneOnly, ccs, "", "")
      def cyPlusOneOnlyDoc = document(cyPlusOneOnlyViewModel)

      "display amount owing" in {
        cyPlusOneOnlyDoc must haveParagraphWithText(withPoundPrefix(MoneyPounds(cyPlusOneOnlyViewModel.iyaCYPlusOneAmount, 2)))
      }

      "display heading text in " in {
        cyPlusOneOnlyDoc must haveH2HeadingWithText(messages("tai.iya.cyPlusOne.how.collected.heading"))
      }

      "display text relating to p800 " in {
        cyPlusOneOnlyDoc must haveParagraphWithText(messages("tai.iya.cyPlusOne.what.next.p800.description"))
      }

      "display text regarding tax free amount " in {
        cyPlusOneOnlyDoc must haveParagraphWithText(messages("tai.iya.reduce.tax-free.amount.description",
          Dates.formatDate(TaxYear().next.start)))
      }

      "display tax code may change text" in {
        cyPlusOneOnlyDoc must haveParagraphWithText(messages("tai.iya.cyPlusOne.taxCodeMayBeChanged.description"))
      }
    }
  }


}