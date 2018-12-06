package uk.gov.hmrc.tai.util

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.viewModels.{TaxFreeAmountSummaryViewModel, TaxFreeAmountViewModel}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.time.TaxYearResolver


trait YourTaxFreeAmount extends ViewModelHelper with TaxAccountCalculator {
  def buildTaxFreeAmount(recentTaxCodeChangeDate: LocalDate,
                         codingComponents: Seq[CodingComponent],
                         employmentNames: Map[Int, String],
                         companyCarBenefits: Seq[CompanyCarBenefit])
                        (implicit messages: Messages): YourTaxFreeAmountViewModel = {
    val taxCodeDateRange = TaxYearRangeUtil.dynamicDateRange(recentTaxCodeChangeDate, TaxYearResolver.endOfCurrentTaxYear)

    val taxFreeAmountTotal = taxFreeAmount(codingComponents)
    val annualTaxFreeAmount = withPoundPrefixAndSign(MoneyPounds (taxFreeAmountTotal, 0) )
    val taxFreeAmountSummary = TaxFreeAmountSummaryViewModel(codingComponents, employmentNames, companyCarBenefits, taxFreeAmountTotal)

    YourTaxFreeAmountViewModel(
      taxFreeAmountTotal,
      taxCodeDateRange,
      annualTaxFreeAmount,
      taxFreeAmountSummary
    )
  }
}
