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

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.model.domain.{BankAccount, UntaxedInterest}
import uk.gov.hmrc.tai.model.{AmountRequest, CloseAccountRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class BbsiConnector @Inject() (httpHandler: HttpHandler) extends DefaultServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def bankAccounts(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[BankAccount]] = {
    httpHandler.getFromApi(bbsiAccountsUrl(nino)) map ( json => (json \ "data").as[Seq[BankAccount]]) recover {
      case _: Exception =>
        Logger.warn(s"Exception generated while reading bank-accounts for nino $nino")
        Seq.empty[BankAccount]
    }
  }

  def bankAccount(nino:Nino, id:Int)(implicit hc: HeaderCarrier) : Future[Option[BankAccount]] = {
    httpHandler.getFromApi(bbsiAccountUrl(nino, id)) map {
      json: JsValue => (json \ "data").asOpt[BankAccount]

    } recover {
      case NonFatal(_) =>
        Logger.warn(s"Could not find bank account for nino: $nino and id: $id")
        None
    }
  }

  def closeBankAccount(nino: Nino, id: Int, closeAccountRequest: CloseAccountRequest)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    httpHandler.putToApi[CloseAccountRequest](bbsiEndAccountUrl(nino, id), closeAccountRequest).map(response => (response.json \ "data").asOpt[String])
  }


  def untaxedInterest(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[UntaxedInterest]] = {
    httpHandler.getFromApi(bbsiSavingsInvestmentsUrl(nino)) map {
      json: JsValue => (json \ "data").asOpt[UntaxedInterest]
    }
  }


  def removeBankAccount(nino: Nino, id: Int)(implicit hc: HeaderCarrier): Future[Option[String]] ={
    httpHandler.deleteFromApi(bbsiAccountUrl(nino, id)).map(response => (response.json \ "data").asOpt[String])
  }

  def updateBankAccountInterest(nino: Nino, id: Int, amountRequest: AmountRequest)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    httpHandler.putToApi[AmountRequest](bbsiUpdateAccountUrl(nino, id), amountRequest).map(response => (response.json \ "data").asOpt[String])
  }

  def bbsiAccountsUrl(nino: Nino): String = s"$serviceUrl/tai/$nino/tax-account/income/savings-investments/untaxed-interest/bank-accounts"
  def bbsiAccountUrl(nino: Nino, id: Int): String = s"$serviceUrl/tai/$nino/tax-account/income/savings-investments/untaxed-interest/bank-accounts/$id"
  def bbsiEndAccountUrl(nino: Nino, id: Int): String = s"$serviceUrl/tai/$nino/tax-account/income/savings-investments/untaxed-interest/bank-accounts/$id/closedAccount"
  def bbsiUpdateAccountUrl(nino: Nino, id: Int): String = s"$serviceUrl/tai/$nino/tax-account/income/savings-investments/untaxed-interest/bank-accounts/$id/interest-amount"
  def bbsiSavingsInvestmentsUrl(nino: Nino): String = s"$serviceUrl/tai/$nino/tax-account/income/savings-investments/untaxed-interest"

}
