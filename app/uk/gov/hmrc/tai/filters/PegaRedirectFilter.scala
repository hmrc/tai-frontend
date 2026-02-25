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

import play.api.Configuration
import play.api.mvc._
import org.apache.pekko.stream.Materializer

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.tai.util.PathPatternMatcher.patternMatches
import uk.gov.hmrc.sca.utils.Keys.getTrustedHelperFromRequest

@Singleton
class PegaRedirectFilter @Inject() (config: Configuration)(implicit val mat: Materializer) extends Filter {

  private val pegaConfig = config.get[Configuration]("pega")

  private val enabled: Boolean = pegaConfig.getOptional[Boolean]("redirects.enabled").getOrElse(false)

  private val host: String = pegaConfig.get[String]("host")

  private val mappings: Map[String, String] = pegaConfig.get[Map[String, String]]("redirect-urls-mapping")

  override def apply(f: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    val isTrustedHelper: Boolean = getTrustedHelperFromRequest(requestHeader).isDefined

    if (isTrustedHelper || !enabled) {
      f(requestHeader)
    } else {
      val requestPath = requestHeader.path

      val redirectUrl = mappings.collectFirst {
        case (requestPathPattern, targetPath) if patternMatches(requestPathPattern, requestPath) => targetPath
      }

      redirectUrl match {
        case Some(path) => Future.successful(Results.Redirect(s"$host$path"))
        case None       => f(requestHeader)
      }
    }
  }
}
