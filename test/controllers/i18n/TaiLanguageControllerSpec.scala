/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.i18n

import builders.RequestBuilder
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.tai.config.ApplicationConfig
import utils.BaseSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class TaiLanguageControllerSpec extends BaseSpec {

  def sut = new TaiLanguageController(
    appConfig,
    langUtils,
    stubControllerComponents(),
    partialRetriever,
    templateRenderer
  )

  "switchToLanguage" must {

    "default to the correct fallbackUrl" when {

      "there is no referrer set in the request header" in {

        val result = sut.english()(FakeRequest())

        redirectLocation(result) mustBe Some(controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage.url)
      }
    }
  }
}
