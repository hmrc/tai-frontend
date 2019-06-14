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

package controllers.i18n

import com.google.inject.Inject
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Environment
import play.api.Play.current
import play.api.i18n.Lang
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.language.LanguageController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig

class TaiLanguageController @Inject()(authenticate: AuthAction,
                                      validatePerson: ValidatePerson,
                                      override implicit val partialRetriever: FormPartialRetriever,
                                      override implicit val templateRenderer: TemplateRenderer,
                                      implicit val environment: Environment) extends LanguageController
  with TaiBaseController
  with FeatureTogglesConfig {

  override protected def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  override protected def fallbackURL: String = controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage.url
}
