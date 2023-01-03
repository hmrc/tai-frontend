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

package uk.gov.hmrc.tai

import play.api.Logging
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.UpdateOrRemoveCompanyBenefitDecisionConstants._

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DecisionCacheWrapper @Inject()(@Named("End Company Benefit") journeyCacheService: JourneyCacheService)
    extends Results with Logging {

  private val journeyStartRedirection = Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad().url)

  def getDecision()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    val benefitType = journeyCacheService.mandatoryJourneyValue(EndCompanyBenefitConstants.BenefitTypeKey)
    benefitType.flatMap[Option[String]] {
      case Right(bt) =>
        getBenefitDecisionKey(Some(bt)) match {
          case Some(bdk) =>
            journeyCacheService.currentValue(bdk)
          case _ =>
            logger.error(s"Unable to form compound key for $DecisionChoice using $benefitType")
            Future.successful(None)
        }
      case Left(_) =>
        logger.error(s"Unable to find ${EndCompanyBenefitConstants.BenefitTypeKey} when retrieving decision")
        Future.successful(None)
    }
  }

  def cacheDecision(decision: String, f: (String, Result) => Result)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] = {
    val benefitType = journeyCacheService.mandatoryJourneyValue(EndCompanyBenefitConstants.BenefitTypeKey)
    benefitType.flatMap[Result] {
      case Right(bt) =>
        getBenefitDecisionKey(Some(bt)) match {
          case Some(bdk) =>
            journeyCacheService.cache(bdk, decision).map { _ =>
              f(decision, journeyStartRedirection)
            }
          case _ =>
            logger.error(s"Unable to form compound key for $DecisionChoice using $benefitType")
            Future.successful(journeyStartRedirection)
        }
      case Left(_) =>
        logger.error(s"Unable to find ${EndCompanyBenefitConstants.BenefitTypeKey} when retrieving decision")
        Future.successful(journeyStartRedirection)
    }
  }

  private def getBenefitDecisionKey(benefitType: Option[String]): Option[String] =
    benefitType.map(x => s"$x $DecisionChoice")
}
