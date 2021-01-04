/*
 * Copyright 2021 HM Revenue & Customs
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

package utils.factories

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.tai.model.domain.TaxCodeMismatch

object TaxCodeMismatchFactory {

  def matchedTaxCode: TaxCodeMismatch =
    TaxCodeMismatch(false, Seq("1185L"), Seq("1185L"))

  def mismatchedTaxCode: TaxCodeMismatch =
    TaxCodeMismatch(true, Seq("1185L"), Seq("0T"))

  def mismatchedTaxCodeComplex: TaxCodeMismatch =
    TaxCodeMismatch(true, Seq("1185L", "0T"), Seq("1180L", "0T"))

  def matchedTaxCodeJson: JsObject =
    Json.obj(
      "data" -> Json.obj(
        "mismatch"            -> false,
        "unconfirmedTaxCodes" -> Json.arr("1185L"),
        "confirmedTaxCodes"   -> Json.arr("1185L")
      )
    )

  def mismatchedTaxCodeJson: JsObject =
    Json.obj(
      "data" -> Json.obj(
        "mismatch"            -> true,
        "unconfirmedTaxCodes" -> Json.arr("1185L"),
        "confirmedTaxCodes"   -> Json.arr("0T")
      )
    )
}
