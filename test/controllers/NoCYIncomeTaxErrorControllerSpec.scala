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

import builders.{AuthBuilder, RequestBuilder}
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.tai.model.domain.{Employment, Person}
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class NoCYIncomeTaxErrorControllerSpec
  extends PlaySpec
    with FakeTaiPlayApplication
    with ScalaFutures with I18nSupport with MockitoSugar {

  "Calling the Current Year Page method" should {

    "call noCYIncomeTaxErrorPage() successfully with an authorised session " in {
      val sut = createSUT()
      val result = sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      val doc = Jsoup.parse(contentAsString(result))
      status(result) mustBe OK
      doc.title() must include(Messages("tai.noCYIncomeError.title"))
    }

    "call employment service to fetch sequence of employments" in {
      val sut = createSUT()
      Await.result(sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
      verify(sut.employmentService, times(1)).employments(any(), any())(any())
    }

    "display the page" when {
      "employment service throws NotFound exception" in {
        val sut = createSUT(employmentDataFailure = Some(new NotFoundException("no data found")))
        val result = sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        Await.result(result, 5 seconds)
        verify(sut.employmentService, times(1)).employments(any(), any())(any())
        status(result) mustBe OK
      }

      "employment service throws BadRequest exception" in {
        val sut = createSUT(employmentDataFailure = Some(new BadRequestException("bad request")))
        val result = sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        Await.result(result, 5 seconds)
        verify(sut.employmentService, times(1)).employments(any(), any())(any())
        status(result) mustBe OK
      }
    }

    "redirect" when {
      val nino = generateNino

      "user is deceased" in {
        val person = defaultPerson.copy(isDeceased = true)
        val sut = createSUT(person)
        val result = sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe "/check-income-tax/deceased"
      }

      "MCI indicator is true" in {
        val person = defaultPerson.copy(hasCorruptData = true)
        val sut = createSUT(person)
        val result = sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe "/check-income-tax/tax-estimate-unavailable"
      }

      "to deceased page when both deceased and MCI indicators are true" in {
        val person = defaultPerson.copy(hasCorruptData = true, isDeceased = true)
        val sut = createSUT(person)
        val result = sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe "/check-income-tax/deceased"
      }
    }
  }


  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val hc = HeaderCarrier()

  def generateNino: Nino = new Generator(new Random).nextNino

  val defaultPerson = fakePerson(generateNino)

  def createSUT(person: Person = defaultPerson, employmentDataFailure: Option[Throwable] = None) = new SUT(person, employmentDataFailure)

  class SUT(person: Person, employmentDataFailure: Option[Throwable]) extends NoCYIncomeTaxErrorController {
    override val personService = mock[PersonService]
    override implicit val templateRenderer = MockTemplateRenderer
    override implicit val partialRetriever = MockPartialRetriever
    override val employmentService = mock[EmploymentService]

    override val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val authConnector: AuthConnector = mock[AuthConnector]

    val ad = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(person))

    val sampleEmployment = Seq(Employment("empName", None, new LocalDate(2017, 6, 9), None, Nil, "taxNumber", "payeNumber", 1, None, false, false))

    employmentDataFailure match {
      case None => when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(sampleEmployment))
      case Some(throwable) => when(employmentService.employments(any(), any())(any())).thenReturn(Future.failed(throwable))
    }

  }

}
