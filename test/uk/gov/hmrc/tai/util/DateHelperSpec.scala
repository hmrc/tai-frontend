/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec

class DateHelperSpec extends PlaySpec {

  "Date helper month" must {
    "return month" when {
      "provided with a valid date in any alpha format" in {
        DateHelper.monthOfYear("28 February 2018") must be("February")
      }
    }

    "return empty string" when {
      "provided with numerical date" in {
        DateHelper.monthOfYear("28/2/2018") must be("")
      }
    }
  }

  "dateTimeFormat" must {
    "return dd MMM yyyy 'at' HH:mm" when {
      "given valid DateTime" in {
        val dt = DateTime.parse("2022-12-21T13:04:55.942Z")
        DateHelper.dateTimeFormat(dt) must be("21 December 2022 at 13:04")
      }
    }
  }

}
