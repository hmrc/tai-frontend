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

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.TaiConstants.encodedMinusSign
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.time.TaxYearResolver

import scala.util.Random

/**
  * Created by digital032748 on 25/07/18.
  */
class TaxCodeChangeViewModelSpec extends PlaySpec {

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val previousTaxCodeRecord1 = TaxCodeRecord("code", startDate, startDate.plusMonths(1),"A Employer 1", false, "1234", false)
  val currentTaxCodeRecord1 = previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)
  val fullYearTaxCode = TaxCodeRecord("code", startDate, TaxYearResolver.endOfCurrentTaxYear, "B Employer 1", false, "12345", false)
  val primaryFullYearTaxCode = fullYearTaxCode.copy(employerName = "C", pensionIndicator = false, primary = true)

  val taxCodeChange = TaxCodeChange(
    Seq(previousTaxCodeRecord1, primaryFullYearTaxCode),
    Seq(currentTaxCodeRecord1, primaryFullYearTaxCode)
  )


  "TaxCodeChangeViewModel" must {
    "translate the taxCodeChange object into a TaxCodePairs" in {
      val model = TaxCodeChangeViewModel(taxCodeChange)
      val taxCodePairs = TaxCodePairs

      model.pairs mustEqual TaxCodePairs(Seq(
        TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)),
        TaxCodePair(Some(previousTaxCodeRecord1), Some(currentTaxCodeRecord1))
      ))
    }

    "sets the changeDate to the mostRecentTaxCodeChangeDate" in {
      val model = TaxCodeChangeViewModel(taxCodeChange)

      model.changeDate mustEqual currentTaxCodeRecord1.startDate
    }
  }
}
