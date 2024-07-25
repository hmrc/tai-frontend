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

import controllers.auth.{AuthedUser, DataRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.ScalaOngoingStubbing
import pages.{BenefitDecisionPage, BenefitsDecisionTesterPage, EndCompanyBenefitsTypeTesterPage}
import pages.benefits.EndCompanyBenefitsTypePage
import play.api.http.Status.{NOT_FOUND, OK, SEE_OTHER}
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.Telephone
import uk.gov.hmrc.tai.util.constants.UpdateOrRemoveCompanyBenefitDecisionConstants.YesIGetThisBenefit
import utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class DecisionCacheWrapperSpec extends BaseSpec with Results {

  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  val sessionId = "testSessionId"

  def randomNino(): Nino = new Generator(new Random()).nextNino

  val wrapper: DecisionCacheWrapper = new DecisionCacheWrapper(
    mockAuthJourney,
    mockJourneyCacheNewRepository,
    ec
  ) {
    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  private def setup(ua: UserAnswers): ScalaOngoingStubbing[ActionBuilder[DataRequest, AnyContent]] =
    when(mockAuthJourney.authWithDataRetrieval) thenReturn new ActionBuilder[DataRequest, AnyContent] {
      override def invokeBlock[A](
        request: Request[A],
        block: DataRequest[A] => Future[Result]
      ): Future[Result] =
        block(
          DataRequest(
            request,
            taiUser = AuthedUser(
              Nino(nino.toString()),
              Some("saUtr"),
              None
            ),
            fullName = "",
            userAnswers = ua
          )
        )

      override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = ec
    }

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheNewRepository)
  }

  "getDecision" must {
    "return a None" when {

      "there is no cached BenefitType" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(BenefitDecisionPage, YesIGetThisBenefit)
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = wrapper.getDecision(FakeRequest())

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe "Benefit type not found"
      }

      "there there is no cached Decision and can't form a compound key using benefit type" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsTypeTesterPage, Some(123))
          .setOrException(BenefitsDecisionTesterPage, None)
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = wrapper.getDecision(FakeRequest())

        status(result) mustBe NOT_FOUND
      }

      "return the cached decision" when {
        "there is a cached value for the key given" in {
          reset(mockJourneyCacheNewRepository)

          val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
            .setOrException(BenefitDecisionPage, YesIGetThisBenefit)
            .setOrException(EndCompanyBenefitsTypePage, Telephone.name)
          setup(mockUserAnswers)

          when(mockJourneyCacheNewRepository.get(any(), any()))
            .thenReturn(Future.successful(Some(mockUserAnswers)))

          when(mockJourneyCacheNewRepository.set(any())) thenReturn Future.successful(true)

          val result = wrapper.getDecision(FakeRequest())

          status(result) mustBe OK
        }
      }
    }

    "cacheDecision" must {
      "cache the result and return the function" when {
        "we are able to generate a benefitDecisionKey" in {
          reset(mockJourneyCacheNewRepository)

          val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
            .setOrException(BenefitDecisionPage, YesIGetThisBenefit)
            .setOrException(EndCompanyBenefitsTypePage, Telephone.name)
          setup(mockUserAnswers)

          when(mockJourneyCacheNewRepository.get(any(), any()))
            .thenReturn(Future.successful(Some(mockUserAnswers)))

          when(mockJourneyCacheNewRepository.set(any())) thenReturn Future.successful(true)

          val function = (_: String, b: Result) => b
          val result = wrapper.cacheDecision(YesIGetThisBenefit, function)(FakeRequest())

          status(result) mustBe SEE_OTHER

          redirectLocation(
            result
          ).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
        }
      }
    }
    "return the default redirection" when {
      "we have no benefit type cached" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(BenefitDecisionPage, YesIGetThisBenefit)
          .setOrException(EndCompanyBenefitsTypeTesterPage, None)
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockJourneyCacheNewRepository.set(any())) thenReturn Future.successful(true)

        val result =
          wrapper.cacheDecision(YesIGetThisBenefit, (_: String, b: Result) => b)(FakeRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(
          result
        ).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }
  }
}
