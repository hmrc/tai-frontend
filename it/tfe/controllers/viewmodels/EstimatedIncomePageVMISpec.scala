package tfe.controllers.viewmodels

import controllers.viewModels.EstimatedIncomePageVM
import uk.gov.hmrc.tai.viewModels.EstimatedIncomeViewModel
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import tfe.TaiFrontendBaseISpec
import tfe.data.TaiData
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.TaxSummaryDetails

class EstimatedIncomePageVMISpec extends TaiFrontendBaseISpec("EstimatedIncomePageISpec")  with ScalaFutures with BeforeAndAfterEach {
  "EstimatedIncomePageVM.createObject" should {

    "succeed" in new Setup {
      val nino = new Generator().nextNino
      val details: TaxSummaryDetails = TaiData.nonCodedTaxSummary
      val result: EstimatedIncomeViewModel = await(EstimatedIncomePageVM.createObject(nino, details))

      result.incomeTaxEstimate shouldBe 14514.6
      result.incomeEstimate shouldBe 62219
      result.taxFreeEstimate shouldBe 10000
    }
  }
}
