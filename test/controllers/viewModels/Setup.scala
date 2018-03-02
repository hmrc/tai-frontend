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

package controllers.viewModels

import builders.UserBuilder
import controllers.auth.TaiUser
import data.TaiData
import org.mockito.{ArgumentCaptor, Matchers, Mockito}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.DomainConnector
import uk.gov.hmrc.tai.model.{PersonalTaxSummaryContainer, TaxSummaryDetails}
import uk.gov.hmrc.tai.viewModels.{BandedGraph, EstimatedIncomeViewModel}

import scala.concurrent.Future

trait Setup extends MockitoSugar {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val user: TaiUser = UserBuilder.apply()

  val mockDomainConnector: DomainConnector = mock[DomainConnector]
  val ninoCaptor: ArgumentCaptor[Nino] = ArgumentCaptor.forClass(classOf[Nino])
  val personalTaxSummaryContainerCaptor: ArgumentCaptor[PersonalTaxSummaryContainer] = ArgumentCaptor.forClass(classOf[PersonalTaxSummaryContainer])

  val nino = new Generator().nextNino
  val taxSummary = TaxSummaryDetails(nino.value,1)
  val container = PersonalTaxSummaryContainer(taxSummary, Map.empty)


  val estimatedIncomeViewModel = EstimatedIncomeViewModel(
    newGraph = BandedGraph("foo"),
    ukDividends = None,
    taxBands = None,
    incomeTaxReducedToZeroMessage = None, taxRegion = "UK")

  object EstimatedIncomePageVMTest extends EstimatedIncomePageVMBuilder {
    override def domainConnector: DomainConnector = mockDomainConnector
  }

}

trait Success extends Setup {
  Mockito.when(mockDomainConnector.buildEstimatedIncomeView(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(estimatedIncomeViewModel))
}

trait ComparisonSuccess extends Setup {
  Mockito.when(mockDomainConnector.buildEstimatedIncomeView(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(TaiData.getEstimatedIncome))
}