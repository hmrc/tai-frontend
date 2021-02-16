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

package controllers.actions

import controllers.Assets.Redirect
import controllers.auth.{AuthenticatedRequest, DataRequest, OptionalDataRequest}
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.tai.util.CachedData
import utils.BaseSpec

import scala.concurrent.Future

class DataRequiredActionSpec extends BaseSpec {

  class Harness() extends DataRequiredActionImpl {

    def callRefine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

  val id = "id"
  val cachedData = CachedData(CacheMap(id, Map.empty))
  val request = AuthenticatedRequest(FakeRequest(), id, authedUser, "Some One")

  "DataRequiredAction" when {

    "Cached data is set in the request" should {

      "return a DataRequest" in {

        val optDataRequest = OptionalDataRequest(request, id, Some(cachedData))

        val expected = DataRequest(optDataRequest.request, id, cachedData)

        val action = new Harness()
        val futureResult =
          action.callRefine(optDataRequest)
        whenReady(futureResult) { result =>
          result mustBe Right(expected)
        }
      }
    }

    "Cached data is empty in the request" should {

      "redirect to timeout page" in {

        val optDataRequest = OptionalDataRequest(request, id, None)

        val action = new Harness()
        val futureResult =
          action.callRefine(optDataRequest)
        whenReady(futureResult) { result =>
          result mustBe Left(Redirect(controllers.routes.ServiceController.timeoutPage()))
        }
      }
    }
  }
}
