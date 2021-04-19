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

import controllers.auth.AuthAction

import javax.inject.Inject
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import views.html.{deceased_helpline, error_no_primary, error_template_noauth}

import scala.concurrent.Future

class DeceasedController @Inject()(
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  deceased_helpline: deceased_helpline,
  override val error_template_noauth: error_template_noauth,
  override val error_no_primary: error_no_primary,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)
    extends TaiBaseController(mcc) {

  def deceased() = authenticate.async(implicit request => Future.successful(Ok(deceased_helpline())))

}
