/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import controllers.actions.FakeValidatePerson
import controllers.auth.AuthAction
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.constants.TaiConstants

class ServiceControllerSpec extends UnitSpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val hc = HeaderCarrier()

  "Time Out page" should {
    "return page when called" in {
      val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody()
      val sut = createSut()
      val result = sut.timeoutPage()(fakeRequest)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() should include(Messages("tai.timeout.title"))
    }
  }

  "Sign Out" should {
    "redirect to company auth frontend if it is a GG user" in {
      val sut = createSut()

      val result = sut.serviceSignout()(fakeRequest)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(ApplicationConfig.companyAuthFrontendSignOutUrl)
    }

    "redirect to citizen auth frontend if it is a Verify user" in {
      val sut = createSut(FakeAuthActionVerify)

      val result = sut.serviceSignout()(fakeRequest)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(ApplicationConfig.citizenAuthFrontendSignOutUrl)
      session(result).get(TaiConstants.SessionPostLogoutPage) shouldBe Some(ApplicationConfig.feedbackSurveyUrl)
    }
  }

  "gateKeeper" should {
    "return manualCorrespondence page when called" in {
      val fakeRequest = FakeRequest("GET", "").withFormUrlEncodedBody()
      val sut = createSut()
      val result = sut.gateKeeper()(fakeRequest)
      status(result) shouldBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() should include(Messages("tai.gatekeeper.refuse.title"))
    }
  }

  def createSut(authAction: AuthAction = FakeAuthAction) = new SUT(authAction)

  class SUT(authAction: AuthAction = FakeAuthAction) extends ServiceController(
    authAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  )

}

