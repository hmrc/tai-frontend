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

package uk.gov.hmrc.tai.service

import org.joda.time.{DateTime, LocalDate}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.BbsiConnector
import uk.gov.hmrc.tai.model.domain.{BankAccount, UntaxedInterest}
import uk.gov.hmrc.tai.model.{AmountRequest, CloseAccountRequest, TaxYear}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class BbsiServiceSpec extends PlaySpec with MockitoSugar {

  "bankAccounts" should {

    "return nil" when {
      "connector returns nil" in {

        val sut = createSUT

        when(connector.bankAccounts(any())(any())).thenReturn(Future.successful(Nil))

        val data = Await.result(sut.bankAccounts(nino), 5.seconds)

        data mustBe Nil
      }
    }

    "return one bank account" when {
      "connector returns one bank account" in {

        val sut = createSUT

        when(connector.bankAccounts(any())(any())).thenReturn(Future.successful(oneBankAccount))

        val data = Await.result(sut.bankAccounts(nino), 5.seconds)

        data mustBe oneBankAccount
      }
    }

    "return multiple bank accounts" when {
      "connector returns multiple bank account" in {

        val sut = createSUT

        when(connector.bankAccounts(any())(any())).thenReturn(Future.successful(multipleBankAccounts))

        val data = Await.result(sut.bankAccounts(nino), 5.seconds)

        data mustBe multipleBankAccounts
      }
    }
  }

  "bankAccount" should {

    "return none" when {
      "connector returns none" in {

        val sut = createSUT

        when(connector.bankAccount(any(), org.mockito.Matchers.eq(1))(any())).thenReturn(Future.successful(None))

        val data = Await.result(sut.bankAccount(nino, 1), 5.seconds)

        data mustBe None
      }
    }

    "return one bank account" when {
      "connector returns one bank account" in {

        val sut = createSUT

        when(connector.bankAccount(any(), org.mockito.Matchers.eq(2))(any()))
          .thenReturn(Future.successful(Some(bankAccount2)))

        val data = Await.result(sut.bankAccount(nino, 2), 5.seconds)

        data.get mustBe bankAccount2
      }
    }
  }

  "bankAccounts - closeBankAccount" should {
    "return envelope id" in {
      val sut = createSUT
      when(connector.closeBankAccount(any(), any(), any())(any())).thenReturn(Future.successful(Some("123-456-789")))

      val data =
        Await.result(sut.closeBankAccount(nino, 1, CloseAccountRequest(new LocalDate(2017, 1, 1), None)), 5.seconds)

      data mustBe "123-456-789"
    }

    "throw exception" when {
      "envelope id is none" in {
        val sut = createSUT
        when(connector.closeBankAccount(any(), any(), any())(any())).thenReturn(Future.successful(None))

        val ex = the[RuntimeException] thrownBy Await
          .result(sut.closeBankAccount(nino, 1, CloseAccountRequest(new LocalDate(2017, 1, 1), None)), 5.seconds)

        ex.getMessage mustBe "Failed while closing the bank account"
      }
    }
  }

  "untaxedInterest" should {

    "throw an exception" when {
      "connector returns none" in {

        val sut = createSUT

        when(connector.untaxedInterest(any())(any())).thenReturn(Future.successful(None))

        val ex = the[RuntimeException] thrownBy Await.result(sut.untaxedInterest(nino), 5.seconds)

        ex.getMessage mustBe "Failed while retrieving untaxed interest"

      }
    }

    "return untaxed interest" when {
      "connector returns untaxed interest" in {

        val sut = createSUT

        when(connector.untaxedInterest(any())(any())).thenReturn(Future.successful(Some(untaxedInterest)))

        val data = Await.result(sut.untaxedInterest(nino), 5.seconds)

        data mustBe untaxedInterest
      }
    }
  }

  "remove bank account" should {
    "return envelope id" in {
      val sut = createSUT
      when(connector.removeBankAccount(any(), any())(any())).thenReturn(Future.successful(Some("123-456-789")))

      val data = Await.result(sut.removeBankAccount(nino, 1), 5.seconds)

      data mustBe "123-456-789"
    }

    "throw exception" when {
      "envelope id is none" in {
        val sut = createSUT
        when(connector.removeBankAccount(any(), any())(any())).thenReturn(Future.successful(None))

        val ex = the[RuntimeException] thrownBy Await.result(sut.removeBankAccount(nino, 1), 5.seconds)

        ex.getMessage mustBe "Failed while removing the bank account"
      }
    }
  }

  "update bank account" should {
    "return envelope id" in {
      val sut = createSUT
      when(connector.updateBankAccountInterest(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some("123-456-789")))

      val data = Await.result(sut.updateBankAccountInterest(nino, 1, amountRequest), 5.seconds)

      data mustBe "123-456-789"
    }

    "throw exception" when {
      "envelope id is none" in {
        val sut = createSUT
        when(connector.updateBankAccountInterest(any(), any(), any())(any())).thenReturn(Future.successful(None))

        val ex = the[RuntimeException] thrownBy Await
          .result(sut.updateBankAccountInterest(nino, 1, amountRequest), 5.seconds)

        ex.getMessage mustBe "Failed while updating the bank account"
      }
    }
  }

  private val amountRequest = AmountRequest(2000)
  private val year: TaxYear = TaxYear(DateTime.now().getYear)
  private val nino: Nino = new Generator().nextNino
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val bankAccount1 = BankAccount(
    1,
    Some("TestAccountNumber1"),
    Some("TestSortCode1"),
    Some("TestBankName1"),
    678.90,
    Some("TestSource1"))

  private val bankAccount2 = BankAccount(
    2,
    Some("TestAccountNumber2"),
    Some("TestSortCode2"),
    Some("TestBankName2"),
    123.45,
    Some("TestSource2"))

  private val oneBankAccount = Seq(bankAccount1)

  private val multipleBankAccounts = Seq(bankAccount1, bankAccount2)

  private val untaxedInterest = UntaxedInterest(200, Nil)

  def createSUT = new SUT

  val connector = mock[BbsiConnector]

  class SUT extends BbsiService(connector)

}
