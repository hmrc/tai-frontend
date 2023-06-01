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

package controllers.testOnly

import akka.Done
import builders.RequestBuilder
import controllers.FakeAuthAction
import controllers.actions.FakeValidatePerson
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.SEE_OTHER
import play.api.i18n.I18nSupport
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.journeyCache._
import utils.BaseSpec

import scala.concurrent.Future

class TaiUpdateIncomeControllerSpec extends BaseSpec with I18nSupport with BeforeAndAfterEach with ScalaFutures {

  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  override def beforeEach(): Unit =
    Mockito.reset(journeyCacheService)

  val employerId = 14
  val employerName = "Employer Name"
  val cacheKey = s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$employerId"

  private def sut = new TaiUpdateIncomeController(
    journeyCacheService,
    FakeAuthAction,
    FakeValidatePerson,
    mcc
  )

  "TaiUpdateIncomeController" must {

    "delete the journey cache to facilitate the next test run" in {

      when(journeyCacheService.delete()(any())).thenReturn(Future.successful(TaiSuccessResponse))
      when(journeyCacheService.flush()(any())).thenReturn(Future.successful(Done))

      val result = sut.delete(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      result.futureValue
      status(result) mustBe SEE_OTHER
      verify(journeyCacheService, times(1)).delete()(any())

    }
  }

}
