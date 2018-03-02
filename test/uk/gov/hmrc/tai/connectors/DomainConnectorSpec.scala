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

package uk.gov.hmrc.tai.connectors

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.tai.config.WSHttp
import uk.gov.hmrc.tai.model.PersonalTaxSummaryContainer
import uk.gov.hmrc.tai.viewModels.EstimatedIncomeViewModel

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class DomainConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with ServicesConfig {

  "DomainConnector" should {

    implicit val hc = HeaderCarrier()

    "have personal-tax-summary base url as serviceUrl" in {
      DomainConnector.serviceUrl mustBe baseUrl("personal-tax-summary")
    }

    "have WSHttp as http" in {
      DomainConnector.http mustBe WSHttp
    }

    "return a Future[EstimatedIncomeViewModel]" in {
      when(SUT.http.POST[PersonalTaxSummaryContainer, EstimatedIncomeViewModel](any(),any(),any())(any(),any(),any(), any()))
        .thenReturn(Future.successful[EstimatedIncomeViewModel](estimatedIncomeViewModel))

      Await.result(SUT.buildEstimatedIncomeView(nino, container), 5 seconds) mustBe estimatedIncomeViewModel
    }


  }

  private object SUT extends DomainConnector with ServicesConfig {
    override lazy val serviceUrl = baseUrl("personal-tax-summary")
    override def http = mockHttp
  }

  private val nino: Nino = new Generator().nextNino
  private lazy val mockHttp = mock[WSHttp]
  private val estimatedIncomeViewModel = mock[EstimatedIncomeViewModel]
  private val container: PersonalTaxSummaryContainer = mock[PersonalTaxSummaryContainer]
}
