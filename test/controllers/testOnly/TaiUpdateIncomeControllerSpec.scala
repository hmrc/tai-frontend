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

package controllers.testOnly

import builders.RequestBuilder
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import play.api.http.Status.SEE_OTHER
import play.api.i18n.I18nSupport
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.model.UserAnswers
import utils.BaseSpec

import scala.concurrent.Future

class TaiUpdateIncomeControllerSpec extends BaseSpec with I18nSupport {

  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockJourneyCacheNewRepository)
  }

  val employerId = 14

  private def sut = new TaiUpdateIncomeController(
    mockAuthJourney,
    mcc,
    mockJourneyCacheNewRepository
  )

  "TaiUpdateIncomeController" must {

    "delete the journey cache to facilitate the next test run" in {

      val mockUserAnswers: UserAnswers = UserAnswers("testSessionId", nino.nino)

      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.clear(any(), any())).thenReturn(Future.successful(true))

      val result = sut.delete(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      result.futureValue
      status(result) mustBe SEE_OTHER
      verify(mockJourneyCacheNewRepository, times(1)).clear(any(), any())
    }
  }

}
