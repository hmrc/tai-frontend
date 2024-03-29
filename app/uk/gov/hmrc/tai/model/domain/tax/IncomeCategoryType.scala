/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model.domain.tax

import play.api.libs.json.{Format, JsResult, JsString, JsSuccess, JsValue}

sealed trait IncomeCategoryType
case object NonSavingsIncomeCategory extends IncomeCategoryType
case object UntaxedInterestIncomeCategory extends IncomeCategoryType
case object BankInterestIncomeCategory extends IncomeCategoryType
case object UkDividendsIncomeCategory extends IncomeCategoryType
case object ForeignInterestIncomeCategory extends IncomeCategoryType
case object ForeignDividendsIncomeCategory extends IncomeCategoryType

object IncomeCategoryType {
  implicit val incomeCategoryTypeFormats: Format[IncomeCategoryType] = new Format[IncomeCategoryType] {
    override def reads(json: JsValue): JsResult[IncomeCategoryType] =
      json.as[String] match {
        case "NonSavingsIncomeCategory"       => JsSuccess(NonSavingsIncomeCategory)
        case "UntaxedInterestIncomeCategory"  => JsSuccess(UntaxedInterestIncomeCategory)
        case "BankInterestIncomeCategory"     => JsSuccess(BankInterestIncomeCategory)
        case "UkDividendsIncomeCategory"      => JsSuccess(UkDividendsIncomeCategory)
        case "ForeignInterestIncomeCategory"  => JsSuccess(ForeignInterestIncomeCategory)
        case "ForeignDividendsIncomeCategory" => JsSuccess(ForeignDividendsIncomeCategory)
        case _                                => throw new IllegalArgumentException("Invalid income category type")
      }

    override def writes(o: IncomeCategoryType): JsValue = JsString(o.toString)
  }
}
