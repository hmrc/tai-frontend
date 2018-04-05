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

package uk.gov.hmrc.tai.viewModels.income

import controllers.FakeTaiPlayApplication
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{Messages, MessagesApi}
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.tai.util.JourneyCacheConstants



class BbsiClosedCheckYourAnswersViewModelSpec extends PlaySpec
  with JourneyCacheConstants with FakeTaiPlayApplication {

  "Close BBSI view model" should {
    "create a valid model" when {
      "given a correct date, bank account name and closing interest amount" in {

        val result: BbsiClosedCheckYourAnswersViewModel = BbsiClosedCheckYourAnswersViewModel(0,
          dateWithinCurrentTaxYear.toString("yyyy-MM-dd"), Some("Bank name"), Some("123.43"))

        result.journeyConfirmationLines.length mustBe 3

        result.journeyConfirmationLines(applicationMessages)(0) mustBe CheckYourAnswersConfirmationLine(
          Messages("tai.checkYourAnswers.whatYouToldUs"),
          Messages("tai.bbsi.end.checkYourAnswers.rowOne.answer"),
          "/check-income-tax/income/bank-building-society-savings/accounts/0/decision")

        result.journeyConfirmationLines(applicationMessages)(1) mustBe CheckYourAnswersConfirmationLine(
            Messages("tai.bbsi.end.checkYourAnswers.rowTwo.question"),
            dateWithinCurrentTaxYear.toString("d MMMM yyyy"),
            "/check-income-tax/income/bank-building-society-savings/0/close/date")
      }
    }

    "create a valid model with closing interest" when {
      "given a date within the current tax year, bank account name and closing interest amount" in {

        val result: BbsiClosedCheckYourAnswersViewModel = BbsiClosedCheckYourAnswersViewModel(0,
          dateWithinCurrentTaxYear.toString("yyyy-MM-dd"), Some("Bank name"), Some("123456"))

        result.journeyConfirmationLines.length mustBe 3

        result.journeyConfirmationLines(applicationMessages)(0) mustBe CheckYourAnswersConfirmationLine(
          Messages("tai.checkYourAnswers.whatYouToldUs"),
          Messages("tai.bbsi.end.checkYourAnswers.rowOne.answer"),
          "/check-income-tax/income/bank-building-society-savings/accounts/0/decision")

        result.journeyConfirmationLines(applicationMessages)(1) mustBe CheckYourAnswersConfirmationLine(
          Messages("tai.bbsi.end.checkYourAnswers.rowTwo.question"),
          dateWithinCurrentTaxYear.toString("d MMMM yyyy"),
          "/check-income-tax/income/bank-building-society-savings/0/close/date")

        result.journeyConfirmationLines(applicationMessages)(2) mustBe CheckYourAnswersConfirmationLine(
          Messages("tai.bbsi.end.checkYourAnswers.rowThree.question", TaxYearResolver.currentTaxYear.toString),
          "£123,456",
          "/check-income-tax/income/bank-building-society-savings/0/close/interest")
      }
    }

    "create a valid model without closing interest" when {
      "given a date ouside of current tax year and a bank account name" in {

        val result: BbsiClosedCheckYourAnswersViewModel = BbsiClosedCheckYourAnswersViewModel(0,
          dateBeforeCurrentTaxYear.toString("yyyy-MM-dd"), Some("Bank name"), None)

        result.journeyConfirmationLines.length mustBe 2

        result.journeyConfirmationLines(applicationMessages)(0) mustBe CheckYourAnswersConfirmationLine(
          Messages("tai.checkYourAnswers.whatYouToldUs"),
          Messages("tai.bbsi.end.checkYourAnswers.rowOne.answer"),
          "/check-income-tax/income/bank-building-society-savings/accounts/0/decision")

        result.journeyConfirmationLines(applicationMessages)(1) mustBe CheckYourAnswersConfirmationLine(
          Messages("tai.bbsi.end.checkYourAnswers.rowTwo.question"),
          dateBeforeCurrentTaxYear.toString("d MMMM yyyy"),
          "/check-income-tax/income/bank-building-society-savings/0/close/date")
      }
    }
  }

  val dateBeforeCurrentTaxYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(1)
  val dateWithinCurrentTaxYear = TaxYearResolver.startOfCurrentTaxYear
}