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

import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.PreviousYearUnderpaymentViewModel


class previousYearUnderpaymentViewSpec extends TaiViewSpec {


  "previousYearUnderpaymentView" must {

    behave like pageWithCombinedHeader(
      Messages("tai.iya.tax.you.owe.cy-minus-one.preHeading"),
      Messages("tai.iya.tax.you.owe.cy-minus-one.title"))

    behave like pageWithTitle(Messages("tai.iya.tax.you.owe.cy-minus-one.title"))

    behave like pageWithBackLink

    "display paragraphs" in {
      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.para1"))
      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.para2"))
      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.para3"))
      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.para4"))
      doc must haveParagraphWithText(Messages("tai.previous.year.underpayment.para5"))


    }


  }

  val shouldHavePaid = 1000
  val actuallyPaid = 900
  val allowanceReducedBy = 500
  val amountDue = 100

  override def view = previousYearUnderpayment(PreviousYearUnderpaymentViewModel(shouldHavePaid, actuallyPaid, allowanceReducedBy, amountDue))

}
