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

package controllers

import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import views.html.{error_no_primary, error_template_noauth}

abstract class TaiBaseController(mcc: MessagesControllerComponents)
    extends FrontendController(mcc) with ErrorPagesHandler with I18nSupport {

  val error_template_noauth: error_template_noauth
  val error_no_primary: error_no_primary

  implicit def templateRenderer: TemplateRenderer
  implicit def partialRetriever: FormPartialRetriever

}
