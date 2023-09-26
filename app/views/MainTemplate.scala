/*
 * Copyright 2023 HM Revenue & Customs
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

package views.html

import com.google.inject.{ImplementedBy, Inject}
import uk.gov.hmrc.tai.config.ApplicationConfig
import play.api.Logging
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.sca.models.BannerConfig
import uk.gov.hmrc.sca.services.WrapperService
import uk.gov.hmrc.tai.model.admin.SCAWrapperToggle

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import views.html.oldMainTemplate
import views.html.includes.{AdditionalJavascript, HeadBlock}

@ImplementedBy(classOf[MainTemplateImpl])
trait MainTemplate {
  def apply(
    title: String,
    authedUser: Option[controllers.auth.AuthedUser] = None,
    pageTitle: Option[String] = None,
    backLinkUrl: Option[String] = Some("#"),
    backLinkContent: Option[String],
    backLinkId: String = "back-link",
    disableSessionExpired: Boolean = false,
    pagePrintable: Boolean = false,
    pagePrintName: Option[String] = None,
    showPtaAccountNav: Boolean = true,
    formForErrorSummary: Option[Form[_]] = None
  )(content: Html)(implicit request: Request[_], messages: Messages): HtmlFormat.Appendable
}

class MainTemplateImpl @Inject() (
  appConfig: ApplicationConfig,
  featureFlagService: FeatureFlagService,
  wrapperService: WrapperService,
  oldLayout: oldMainTemplate,
  scripts: AdditionalJavascript,
  headBlock: HeadBlock
) extends MainTemplate with Logging {
  override def apply(
    title: String,
    authedUser: Option[controllers.auth.AuthedUser] = None,
    pageTitle: Option[String] = None,
    backLinkUrl: Option[String] = Some("#"),
    backLinkContent: Option[String],
    backLinkId: String = "back-link",
    disableSessionExpired: Boolean = false,
    pagePrintable: Boolean = false,
    pagePrintName: Option[String] = None,
    showPtaAccountNav: Boolean = true,
    formForErrorSummary: Option[Form[_]] = None
  )(content: Html)(implicit request: Request[_], messages: Messages): HtmlFormat.Appendable = {
    val scaWrapperToggle =
      Await.result(featureFlagService.get(SCAWrapperToggle), Duration(appConfig.SCAWrapperFutureTimeout, SECONDS))

    val prefix =
      if (formForErrorSummary.exists(_.errors.nonEmpty)) {
        s"${Messages("tai.page.title.error")} "
      } else {
        ""
      }
    val fullPageTitle = s"$prefix$title - ${Messages("tai.currentYearSummary.heading")} - GOV.UK"

    if (scaWrapperToggle.isEnabled) {
      logger.debug(s"SCA Wrapper layout used for request `${request.uri}``")
      wrapperService.layout(
        content = content,
        pageTitle = Some(fullPageTitle),
        serviceNameKey = Some(messages(pageTitle.getOrElse("tai.service.navTitle"))),
        serviceNameUrl = Some(appConfig.taiHomePageUrl),
//      sidebarContent: Option[Html] = None,
        signoutUrl = controllers.routes.ServiceController.serviceSignout().url,
        timeOutUrl = Some(controllers.routes.ServiceController.sessionExpiredPage().url),
        keepAliveUrl = controllers.routes.ServiceController.keepAlive().url,
        showBackLinkJS = backLinkContent.isDefined && backLinkUrl.contains("#"),
        backLinkUrl = if (backLinkContent.isDefined) backLinkUrl else None,
        // showSignOutInHeader: Boolean = false,
        styleSheets = Seq(headBlock()),
        scripts = Seq(scripts()),
        bannerConfig = BannerConfig(false, true, false),
        // optTrustedHelper: Option[TrustedHelper] = None,
        fullWidth = true,
        hideMenuBar = !showPtaAccountNav,
        disableSessionExpired = disableSessionExpired
      )(messages, HeaderCarrierConverter.fromRequest(request), request)
    } else {
      logger.debug(s"Old layout used for request `${request.uri}``")
      oldLayout(
        title,
        authedUser,
        pageTitle,
        backLinkUrl,
        backLinkContent,
        backLinkId,
        disableSessionExpired,
        pagePrintable,
        pagePrintName,
        showPtaAccountNav,
        formForErrorSummary
      )(content)(request, messages)
    }
  }
}
