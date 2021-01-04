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

package controllers.i18n

import javax.inject.Inject
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig

class TaiLanguageController @Inject()(
  applicationConfig: ApplicationConfig,
  languageUtils: LanguageUtils,
  cc: ControllerComponents,
  val partialRetriever: FormPartialRetriever,
  val templateRenderer: TemplateRenderer)(implicit messagesApi: MessagesApi)
    extends LanguageController(applicationConfig.runModeConfiguration, languageUtils, cc) {

  override protected def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  override protected def fallbackURL: String = controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage.url

  def english(): Action[AnyContent] = switchToLanguage(language = "english")
  def welsh(): Action[AnyContent] = switchToLanguage(language = "cymraeg")
}
