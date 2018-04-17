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

import controllers.auth.TaiUser
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.model.{TaiRoot, TaxSummaryDetails}

import scala.concurrent.Future

trait ServiceChecks extends TaiBaseController {
  type CustomRule = (TaxSummaryDetails) => Future[Result]
  def runServiceChecks(e:Either[String,TaxSummaryDetails])(custom: Option[CustomRule])(implicit user: TaiUser, hc: HeaderCarrier) : Future[Result]
}

object SimpleServiceCheck extends ServiceChecks {

  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

  override def runServiceChecks(either: Either[String,TaxSummaryDetails])
                               (custom: Option[CustomRule])
                               (implicit user: TaiUser, hc: HeaderCarrier): Future[Result] = {
    either match {
      case Left("deceased") => Future.successful(Redirect(routes.DeceasedController.deceased()))
      case Left("mci") => Future.successful(Redirect(routes.ServiceController.gateKeeper()))
      case Left("ceased") => Future.successful(Redirect(routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage()))
      case Right(details:TaxSummaryDetails) => custom.getOrElse(throw new Exception("Method not defined"))(details)
    }
  }
}

object ServiceCheckLite extends TaiBaseController {

  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

  def personDetailsCheck(proceed: Future[Result])(implicit taiRoot: TaiRoot): Future[Result] = {
    if (taiRoot.deceasedIndicator.getOrElse(false)) {
      Future.successful(Redirect(routes.DeceasedController.deceased()))
    } else if (taiRoot.manualCorrespondenceInd) {
      Future.successful(Redirect(routes.ServiceController.gateKeeper()))
    } else {
      proceed
    }
  }
}
