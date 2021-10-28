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

package uk.gov.hmrc.tai.model.domain

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json}

class PaymentFrequencySpec extends PlaySpec {

  "Payment Frequency" must {
    "create a valid object" when {
      "user received a valid payment frequency" in {
        JsString("Weekly").as[PaymentFrequency] mustBe Weekly
        JsString("FortNightly").as[PaymentFrequency] mustBe FortNightly
        JsString("FourWeekly").as[PaymentFrequency] mustBe FourWeekly
        JsString("Monthly").as[PaymentFrequency] mustBe Monthly
        JsString("Quarterly").as[PaymentFrequency] mustBe Quarterly
        JsString("BiAnnually").as[PaymentFrequency] mustBe BiAnnually
        JsString("Annually").as[PaymentFrequency] mustBe Annually
        JsString("OneOff").as[PaymentFrequency] mustBe OneOff
        JsString("Irregular").as[PaymentFrequency] mustBe Irregular
      }
    }

    "throw an illegal exception" in {
      val ex = the[IllegalArgumentException] thrownBy JsString("NA").as[PaymentFrequency]
      ex.getMessage mustBe "Invalid payment frequency"
    }

    "create a valid json" when {
      "user received a valid payment frequency" in {
        Json.toJson[PaymentFrequency](Weekly) mustBe JsString("Weekly")
        Json.toJson[PaymentFrequency](FortNightly) mustBe JsString("FortNightly")
        Json.toJson[PaymentFrequency](FourWeekly) mustBe JsString("FourWeekly")
        Json.toJson[PaymentFrequency](Monthly) mustBe JsString("Monthly")
        Json.toJson[PaymentFrequency](Quarterly) mustBe JsString("Quarterly")
        Json.toJson[PaymentFrequency](BiAnnually) mustBe JsString("BiAnnually")
        Json.toJson[PaymentFrequency](Annually) mustBe JsString("Annually")
        Json.toJson[PaymentFrequency](OneOff) mustBe JsString("OneOff")
        Json.toJson[PaymentFrequency](Irregular) mustBe JsString("Irregular")
      }
    }
  }
}
