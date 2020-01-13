/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model.domain

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class BankAccountSpec extends PlaySpec {

  "BankAccount" must {

    "not contain bank accounts" when {

      "no data is provided." in {

        val jsonBankAccounts = Json.arr()

        val bankAccounts = jsonBankAccounts.as[Seq[BankAccount]]

        bankAccounts mustBe Nil
      }
    }

    "contain one bank account" when {
      "json contains only gross interest" in {

        val jsonBankAccount = Json.obj("id" -> 1, "grossInterest" -> grossInterest1)

        val jsonBankAccounts = Json.arr(jsonBankAccount)

        val bankAccounts = jsonBankAccounts.as[Seq[BankAccount]]

        bankAccounts.size mustBe 1

        val bankAccount1 = bankAccounts(0)

        bankAccount1.accountNumber mustBe None
        bankAccount1.sortCode mustBe None
        bankAccount1.bankName mustBe None
        bankAccount1.grossInterest mustBe grossInterest1
        bankAccount1.source mustBe None
      }
    }
  }

  "contain more than one bank account" when {
    "json contains only gross interest" in {

      val jsonBankAccount1 = Json.obj("id" -> 1, "grossInterest" -> grossInterest1)
      val jsonBankAccount2 = Json.obj("id" -> 2, "grossInterest" -> grossInterest2)

      val jsonBankAccounts = Json.arr(jsonBankAccount1, jsonBankAccount2)

      val bankAccounts = jsonBankAccounts.as[Seq[BankAccount]]

      bankAccounts.size mustBe 2

      val bankAccount1 = bankAccounts(0)

      bankAccount1.accountNumber mustBe None
      bankAccount1.sortCode mustBe None
      bankAccount1.bankName mustBe None
      bankAccount1.grossInterest mustBe grossInterest1
      bankAccount1.source mustBe None

      val bankAccount2 = bankAccounts(1)

      bankAccount2.accountNumber mustBe None
      bankAccount2.sortCode mustBe None
      bankAccount2.bankName mustBe None
      bankAccount2.grossInterest mustBe grossInterest2
      bankAccount2.source mustBe None
    }
  }

  "contain one bank account" when {
    "json contains all fields" in {

      val jsonBankAccounts = Json.arr(jsonBankAccount)

      val bankAccounts = jsonBankAccounts.as[Seq[BankAccount]]

      bankAccounts.size mustBe 1

      val bankAccount1 = bankAccounts(0)

      bankAccount1.accountNumber mustBe Some(accountNo)
      bankAccount1.sortCode mustBe Some(sortCode)
      bankAccount1.bankName mustBe Some(bankName)
      bankAccount1.grossInterest mustBe grossInterest1
      bankAccount1.source mustBe Some(source)
    }
  }

  "format the sort code" in {
    bankAccount.formattedSortCode mustBe Some("12 34 56")
  }

  val id: Int = 0
  val accountNo = "test account no"
  val sortCode = "123456"
  val bankName = "test bank name"
  val source = "customer"

  val bankAccount = BankAccount(0, Some(accountNo), Some(sortCode), Some(bankName), 0, Some(source))
  val grossInterest1: BigDecimal = 123.45
  val grossInterest2: BigDecimal = 678.90

  val jsonBankAccount = Json.obj(
    "id"            -> id,
    "accountNumber" -> accountNo,
    "sortCode"      -> sortCode,
    "bankName"      -> bankName,
    "grossInterest" -> 123.45,
    "source"        -> source)
}
