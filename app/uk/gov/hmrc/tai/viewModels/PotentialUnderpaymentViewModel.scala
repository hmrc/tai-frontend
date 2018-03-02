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

package uk.gov.hmrc.tai.viewModels

case class PotentialUnderpaymentViewModel(
  displayCYOnly:Boolean = false,
  displayCYPlusOneOnly:Boolean = false,
  displayCYAndCYPlusOneOnly:Boolean = false,
  displayNoValues:Boolean = true,
  iyaCYAmount:BigDecimal,
  iyaCYPlusOneAmount:BigDecimal,
  iyaTotalAmount:BigDecimal,
  iyaTaxCodeChangeAmount:BigDecimal){

  def gaDimensions: Option[Map[String, String]] = {
    if(displayCYOnly){
      Some(Map("valueOfIycdcPayment" -> iyaCYAmount.toString(), "iycdcReconciliationStatus" -> "Current Year"))
    } else if(displayCYPlusOneOnly){
      Some(Map("valueOfIycdcPayment" -> iyaCYPlusOneAmount.toString(), "iycdcReconciliationStatus" -> "Next Year"))
    } else if(displayCYAndCYPlusOneOnly){
      Some(Map("valueOfIycdcPayment" -> iyaCYAmount.toString(), "iycdcReconciliationStatus" -> "Current and Next Year"))
    } else {
      None
    }
  }

}

