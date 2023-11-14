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

package uk.gov.hmrc.tai.service

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{mock, reset, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.{Environment, Mode}
import uk.gov.hmrc.tai.config.ApplicationConfig

import java.io.File

class URLServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with BeforeAndAfterEach {

  private val testEnv: Environment = Environment(new File(""), classOf[URLServiceSpec].getClassLoader, Mode.Test)
  private val devEnv: Environment = Environment(new File(""), classOf[URLServiceSpec].getClassLoader, Mode.Dev)
  private val mockApplicationConfig = mock[ApplicationConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationConfig)
  }

  "localFriendlyUrl" should {
    "return the original url if it is in the test environment" in {
      val service = new URLService(mockApplicationConfig, testEnv)
      service.localFriendlyUrl("A", "B") shouldBe "A"
    }

    "return url string with localhost and port if is in development (local/ jenkins) environment" in {
      val service = new URLService(mockApplicationConfig, devEnv)
      when(mockApplicationConfig.getOptional[String](any())(any())).thenReturn(Option("Dev"))
      service.localFriendlyUrl("A", "B") shouldBe "http://BA"
    }

    "return the original url if is in production environment" in {
      val service = new URLService(mockApplicationConfig, devEnv)
      when(mockApplicationConfig.getOptional[String](any())(any())).thenReturn(Option("Prod"))
      service.localFriendlyUrl("A", "B") shouldBe "A"
    }

    "if url is absolute then return the original url regardless of environment" in {
      val service = new URLService(mockApplicationConfig, devEnv)
      when(mockApplicationConfig.getOptional[String](any())(any())).thenReturn(Option("Prod"))
      service.localFriendlyUrl("http://A", "B") shouldBe "http://A"
    }

  }
}
