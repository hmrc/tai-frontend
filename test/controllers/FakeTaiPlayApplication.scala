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

import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.{Args, Status, Suite, TestSuite}
import org.scalatestplus.play.OneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.tai.model.domain.Person

trait FakeTaiPlayApplication extends OneServerPerSuite with PatienceConfiguration with TestSuite {
  this: Suite =>
  override lazy val port = 12345

    val additionalConfiguration = Map [String, Any] (
                                          "govuk-tax.Test.services.contact-frontend.host" -> "localhost",
                                          "govuk-tax.Test.services.contact-frontend.port" -> "12345",
                                          "govuk-tax.Test.services.pertax-frontend.host" -> "localhost",
                                          "govuk-tax.Test.services.pertax-frontend.port" -> "1111",
                                          "govuk-tax.Test.services.personal-tax-summary.host" -> "localhost",
                                          "govuk-tax.Test.services.personal-tax-summary.port" -> "2222",
                                          "govuk-tax.Test.services.activity-logger.host" -> "localhost",
                                          "govuk-tax.Test.services.activity-logger.port" -> "12345",
                                          "tai.cy3.enabled" -> true,
                                          "govuk-tax.Test.services.feedback-survey-frontend.host" -> "localhost",
                                          "govuk-tax.Test.services.feedback-survey-frontend.port" -> "3333",
                                          "govuk-tax.Test.services.company-auth.host" -> "localhost",
                                          "govuk-tax.Test.services.company-auth.port" -> "4444",
                                          "govuk-tax.Test.services.citizen-auth.host" -> "localhost",
                                          "govuk-tax.Test.services.citizen-auth.port" -> "9999"
                                          )

    implicit override lazy val app: Application = new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[ch.qos.logback.classic.Logger]
    .setLevel(ch.qos.logback.classic.Level.WARN)

  def fakePerson(nino:Nino) = Person(nino, "firstname", "surname", false, false)
  val fakeRequest = FakeRequest("GET", "/")

  abstract override def run(testName: Option[String], args: Args): Status = super[OneServerPerSuite].run(testName, args)
}
