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

package uk.gov.hmrc.tai.viewModels.income

import controllers.income.bbsi.routes
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Play.current
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.formatting.Money
import uk.gov.hmrc.tai.model.TaxYear


case class BbsiClosedCheckYourAnswersViewModel(id: Int, closeBankAccountDate: String, closeBankAccountName: Option[String], closeBankAccountInterest: Option[String]) {

  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {

    val confirmationLines = Seq(CheckYourAnswersConfirmationLine(
        Messages("tai.checkYourAnswers.whatYouToldUs"),
        Messages("tai.bbsi.end.checkYourAnswers.rowOne.answer"),
        routes.BbsiController.decision(id)toString),
      CheckYourAnswersConfirmationLine(
        Messages("tai.bbsi.end.checkYourAnswers.rowTwo.question"),
        Dates.formatDate(new LocalDate(closeBankAccountDate)),
        routes.BbsiCloseAccountController.captureCloseDate(id).toString))

    if (bankAccountClosedInCurrentTaxYear) {
      confirmationLines :+ CheckYourAnswersConfirmationLine(
        Messages("tai.bbsi.end.checkYourAnswers.rowThree.question", TaxYear().year.toString),
        closeBankAccountInterest
          .map(interest => Money.pounds(BigDecimal(interest)).toString().trim.replace("&pound;", "\u00A3"))
          .getOrElse(Messages("tai.closeBankAccount.closingInterest.notKnown")),
        routes.BbsiCloseAccountController.captureClosingInterest(id).toString)
    }
    else confirmationLines
  }

  val bankAccountClosedInCurrentTaxYear: Boolean = TaxYear().within(LocalDate.parse(closeBankAccountDate))

}
