package uk.gov.hmrc.tai.util

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountViewModel

class YourTaxFreeAmountSpec extends PlaySpec {
  "builds tax free amount view model" in {
    val taxFreeAmount = new TestTaxFreeAmount


  }

  class TaxAccountCalcuatorMock { this: TaxAccountCalculator =>
    override def taxFreeAmount(codingComponents: Seq[CodingComponent])
  }

  class TestTaxFreeAmount extends YourTaxFreeAmount {
    override def dynamicDateRangeHtmlNonBreak(): String {
        "abc"
    }

//    val taxCodeDateRange = TaxYearRangeUtil.dynamicDateRange(recentTaxCodeChangeDate, TaxYearResolver.endOfCurrentTaxYear)
//
//    val taxFreeAmountTotal = taxFreeAmount(codingComponents)
//    val annualTaxFreeAmount = withPoundPrefixAndSign(MoneyPounds (taxFreeAmountTotal, 0) )
//    val taxFreeAmountSummary = TaxFreeAmountSummaryViewModel(codingComponents, employmentNames, companyCarBenefits, taxFreeAmountTotal)
//
//    YourTaxFreeAmountViewModel(
//      taxFreeAmountTotal,
//      taxCodeDateRange,
//      annualTaxFreeAmount,
//      taxFreeAmountSummary
    }
  }

}
