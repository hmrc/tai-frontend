/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.tai

import controllers.auth.AuthJourney
import pages.BenefitDecisionPage
import pages.benefits.EndCompanyBenefitsTypePage
import play.api.Logging
import play.api.mvc._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.util.constants.UpdateOrRemoveCompanyBenefitDecisionConstants._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DecisionCacheWrapper @Inject() (
  authenticate: AuthJourney,
  journeyCacheNewRepository: JourneyCacheNewRepository,
  implicit val ec: ExecutionContext
) extends Results with Logging {

  private val journeyStartRedirection = Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad().url)

  def getDecision: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val benefitType: Option[String] = request.userAnswers.get(EndCompanyBenefitsTypePage)

    benefitType match {
      case Some(bt) =>
        getBenefitDecisionKey(Some(bt)) match {
          case Some(_) =>
            Future.successful(Ok(request.userAnswers.get(EndCompanyBenefitsTypePage).toString))
          case None =>
            logger.error(s"Unable to form compound key for $DecisionChoice using $benefitType")
            Future.successful(NotFound("Unable to form compound key"))
        }
      case None =>
        Future.successful(NotFound("Benefit type not found"))
    }
  }

  def cacheDecision(decision: String, f: (String, Result) => Result): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      val benefitTypeFuture: Option[String] = request.userAnswers.get(EndCompanyBenefitsTypePage)

      benefitTypeFuture match {
        case Some(bt) =>
          getBenefitDecisionKey(Some(bt)) match {
            case Some(_) =>
              journeyCacheNewRepository.set(request.userAnswers.setOrException(BenefitDecisionPage, decision)).map {
                _ =>
                  f(decision, journeyStartRedirection)
              }
            case _ =>
              Future.successful(journeyStartRedirection)
          }
        case None =>
          Future.successful(journeyStartRedirection)
      }
    }

  private def getBenefitDecisionKey(benefitType: Option[String]): Option[String] =
    benefitType.map(x => s"$x ${BenefitDecisionPage.toString}")
}
