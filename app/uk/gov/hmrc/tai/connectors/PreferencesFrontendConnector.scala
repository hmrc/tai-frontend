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

package uk.gov.hmrc.tai.connectors

import java.net.URLEncoder

import controllers.routes
import play.api.Logger
import play.api.Play.current
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.crypto.{ApplicationCrypto, Encrypter, PlainText}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{CorePut, HttpReads, HttpResponse, RawReads}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import uk.gov.hmrc.tai.auth.ConfigProperties
import uk.gov.hmrc.tai.config.WSHttp
import uk.gov.hmrc.tai.connectors.PreferencesFrontendConnector.ActivatePaperlessResponse
import uk.gov.hmrc.tai.connectors.PreferencesFrontendConnector.ActivatePaperlessResponse.ActivationNotAllowed

object PreferencesFrontendConnector extends PreferencesFrontendConnector with ServicesConfig {
  override val http = WSHttp

  override lazy val baseUrl: String = baseUrl("preferences-frontend")

  override lazy val queryParamEcrypter: Encrypter = ApplicationCrypto.QueryParameterCrypto

  override lazy val crypto = SessionCookieCryptoFilter.encrypt _

  override lazy val returnUrl: String = s"${ConfigProperties.taxPlatformTaiRootUri}${routes.TaxAccountSummaryController.onPageLoad().url}"

  override lazy val returnLinkText: String = Messages("tai.estimatedIncomeTax.paperlessActivation.returnLinkText")

  sealed trait ActivatePaperlessResponse
  object ActivatePaperlessResponse {
    case object Activated extends ActivatePaperlessResponse
    case object ActivationNotAllowed extends ActivatePaperlessResponse
    case class ActivationRequiresUserAction(redirectUrl: String) extends ActivatePaperlessResponse

    implicit val reads: HttpReads[ActivatePaperlessResponse] = new HttpReads[ActivatePaperlessResponse] with RawReads {
      def read(method: String, url: String, response: HttpResponse) = response.status match {
        case PRECONDITION_FAILED  => ActivationRequiresUserAction((response.json \ "redirectUserTo").as[String])
        case OK | CREATED         => Activated
        case _                    => ActivationNotAllowed
      }
    }
  }
}

trait PreferencesFrontendConnector extends HeaderCarrierForPartialsConverter {

  def http: CorePut

  def baseUrl: String

  def returnUrl: String

  def returnLinkText: String

  def queryParamEcrypter: Encrypter

  private val activationPayload = Json.obj("active" -> true)

  def activatePaperless(nino: Nino)(implicit request: RequestHeader) =
    http.PUT[JsValue, ActivatePaperlessResponse](
      url = url(nino),
      body = activationPayload
    ) recover {
      case e =>
        Logger.error(s"Exception during paperless activation: ${e.getMessage}")
        ActivationNotAllowed
    }

  private def url(nino: Nino) =
    s"$baseUrl/paperless/activate/notice-of-coding/${nino.value}?returnUrl=${encryptAndEncode(returnUrl)}&returnLinkText=${encryptAndEncode(returnLinkText)}"

  private def encryptAndEncode(text: String) =
    URLEncoder.encode(queryParamEcrypter.encrypt(PlainText(text)).value, "UTF-8")
}

