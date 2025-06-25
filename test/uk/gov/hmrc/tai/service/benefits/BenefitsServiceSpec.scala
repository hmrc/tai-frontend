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

package uk.gov.hmrc.tai.service.benefits

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.when
import uk.gov.hmrc.tai.connectors.BenefitsConnector
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, EndedCompanyBenefit, GenericBenefit}
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import uk.gov.hmrc.http.NotFoundException

class BenefitsServiceSpec extends BaseSpec {

  "benefits" must {
    "return benefits" in {
      val sut           = createSut
      val sampleTaxYear = 2018
      when(benefitsConnector.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))

      val result = sut.benefits(nino, sampleTaxYear)
      Await.result(result, 5 seconds) mustBe benefits
    }

    "retun empty benefits in NotFoundException is received" in {
      val sut           = createSut
      val sampleTaxYear = 2018
      when(benefitsConnector.benefits(any(), any())(any()))
        .thenReturn(Future.failed(new NotFoundException("Not found")))

      val result = sut.benefits(nino, sampleTaxYear)
      Await.result(result, 5 seconds) mustBe Benefits(Seq.empty, Seq.empty)
    }
  }

  "Ended company benefit" must {
    "return an envelope id" in {
      val sut                 = createSut
      val endedCompanyBenefit =
        EndedCompanyBenefit("Accommodation", "Before 6th April", Some("1000000"), "Yes", Some("0123456789"))
      when(benefitsConnector.endedCompanyBenefit(meq(nino), meq(1), meq(endedCompanyBenefit))(any()))
        .thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.endedCompanyBenefit(nino, 1, endedCompanyBenefit), 5.seconds)

      envId mustBe "123-456-789"
    }

    "generate a runtime exception" when {
      "no envelope id was returned from the connector layer" in {
        val sut                 = createSut
        val endedCompanyBenefit =
          EndedCompanyBenefit("Accommodation", "Before 6th April", Some("1000000"), "Yes", Some("0123456789"))
        when(benefitsConnector.endedCompanyBenefit(meq(nino), meq(1), meq(endedCompanyBenefit))(any()))
          .thenReturn(Future.successful(None))

        val rte = the[RuntimeException] thrownBy Await
          .result(sut.endedCompanyBenefit(nino, 1, endedCompanyBenefit), 5.seconds)
        rte.getMessage mustBe s"No envelope id was generated when attempting to end company benefit for ${nino.nino}"
      }
    }
  }

  private val benefits = Benefits(Seq(CompanyCarBenefit(1, BigDecimal(1), Seq.empty)), Seq.empty[GenericBenefit])

  private def createSut = new SUT

  val benefitsConnector = mock[BenefitsConnector]

  private class SUT
      extends BenefitsService(
        benefitsConnector
      )

}
