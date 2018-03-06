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

package controllers.benefits

import builders.{AuthBuilder, RequestBuilder}
import controllers.FakeTaiPlayApplication
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.TaiRoot
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService, JourneyCacheService, TaiService}
import uk.gov.hmrc.tai.util._

import scala.concurrent.Future
import scala.util.Random

class CompanyBenefitControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with FormValuesConstants
  with JourneyCacheConstants
  with DateFormatConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "redirectCompanyBenefitSelection" must {
    "redirect to decision page" in {

      val empId = 1
      val benefitType = "testBenefitType"

      val SUT = createSUT

      when(SUT.journeyCacheService.cache(Matchers.any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

      val result = SUT.redirectCompanyBenefitSelection(empId, benefitType)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.CompanyBenefitController.decision().url
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  def generateNino: Nino = new Generator(new Random).nextNino

  private def createSUT = new SUT

  private val employment = Employment("company name", Some("123"), new LocalDate("2016-05-26"),
    Some(new LocalDate("2016-05-26")), Nil, "", "", 2)

  private class SUT extends CompanyBenefitController {

    override val taiService: TaiService = mock[TaiService]
    override val auditService: AuditService = mock[AuditService]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: PartialRetriever = mock[PartialRetriever]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val trackingJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))

    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(TaiRoot("", 1, "", "", None, "", "", false, None)))
  }
}
