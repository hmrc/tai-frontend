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

package controllers

import play.api.mvc.Result
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.model.TaiRoot
import uk.gov.hmrc.tai.model.domain.Person

import scala.concurrent.Future

object ServiceCheckLite extends TaiBaseController {

  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

  def personDetailsCheck(proceed: Future[Result])(implicit person: Person): Future[Result] = {
    if (person.isDeceased) {
      Future.successful(Redirect(routes.DeceasedController.deceased()))
    } else if (person.hasCorruptData) {
      Future.successful(Redirect(routes.ServiceController.gateKeeper()))
    } else {
      proceed
    }
  }
}
