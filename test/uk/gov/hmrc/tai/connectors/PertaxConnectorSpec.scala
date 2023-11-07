/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.tai.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, ok, post, urlEqualTo}
import org.scalatest.concurrent.IntegrationPatience
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.model.{ErrorView, PertaxResponse}
import utils.{BaseSpec, WireMockHelper}

class PertaxConnectorSpec extends BaseSpec with WireMockHelper with IntegrationPatience {

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure("microservice.services.pertax.port" -> server.port())
    .build()

  lazy val pertaxConnector: PertaxConnector = inject[PertaxConnector]

  def authoriseUrl() = s"/pertax/authorise"

  "PertaxConnector" must {
    "return a PertaxResponse with ACCESS_GRANTED code" in {
      server.stubFor(
        post(urlEqualTo(authoriseUrl()))
          .willReturn(ok("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}"))
      )

      val result = pertaxConnector
        .pertaxPostAuthorise()
        .value
        .futureValue
        .getOrElse(PertaxResponse("INCORRECT RESPONSE", "INCORRECT", None, None))
      result mustBe PertaxResponse("ACCESS_GRANTED", "Access granted", None, None)
    }

    "return a PertaxResponse with NO_HMRC_PT_ENROLMENT code with a redirect link" in {
      server.stubFor(
        post(urlEqualTo(authoriseUrl()))
          .willReturn(
            ok(
              "{\"code\": \"NO_HMRC_PT_ENROLMENT\", \"message\": \"There is no valid HMRC PT enrolment\", \"redirect\": \"/tax-enrolment-assignment-frontend/account\"}"
            )
          )
      )

      val result = pertaxConnector
        .pertaxPostAuthorise()
        .value
        .futureValue
        .getOrElse(PertaxResponse("INCORRECT RESPONSE", "INCORRECT", None, None))
      result mustBe PertaxResponse(
        "NO_HMRC_PT_ENROLMENT",
        "There is no valid HMRC PT enrolment",
        None,
        Some("/tax-enrolment-assignment-frontend/account")
      )
    }

    "return a PertaxResponse with INVALID_AFFINITY code and an errorView" in {
      server.stubFor(
        post(urlEqualTo(authoriseUrl()))
          .willReturn(
            ok(
              "{\"code\": \"INVALID_AFFINITY\", \"message\": \"The user is neither an individual or an organisation\", \"errorView\": {\"url\": \"/path/for/partial\", \"statusCode\": 401}}"
            )
          )
      )

      val result = pertaxConnector
        .pertaxPostAuthorise()
        .value
        .futureValue
        .getOrElse(PertaxResponse("INCORRECT RESPONSE", "INCORRECT", None, None))
      result mustBe PertaxResponse(
        "INVALID_AFFINITY",
        "The user is neither an individual or an organisation",
        Some(ErrorView("/path/for/partial", UNAUTHORIZED)),
        None
      )
    }

    "return a PertaxResponse with MCI_RECORD code and an errorView" in {
      server.stubFor(
        post(urlEqualTo(authoriseUrl()))
          .willReturn(
            ok(
              "{\"code\": \"MCI_RECORD\", \"message\": \"Manual correspondence indicator is set\", \"errorView\": {\"url\": \"/path/for/partial\", \"statusCode\": 423}}"
            )
          )
      )

      val result = pertaxConnector
        .pertaxPostAuthorise()
        .value
        .futureValue
        .getOrElse(PertaxResponse("INCORRECT RESPONSE", "INCORRECT", None, None))
      result mustBe PertaxResponse(
        "MCI_RECORD",
        "Manual correspondence indicator is set",
        Some(ErrorView("/path/for/partial", 423)),
        None
      )
    }

    "return a UpstreamErrorResponse with the correct error code" when {

      List(
        BAD_REQUEST,
        NOT_FOUND,
        FORBIDDEN,
        INTERNAL_SERVER_ERROR
      ).foreach { error =>
        s"an $error is returned from the backend" in {

          server.stubFor(
            post(urlEqualTo(authoriseUrl())).willReturn(
              aResponse()
                .withStatus(error)
            )
          )

          val result = pertaxConnector
            .pertaxPostAuthorise()
            .value
            .futureValue
            .swap
            .getOrElse(UpstreamErrorResponse("INCORRECT RESPONSE", IM_A_TEAPOT))
          result.statusCode mustBe error
        }
      }
    }
  }
}
