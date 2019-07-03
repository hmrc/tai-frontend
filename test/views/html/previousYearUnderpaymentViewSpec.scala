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

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.PreviousYearUnderpaymentViewModel



class previousYearUnderpaymentViewSpec extends TaiViewSpec {


  "previousYearUnderpaymentView" must {

    behave like pageWithCombinedHeader(
      Messages("tai.iya.tax.you.owe.preHeading"),
      Messages("tai.previous.year.underpayment.title"))

    behave like pageWithTitle(Messages("tai.previous.year.underpayment.title"))

    behave like pageWithBackLink

    "display paragraphs" in {

      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.p1", TaxYearRangeUtil.futureTaxYearRangeHtmlNonBreak(-1)))
      doc must haveSpanWithText(poundedAmountDue)
      doc must haveH2HeadingWithText(Messages("tai.previous.year.underpayment.h1"))

      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.p2", allowanceReducedBy, Dates.formatDate(TaxYear().end), poundedAmountDue))

      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.p3"))

      doc must haveH2HeadingWithText(Messages("tai.previous.year.underpayment.h2"))

      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.p4", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.p5"))
    }
  }

  val allowanceReducedBy = 500
  val poundedAmountDue = "£100.00"

  val test = Dates.formatDate(TaxYear().start)


  override def view = previousYearUnderpayment(PreviousYearUnderpaymentViewModel(allowanceReducedBy, poundedAmountDue, Html("some-link")))

}
