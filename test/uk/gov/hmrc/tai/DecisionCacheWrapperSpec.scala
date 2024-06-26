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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.tai.model.domain.Telephone
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.UpdateOrRemoveCompanyBenefitDecisionConstants.YesIGetThisBenefit
import uk.gov.hmrc.tai.util.constants.journeyCache._
import utils.BaseSpec

import scala.concurrent.Future

class DecisionCacheWrapperSpec extends BaseSpec with Results {

  val journeyCacheService = mock[JourneyCacheService]
  val wrapper = new DecisionCacheWrapper(journeyCacheService, ec)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(journeyCacheService)
  }
  "getDecision" must {
    "return a None" when {
      "there is no cached BenefitType" in {

        when(journeyCacheService.mandatoryJourneyValue(meq(EndCompanyBenefitConstants.BenefitTypeKey))(any()))
          .thenReturn(Future.successful(Left("")))

        val result = wrapper.getDecision()
        whenReady(result) { r =>
          r mustBe None
        }
      }

      "there there is no cached Decision" in {
        when(journeyCacheService.mandatoryJourneyValue(meq(EndCompanyBenefitConstants.BenefitTypeKey))(any()))
          .thenReturn(Future.successful(Right(Telephone.name)))
        when(journeyCacheService.currentValue(any())(any()))
          .thenReturn(Future.successful(None))

        val result = wrapper.getDecision()
        whenReady(result) { r =>
          r mustBe None
        }
      }
    }

    "return the cached decision" when {
      "there is a cached value for the key given" in {
        when(journeyCacheService.mandatoryJourneyValue(meq(EndCompanyBenefitConstants.BenefitTypeKey))(any()))
          .thenReturn(Future.successful(Right(Telephone.name)))
        when(journeyCacheService.currentValue(any())(any()))
          .thenReturn(Future.successful(Option(YesIGetThisBenefit)))

        val result = wrapper.getDecision()
        whenReady(result) { r =>
          r mustBe Some(YesIGetThisBenefit)
        }
      }
    }
  }

  "cacheDecision" must {
    "cache the result and return the function" when {
      "we are able to generate a benefitDecisionKey" in {
        when(journeyCacheService.mandatoryJourneyValue(any())(any()))
          .thenReturn(Future.successful(Right(Telephone.name)))
        when(journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map("" -> "")))
        val function = (_: String, b: Result) => b
        val result = wrapper.cacheDecision(YesIGetThisBenefit, function)

        whenReady(result) { _ =>
          verify(journeyCacheService).cache(any(), any())(any())
        }
      }
    }
    "return the default redirection" when {
      "we have no benefit type cached" in {
        when(journeyCacheService.mandatoryJourneyValue(any())(any())).thenReturn(Future.successful(Left("")))

        val result =
          wrapper.cacheDecision(YesIGetThisBenefit, (_: String, b: Result) => b)

        whenReady(result) { r =>
          r mustBe Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
          verify(journeyCacheService, times(0)).cache(any(), meq(YesIGetThisBenefit))(any())
        }
      }
    }
  }
}
