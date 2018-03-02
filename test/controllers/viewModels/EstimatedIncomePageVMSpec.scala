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

import controllers.{FakeTaiPlayApplication, routes}
import uk.gov.hmrc.tai.viewModels.EstimatedIncomeViewModel
import org.mockito.Matchers.any
import org.mockito.Mockito.verify
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.urls.Link
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.model.PersonalTaxSummaryContainer

class EstimatedIncomePageVMSpec
extends UnitSpec
with FakeTaiPlayApplication {
  "Calling the EstimatedIncomePageVMSpec method" should {
    "call personal-tax-summary with the correct parameters" in new Success {
      val result: EstimatedIncomeViewModel = await(EstimatedIncomePageVMTest.createObject(nino, container.details))

      result shouldBe estimatedIncomeViewModel

      verify(mockDomainConnector).buildEstimatedIncomeView(ninoCaptor.capture(), personalTaxSummaryContainerCaptor.capture())(any())
      val actualNino: Nino = ninoCaptor.getValue
      val actualContainer: PersonalTaxSummaryContainer = personalTaxSummaryContainerCaptor.getValue

      actualNino shouldBe nino
      actualContainer.details shouldBe taxSummary
      actualContainer.links.keySet.size shouldBe 4
      actualContainer.links.contains("marriageAllowance") shouldBe true
      actualContainer.links.contains("maintenancePayments") shouldBe true
      actualContainer.links.contains("underpaymentEstimatePageUrl") shouldBe true
      actualContainer.links.contains("taxExplanationScreen") shouldBe true
      actualContainer.links.getOrElse("taxExplanationScreen", "") shouldBe Link.toInternalPage(
        url = routes.TaxExplanationController.taxExplanationPage().toString,
        value = Some(Messages("tai.mergedTaxBand.description")),
        id = Some("taxExplanation")
      ).toHtml.body
    }
  }

  "removeDecimals" should {
    val num1 = BigDecimal(1.0)
    val num2 = BigDecimal(1.1)
    val num3 = BigDecimal(1.4582)
    val num4 = BigDecimal(1)
    val num5 = BigDecimal(0.0)
    val num6 = BigDecimal(-1.0)
    val num7 = BigDecimal(-1.3)
    val num8 = BigDecimal(0)
    val num9 = BigDecimal(1.08530)

    "filter list and return a list with no unnecessary decimals" in {
      val output1 = EstimatedIncomePageVM.removeDecimalsToString(num1)
      val output2 = EstimatedIncomePageVM.removeDecimalsToString(num2)
      val output3 = EstimatedIncomePageVM.removeDecimalsToString(num3)
      val output4 = EstimatedIncomePageVM.removeDecimalsToString(num4)
      val output5 = EstimatedIncomePageVM.removeDecimalsToString(num5)
      val output6 = EstimatedIncomePageVM.removeDecimalsToString(num6)
      val output7 = EstimatedIncomePageVM.removeDecimalsToString(num7)
      val output8 = EstimatedIncomePageVM.removeDecimalsToString(num8)
      val output9 = EstimatedIncomePageVM.removeDecimalsToString(num9)

      output1 shouldBe "1"
      output2 shouldBe "1.1"
      output3 shouldBe "1.4582"
      output4 shouldBe "1"
      output5 shouldBe "0"
      output6 shouldBe "-1"
      output7 shouldBe "-1.3"
      output8 shouldBe "0"
      output9 shouldBe "1.0853"
    }
  }
}
