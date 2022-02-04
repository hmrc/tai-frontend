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

package controllers

import controllers.actions.FakeValidatePerson
import controllers.auth.AuthAction
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.util.constants.TaiConstants
import utils.BaseSpec
import views.html.{ManualCorrespondenceView, SessionExpiredView, TimeoutView}

class ServiceControllerSpec extends BaseSpec {

  "Time Out page" should {
    "return page when called" in {
      val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody()
      val sut = createSut()
      val result = sut.timeoutPage()(fakeRequest)
      status(result) mustBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.timeout.title"))
    }
  }

  "Sign Out" must {
    "redirect to company auth frontend if it is a GG user" in {
      val sut = createSut()

      val result = sut.serviceSignout()(fakeRequest)

      status(result) mustBe 303
      redirectLocation(result) mustBe Some(appConfig.basGatewayFrontendSignOutUrl)
    }

    "redirect to citizen auth frontend if it is a Verify user" in {
      val sut = createSut(FakeAuthActionVerify)

      val result = sut.serviceSignout()(fakeRequest)

      status(result) mustBe 303
      redirectLocation(result) mustBe Some(appConfig.citizenAuthFrontendSignOutUrl)
      session(result).get(TaiConstants.SessionPostLogoutPage) mustBe Some(appConfig.feedbackSurveyUrl)
    }
  }

  "keepAlive" should {

    "return 200" in {

      val sut = createSut()

      val result = sut.keepAlive()(FakeRequest("GET", ""))

      status(result) mustBe OK
    }
  }

  "sessionExpiredPage" should {
    "clear the session" in {

      val sut = createSut()

      val result = sut.keepAlive()(FakeRequest("GET", "").withSession("test" -> "session"))

      session(result) mustBe empty
    }
  }

  "mciErrorPage" should {
    "return manualCorrespondence page when called" in {
      val fakeRequest = FakeRequest("GET", "").withFormUrlEncodedBody()
      val sut = createSut()
      val result = sut.mciErrorPage()(fakeRequest)
      status(result) mustBe LOCKED
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("mci.title"))
    }
  }

  def createSut(authAction: AuthAction = FakeAuthAction) = new SUT(authAction)

  class SUT(authAction: AuthAction = FakeAuthAction)
      extends ServiceController(
        authAction,
        FakeValidatePerson,
        appConfig,
        mcc,
        inject[TimeoutView],
        inject[SessionExpiredView],
        inject[ManualCorrespondenceView],
        templateRenderer
      )

}
