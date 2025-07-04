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

package controllers

import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.{Args, Status, Suite, TestSuite}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.tai.model.domain.{Address, Person}

import scala.concurrent.ExecutionContext

trait FakeTaiPlayApplication extends GuiceOneServerPerSuite with PatienceConfiguration with TestSuite {
  this: Suite =>

  implicit lazy val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  val additionalConfiguration: Map[String, Any] = Map[String, Any](
    "microservice.services.contact-frontend.port"         -> "6666",
    "microservice.services.pertax-frontend.port"          -> "1111",
    "microservice.services.personal-tax-summary.port"     -> "2222",
    "microservice.services.activity-logger.port"          -> "5555",
    "tai.cy3.enabled"                                     -> true,
    "microservice.services.feedback-survey-frontend.port" -> "3333",
    "microservice.services.company-auth.port"             -> "4444"
  )

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .overrides(
      bind[FeatureFlagService].toInstance(mockFeatureFlagService)
    )
    .build()

  org.slf4j.LoggerFactory
    .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[ch.qos.logback.classic.Logger]
    .setLevel(ch.qos.logback.classic.Level.WARN)

  val employerName = "employer name"

  val address: Address        = Address("line1", "line2", "line3", "postcode", "country")
  val partialAddress: Address = Address(Some("line1"), None, None, Some("postcode"), Some("country"))
  val emptyAddress: Address   = Address(None, None, None, None, None)

  def fakePerson(nino: Nino): Person =
    Person(nino, "Firstname", "Surname", isDeceased = false, address)

  def fakePersonWithNoAddress(nino: Nino): Person =
    Person(nino, "Firstname", "Surname", isDeceased = false, emptyAddress)

  def fakePersonWithPartialAddress(nino: Nino): Person =
    Person(nino, "Firstname", "Surname", isDeceased = false, partialAddress)
  val fakeRequest: FakeRequest[AnyContent]             = FakeRequest("GET", "/")

  abstract override def run(testName: Option[String], args: Args): Status =
    super[GuiceOneServerPerSuite].run(testName, args)

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
}
