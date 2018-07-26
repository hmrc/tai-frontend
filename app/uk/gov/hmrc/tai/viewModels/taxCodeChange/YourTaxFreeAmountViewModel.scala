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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.{TaxAccountCalculator, ViewModelHelper}
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountViewModel.withPoundPrefixAndSign
import uk.gov.hmrc.time.TaxYearResolver

/**
  * Created by digital032748 on 25/07/18.
  */
case class YourTaxFreeAmountViewModel(taxCodeDateRange: String, annualTaxFreeAmount:String){}

object YourTaxFreeAmountViewModel extends ViewModelHelper with TaxAccountCalculator {

  def apply(p2IssuedDate: LocalDate, codingComponents: Seq[CodingComponent])(implicit messages: Messages): YourTaxFreeAmountViewModel = {

    val taxCodeDateRange = dynamicDateRangeHtmlNonBreak(p2IssuedDate, TaxYearResolver.endOfCurrentTaxYear)
    val annualTaxFreeAmount = withPoundPrefixAndSign(MoneyPounds(taxFreeAmount(codingComponents), 0))

    YourTaxFreeAmountViewModel(taxCodeDateRange, annualTaxFreeAmount)
  }
}


