/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.tai.filters

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, Configuration}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.sca.models.{PtaMinMenuConfig, TrustedHelper, WrapperDataResponse}
import uk.gov.hmrc.sca.utils.Keys

import scala.concurrent.Future

class PegaRedirectFilterSpec extends PlaySpec {

  private implicit val system: ActorSystem = ActorSystem("pega-redirect-filter")

  private implicit val mat: Materializer = Materializer(system)

  private def buildApp(redirectsEnabled: Boolean): Application = {
    val config = ConfigFactory.parseString(
      s"""
        pega {
          host = "http://localhost:9999"
          redirects.enabled = $redirectsEnabled
          redirect-urls-mapping {
              "/check-income-tax/income-summary" = "/pay-as-you-earn/paye/summary"
              "/check-income-tax/income-details/:empId" = "/pay-as-you-earn/paye/summary"
          }
        }
      """
    )
    new GuiceApplicationBuilder().loadConfig(env => Configuration(config.withFallback(ConfigFactory.load()))).build()
  }

  val nextFilter: RequestHeader => Future[Result] = _ => Future.successful(Results.Ok("NextFilter"))

  "PegaRedirectFilter" should {

    "shouldn't redirect for trusted helpers even when redirects enabled and mapping exists for the requested path" in {

      val filter = new PegaRedirectFilter(buildApp(true).configuration)

      val nino          = new Generator().nextNino
      val trustedHelper = TrustedHelper("principalName", "attorneyName", "returnLinkUrl", Some(nino.nino))

      val request = FakeRequest(GET, "/check-income-tax/income-summary")
        .addAttr(
          Keys.wrapperDataKey,
          WrapperDataResponse(
            Nil,
            PtaMinMenuConfig("", ""),
            Nil,
            Nil,
            Some(0),
            Some(trustedHelper)
          )
        )

      val result = filter.apply(nextFilter)(request)

      status(result) mustBe OK
      contentAsString(result) mustBe "NextFilter"
    }

    "redirect to pega when redirects enabled and mapping exists for the requested path" in {

      val filter = new PegaRedirectFilter(buildApp(true).configuration)

      val request = FakeRequest(GET, "/check-income-tax/income-summary")

      val result = filter.apply(nextFilter)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("http://localhost:9999/pay-as-you-earn/paye/summary")
    }

    "redirect to pega when redirects enabled and prefix mapping matches " in {
      val filter = new PegaRedirectFilter(buildApp(true).configuration)

      val request = FakeRequest(GET, "/check-income-tax/income-details/777")

      val result = filter.apply(nextFilter)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("http://localhost:9999/pay-as-you-earn/paye/summary")
    }

    "Don't redirect to pega when redirect enabled but no mapping matches" in {

      val filter = new PegaRedirectFilter(buildApp(false).configuration)

      val request = FakeRequest(GET, "/some/other/path")

      val result = filter.apply(nextFilter)(request)

      status(result) mustBe OK
    }

    "Dont redirect when pega redirects are not enabled" in {

      val filter = new PegaRedirectFilter(buildApp(false).configuration)

      val request = FakeRequest(GET, "/check-income-tax/income-summary")

      val result = filter.apply(nextFilter)(request)

      status(result) mustBe OK
    }
  }
}
