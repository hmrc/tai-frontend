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

package uk.gov.hmrc.tai.connectors

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.model.domain.{BankAccount, SavingsInvestments, UntaxedInterest}
import uk.gov.hmrc.tai.model.{AmountRequest, CloseAccountRequest}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class BbsiConnectorSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with DefaultServicesConfig
  with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    Mockito.reset(httpHandler)
  }

  "BbsiConnector" should {

    "return empty bank accounts" when {

      "api doesn't return a bank account" in {

        val sut = createSut("http://")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(
          jsonBbsiDetails(Json.arr())
        ))

        val result = Await.result(sut.bankAccounts(nino), 5.seconds)

        result mustBe Nil

        verify(httpHandler, times(1)).getFromApi(
          Matchers.eq(s"http:///tai/$nino/tax-account/income/savings-investments/untaxed-interest/bank-accounts")
        )(any())
      }
    }

    "return Sequence of BankAccounts" when {

      "api returns single bank account" in {

        val sut = createSut("http://")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(
          jsonBbsiDetails(jsonSingleBankAccounts))
        )

        val result = Await.result(sut.bankAccounts(nino), 5.seconds)

        result mustBe Seq(bankAccount1)

        verify(httpHandler, times(1)).getFromApi(
          Matchers.eq(s"http:///tai/$nino/tax-account/income/savings-investments/untaxed-interest/bank-accounts")
        )(any())
      }

      "api returns multiple bank accounts" in {

        val sut = createSut("http://")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(
          jsonBbsiDetails(jsonMultipleBankAccounts))
        )

        val result = Await.result(sut.bankAccounts(nino), 5.seconds)

        result mustBe Seq(bankAccount1, bankAccount2)

        verify(httpHandler, times(1)).getFromApi(
          Matchers.eq(s"http:///tai/$nino/tax-account/income/savings-investments/untaxed-interest/bank-accounts")
        )(any())
      }
    }


    "return untaxed interest" when {

      "api returns untaxed interest json" in {
        val sut = createSut("http://")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(jsonApiResponse(jsonUntaxedInterest)))

        val result = Await.result(sut.untaxedInterest(nino), 5.seconds)

        result mustBe Some(untaxedInterest)

        verify(httpHandler, times(1)).getFromApi(
          Matchers.eq(s"http:///tai/$nino/tax-account/income/savings-investments/untaxed-interest")
        )(any())
      }
    }

    "return None" when {

      "api does not return untaxed interest" in {
        val sut = createSut("http://")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(jsonApiResponse(Json.obj())))

        val result = Await.result(sut.untaxedInterest(nino), 5.seconds)

        result mustBe None

        verify(httpHandler, times(1)).getFromApi(
          Matchers.eq(s"http:///tai/$nino/tax-account/income/savings-investments/untaxed-interest")
        )(any())
      }
    }
  }
    "return exception" when {

     "return empty sequence of bank accounts" when {
      "api returns invalid json" in {

        val sut = createSut()

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(
          Json.obj("" -> "")
        ))

        val result = Await.result(sut.bankAccounts(nino), 5.seconds)

        result mustBe Seq.empty[BankAccount]
      }

      "api throws exception" in {

        val sut = createSut()

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new NotFoundException("")))

        val result = Await.result(sut.bankAccounts(nino), 5.seconds)

        result mustBe Seq.empty[BankAccount]
      }
    }
  }

  "bankAccount" should {
    "returns individual bank account" when {
      "a valid id is passed" in {
        val sut = createSut()

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(
          bankAccountJsonResponse))

        val result = Await.result(sut.bankAccount(nino, 1), 5.seconds)

        result mustBe Some(bankAccount1)

      }
    }

    "returns None" when {
      "an invalid id is passed" in {
        val sut = createSut()

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new RuntimeException))

        val result = Await.result(sut.bankAccount(nino, 1), 5.seconds)

        result mustBe None

      }
      "an unknown json object is returned" in {
        val sut = createSut()

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(JsString("Foo")))

        val result = Await.result(sut.bankAccount(nino, 1), 5.seconds)

        result mustBe None

      }
    }
  }

  "bankAccounts - closeBankAccount" should {

    "return an envelope id" when {
      "we send a PUT request to backend" in {

        val sut = createSut()

        val json = Json.obj("data"-> "123-456-789")

        when(httpHandler.putToApi(any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

        val result = Await.result(sut.closeBankAccount(nino, 1, CloseAccountRequest(new LocalDate(2017, 8, 8), None)), 5.seconds)

        result mustBe Some("123-456-789")
      }
    }

    "return an exception" when {
      "json is invalid" in {

        val sut = createSut()

        val json = Json.obj("test"-> "123-456-789")

        when(httpHandler.putToApi(any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

        val result = Await.result(sut.closeBankAccount(nino, 1, CloseAccountRequest(new LocalDate(2017, 8, 8), None)), 5.seconds)

        result mustBe None
      }
    }
  }

  "removeBankAccount" should {
    "return envelope id" in {
      val sut = createSut()
      val json = Json.obj("data"-> "123-456-789")
      when(httpHandler.deleteFromApi(any())(any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

      val result = Await.result(sut.removeBankAccount(nino, 1), 5.seconds)

      result mustBe Some("123-456-789")
    }

    "return None" when {
      "json is  invalid" in {
        val sut = createSut()
        val json = Json.obj("test"-> "123-456-789")
        when(httpHandler.deleteFromApi(any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

        val result = Await.result(sut.removeBankAccount(nino, 1), 5.seconds)

        result mustBe None
      }
    }
  }

  "updateBankAccount" should {
    "return envelope id" in {
      val sut = createSut()
      val json = Json.obj("data"-> "123-456-789")
      when(httpHandler.putToApi(any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

      val result = Await.result(sut.updateBankAccountInterest(nino, 1, amountRequest), 5.seconds)

      result mustBe Some("123-456-789")
    }

    "return None" when {
      "json is  invalid" in {
        val sut = createSut()
        val json = Json.obj("test"-> "123-456-789")
        when(httpHandler.putToApi(any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

        val result = Await.result(sut.updateBankAccountInterest(nino, 1, amountRequest), 5.seconds)

        result mustBe None
      }
    }
  }

  private val amountRequest = AmountRequest(2000)
  private val nino = new Generator().nextNino
  private val taxYear = LocalDate.now().getYear.toString

  private val accountNumber1 = "test account no 1"
  private val sortCode1 = "test sort code 1"
  private val bankName1 = "test bank name 1"
  private val grossInterest1 = 123.45
  private val source1 = "Customer1"

  private val accountNumber2 = "test account no 2"
  private val sortCode2 = "test sort code 2"
  private val bankName2 = "test bank name 2"
  private val grossInterest2 = 456.78
  private val source2 = "Customer2"

  private val bankAccount1 = BankAccount(1, accountNumber = Some(accountNumber1), sortCode = Some(sortCode1), bankName = Some(bankName1), grossInterest = grossInterest1, source = Some(source1))

  private val bankAccount2 = BankAccount(2, accountNumber = Some(accountNumber2), sortCode = Some(sortCode2), bankName = Some(bankName2), grossInterest = grossInterest2, source = Some(source2))

  private implicit val hc = HeaderCarrier()

  private val jsonBankAccount1 = Json.toJson(bankAccount1)

  private val jsonBankAccount2 = Json.toJson(bankAccount2)

  private val bankAccountJsonResponse = Json.obj("data" -> jsonBankAccount1)

  private def jsonBbsiDetails(data: JsArray) = Json.obj("nino" -> nino.nino, "taxYear" -> taxYear, "data" -> data)
  private def jsonApiResponse(data: JsValue) = Json.obj("data" -> data)


  private val jsonSingleBankAccounts = Json.arr(jsonBankAccount1)
  private val jsonMultipleBankAccounts = Json.arr(jsonBankAccount1, jsonBankAccount2)

  private val untaxedInterest = UntaxedInterest(200,Nil)
  private val savingsInvestments = SavingsInvestments(untaxedInterest)

  private val jsonUntaxedInterest = Json.toJson(untaxedInterest)

  def createSut(servUrl: String = "")  = new SUT(servUrl)
  
  val httpHandler = mock[HttpHandler]

  class SUT(servUrl: String = "")  extends BbsiConnector (httpHandler) {
    override val serviceUrl: String = servUrl
  }
}
