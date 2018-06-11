/*
 * Copyright 2018 HM Revenue & Customs
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
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.service.{AuditService, PersonService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PreviousYearUnderpaymentControllerSpec extends PlaySpec
  with OneAppPerSuite
  with MockitoSugar
  with FakeTaiPlayApplication {

  override lazy val app = new GuiceApplicationBuilder().build()
  def injector = app.injector

  val nino = new Generator().nextNino

  "PreviousYearUnderpaymentController" should {
    "respond with OK" when {
      "underpaymentExplanation is called" in {
        val controller = new SUT()
        when(controller.personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

        val res = controller.underpaymentExplanation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(res) mustBe OK
      }
    }

//    "respond with UNAUTHORIZED" when {
//      "should kick user out if not authorized" in {
//        val sut = new SUT()
     //   status(sut.underpaymentExplanation) mustEqual UNAUTHORIZED
//      }
//    }
  }

  private class SUT() extends PreviousYearUnderpaymentController {
    override val personService: PersonService = mock[PersonService]
    override val auditService: AuditService = mock[AuditService]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val authConnector: AuthConnector = mock[AuthConnector]
    override val delegationConnector: DelegationConnector = mock[DelegationConnector]

    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever

    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData(nino))
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
  }
}
