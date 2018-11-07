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

package controllers.auth

import builders.RequestBuilder.buildFakeRequestWithoutAuth
import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.{L100, L200, L300}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.CredentialStrength.Strong
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, PageBlocked, PageIsVisible}
import uk.gov.hmrc.tai.util.constants.TaiConstants.{CompletionUrl, FailureUrl, Origin}
import uk.gov.hmrc.tai.util.constants.TaiConstants

import scala.concurrent.Await
import scala.concurrent.duration._


class TaiConfidenceLevelPredicateSpec extends PlaySpec with FakeTaiPlayApplication{

  "Confidence Level Predicate" should {

    "return page is visible" when {
      "confidence is equal to 200" in {
        val sut = createSUT
        val resp = Await.result(sut(authContext(L200), buildFakeRequestWithoutAuth("POST")), 5 seconds)
        resp mustBe PageIsVisible
      }

      "confidence is greater than 200" in {
        val sut = createSUT
        val resp = Await.result(sut(authContext(L300), buildFakeRequestWithoutAuth("POST")), 5 seconds)
        resp mustBe PageIsVisible
      }
    }

    "return page blocked" when {
      "confidence is below 200" in {
        val sut = createSUT
        val resp = Await.result(sut(authContext(L100), buildFakeRequestWithoutAuth("POST")), 5 seconds)

        resp.isInstanceOf[PageBlocked] mustBe true
        resp.isVisible mustBe false

      }
    }

    "redirect to uplift url" when {
      "confidence is below 200" in {
        val sut = createSUT
        val resp = Await.result(Await.result(sut(authContext(L100), buildFakeRequestWithoutAuth("POST")), 5 seconds).nonVisibleResult, 5 second)

        resp.header.status mustBe 303
        resp.header.headers("Location") mustBe s"/uplift?$Origin=TAI&${TaiConstants.ConfidenceLevel}=200&$CompletionUrl=%2Fcomplete&$FailureUrl=%2Ffailure"
      }
    }

  }

  def authContext(confidenceLevel: ConfidenceLevel) = {

    val nino = new Generator().nextNino

    AuthContext(
      Authority("oid/1234",
        Accounts(paye = Some(PayeAccount("", nino))), None, None,
        Strong, confidenceLevel, None, None, None, ""))
  }

  def createSUT = new SUT

  class SUT extends TaiConfidenceLevelPredicate {
    override def upliftUrl: String = "/uplift"

    override def failureUrl: String = "/failure"

    override def completionUrl: String = "/complete"

    override def origin: String = "TAI"

  }

}
