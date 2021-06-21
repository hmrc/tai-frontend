package uk.gov.hmrc.tai.connectors

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.domain.Generator
import utils.WireMockHelper

class HttpHandlerSpec2 extends WordSpec with GuiceOneAppPerSuite with MustMatchers with WireMockHelper with ScalaFutures with IntegrationPatience with Injecting {

  val generatedNino = new Generator().nextNino

  val generatedSaUtr = new Generator().nextAtedUtr

  lazy val messages = inject[Messages]

  lazy val httpHandler = inject[HttpHandler]

  "getFromApiV2" must {
    "should return a json when OK" in {




    }




  }







}
