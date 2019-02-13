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

package uk.gov.hmrc.tai.util

import builders.RequestBuilder
import controllers.FakeTaiPlayApplication
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec


class ReferralSpec extends PlaySpec with MustMatchers with FakeTaiPlayApplication {
  val referral = new Referral{}

  "referer" must {
    "return a string containing the referral path" in {

      val request = RequestBuilder.buildFakeRequestWithAuth("GET", Map("Referer" -> referralPath))

      referral.referer(request) mustBe referralPath
    }
  }

  "resourceName" must {
    "return a string containing the resource name" in {

      val request = RequestBuilder.buildFakeRequestWithAuth("GET", Map("Referer" -> referralPath))

      referral.resourceName(request) mustBe "somePageResource"
    }
  }

  private val referralPath = "http://somelocation/somePageResource"
}
