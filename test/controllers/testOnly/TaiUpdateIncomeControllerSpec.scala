/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.I18nSupport
import play.api.test.Helpers.{redirectLocation, status, _}
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.service.{EmploymentService, IncomeService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import utils.BaseSpec

import scala.concurrent.Future

class TaiUpdateIncomeControllerSpec
    extends BaseSpec with JourneyCacheConstants with I18nSupport with BeforeAndAfterEach {

  val incomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val personService: PersonService = mock[PersonService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService =
    mock[EstimatedPayJourneyCompletionService]

  override def beforeEach: Unit =
    Mockito.reset(incomeService, journeyCacheService)

  val payToDate = "100"
  val employerId = 1
  val employerName = "Employer Name"

  val cachedData = Future.successful(Right(Seq(employerId.toString, payToDate)))
  val cachedUpdateIncomeNewAmountKey: Future[Either[String, String]] = Future.successful(Right("700"))
  val emptyCache = Future.successful(Left("empty cache"))

  val cacheKey = s"$UpdateIncome_ConfirmedNewAmountKey-$employerId"
  private def createTaiUpdateIncomeController() = new TestTaiUpdateIncomeController()

  "TaiUpdateIncomeController" must {
    "delete the journey cache to facilitate the next test run" in {

      val taiUpdateIncomeController = createTaiUpdateIncomeController()
      when(journeyCacheService.delete()(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = taiUpdateIncomeController.delete(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url

      verify(journeyCacheService, times(1)).delete()(any())

    }
  }

  private class TestTaiUpdateIncomeController()
      extends TaiUpdateIncomeController(
        journeyCacheService,
        FakeAuthAction,
        FakeValidatePerson,
        mcc,
        templateRenderer
      ) {
    when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
    when(journeyCacheService.flush()(any())).thenReturn(Future.successful(Done))
  }

}
