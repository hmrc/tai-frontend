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

import controllers.routes
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{BankAccount, UntaxedInterest}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BankBuildingSocietyAccountsSpec extends TaiViewSpec {

  "bbsi accounts view" should {
    behave like pageWithTitle(messages("tai.bbsi.accountDetails.heading"))
    behave like pageWithBackLink
    behave like haveReturnToSummaryButtonWithUrl(routes.TaxAccountSummaryController.onPageLoad())

    "display heading" in {
      doc(view) must haveElementAtPathWithText(
        "h2",
        messages("tai.bbsi.account.table.heading", TaxYear().year.toString, TaxYear().next.year.toString))
    }

    "display descriptions" in {
      doc(view) must haveParagraphWithText(messages("tai.bbsi.account.desc1"))
      doc(view) must haveParagraphWithText(messages("tai.bbsi.account.desc2"))
    }

    "display account information" in {
      doc(view) must haveElementAtPathWithText(".cya-question h3", bankName1)
      doc(view) must haveElementAtPathWithText(
        ".cya-question > div",
        messages("tai.bbsi.account.accountNumber") + " " + accountNumber1)
      doc(view) must haveElementAtPathWithText(
        ".cya-question > div + div",
        messages("tai.bbsi.account.sortCode") + " " + bankAccount1.formattedSortCode.getOrElse(""))

      doc(view) must haveElementAtPathWithText(".cya-question h3", bankName2)
      doc(view) must haveElementAtPathWithText(
        ".cya-question > div",
        messages("tai.bbsi.account.accountNumber") + " " + accountNumber2)
      doc(view) must haveElementAtPathWithText(
        ".cya-question > div + div",
        messages("tai.bbsi.account.sortCode") + " " + bankAccount2.formattedSortCode.getOrElse(""))

      doc(view) must haveElementAtPathWithText(
        ".cya-answer",
        messages("tai.bbsi.account.table.amount") + " " + "£123.45")
      doc(view) must haveElementAtPathWithText(
        ".cya-answer",
        messages("tai.bbsi.account.table.amount") + " " + "£456.78")

      doc(view) must haveElementAtPathWithText(".cya-change a span", messages("tai.bbsi.account.updateOrRemoveLink"))
      doc(view) must haveLinkWithUrlWithID(
        "bbsiAccountDecision1",
        controllers.income.bbsi.routes.BbsiController.decision(1).url)
    }

    "display estimated interest total" in {
      doc(view) must haveElementAtPathWithText(".highlight h3", messages("tai.bbsi.account.table.total"))
      doc(view) must haveElementAtPathWithText(".highlight .cya-answer", "£2,000.00")
    }

    "display total estimated interest in description" in {
      doc(view) must haveParagraphWithText(
        messages(
          "tai.bbsi.account.totalEstimatedInterest.desc2",
          untaxedInterest.amount,
          TaxYear().year.toString,
          TaxYear().next.year.toString))
    }

    "not display account details" when {
      "account number and sort code has all zeroes" in {
        val view = views.html.incomes.bbsi
          .bank_building_society_accounts(UntaxedInterest(100, List(bankAccount1, bankAccount3, bankAccount2)))

        doc(view) must haveElementAtPathWithText(
          ".cya-question h3",
          messages("tai.bbsi.account.accountDetailsUnavailable"))
        doc(view) must haveElementAtPathWithText(".cya-answer", messages("tai.bbsi.account.youToldUsTheAmount"))
      }
    }
  }

  private val accountNumber1 = "test account no 1"
  private val sortCode1 = "123456"
  private val bankName1 = "test bank name 1"
  private val grossInterest1 = 123.45
  private val source1 = "Customer1"

  private val accountNumber2 = "test account no 2"
  private val sortCode2 = "321654"
  private val bankName2 = "test bank name 2"
  private val grossInterest2 = 456.78
  private val source2 = "Customer2"

  private val accountNumber3 = "*******0000"
  private val sortCode3 = "000000"

  private val bankAccount1 = BankAccount(
    1,
    accountNumber = Some(accountNumber1),
    sortCode = Some(sortCode1),
    bankName = Some(bankName1),
    grossInterest = grossInterest1,
    source = Some(source1))

  private val bankAccount2 = BankAccount(
    2,
    accountNumber = Some(accountNumber2),
    sortCode = Some(sortCode2),
    bankName = Some(bankName2),
    grossInterest = grossInterest2,
    source = Some(source2))
  private val bankAccount3 = BankAccount(
    2,
    accountNumber = Some(accountNumber3),
    sortCode = Some(sortCode3),
    bankName = Some(bankName2),
    grossInterest = grossInterest2,
    source = Some(source2))

  private val accounts: Seq[BankAccount] = List(bankAccount1, bankAccount2)

  private val untaxedInterest = UntaxedInterest(2000, accounts)

  override def view = views.html.incomes.bbsi.bank_building_society_accounts(untaxedInterest)
}
