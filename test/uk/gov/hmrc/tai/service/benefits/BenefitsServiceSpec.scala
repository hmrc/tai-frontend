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

package uk.gov.hmrc.tai.service.benefits

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.BenefitsConnector
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, EndedCompanyBenefit, GenericBenefit}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class BenefitsServiceSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "benefits" must {
    "return benefits" in {
      val sut = createSut
      val sampleTaxYear = 2018
      when(benefitsConnector.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))

      val result = sut.benefits(generateNino, sampleTaxYear)
      Await.result(result, 5 seconds) mustBe benefits
    }
  }

  "Ended company benefit" must {
    "return an envelope id" in {
      val sut = createSut
      val nino = generateNino
      val endedCompanyBenefit =
        EndedCompanyBenefit("Accommodation", "Before 6th April", Some("1000000"), "Yes", Some("0123456789"))
      when(
        benefitsConnector.endedCompanyBenefit(Matchers.eq(nino), Matchers.eq(1), Matchers.eq(endedCompanyBenefit))(
          any())).thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.endedCompanyBenefit(nino, 1, endedCompanyBenefit), 5.seconds)

      envId mustBe "123-456-789"
    }

    "generate a runtime exception" when {
      "no envelope id was returned from the connector layer" in {
        val sut = createSut
        val nino = generateNino
        val endedCompanyBenefit =
          EndedCompanyBenefit("Accommodation", "Before 6th April", Some("1000000"), "Yes", Some("0123456789"))
        when(
          benefitsConnector.endedCompanyBenefit(Matchers.eq(nino), Matchers.eq(1), Matchers.eq(endedCompanyBenefit))(
            any())).thenReturn(Future.successful(None))

        val rte = the[RuntimeException] thrownBy Await
          .result(sut.endedCompanyBenefit(nino, 1, endedCompanyBenefit), 5.seconds)
        rte.getMessage mustBe s"No envelope id was generated when attempting to end company benefit for ${nino.nino}"
      }
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val benefits = Benefits(Seq.empty[CompanyCarBenefit], Seq.empty[GenericBenefit])
  private def generateNino: Nino = new Generator(new Random).nextNino

  private def createSut = new SUT

  val benefitsConnector = mock[BenefitsConnector]

  private class SUT
      extends BenefitsService(
        benefitsConnector
      )

}
