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
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{OtherBasisOperation, Week1Month1BasisOperation}
import uk.gov.hmrc.tai.viewModels.DescriptionListViewModel
import uk.gov.hmrc.time.TaxYearResolver

import scala.collection.immutable.ListMap

/**
  * Created by digital032748 on 25/07/18.
  */
class TaxCodeChangeViewModelSpec extends PlaySpec with FakeTaiPlayApplication {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val previousTaxCodeRecord1 = TaxCodeRecord("1185L", startDate, startDate.plusMonths(1), OtherBasisOperation,"A Employer 1", false, Some("1234"), false)
  val currentTaxCodeRecord1 = previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)
  val fullYearTaxCode = TaxCodeRecord("1185L", startDate, TaxYearResolver.endOfCurrentTaxYear, Week1Month1BasisOperation, "B Employer 1", false, Some("12345"), false)
  val primaryFullYearTaxCode = fullYearTaxCode.copy(employerName = "C", pensionIndicator = false, primary = true)

  val taxCodeChange = TaxCodeChange(
    Seq(previousTaxCodeRecord1, primaryFullYearTaxCode),
    Seq(currentTaxCodeRecord1, primaryFullYearTaxCode)
  )

  "TaxCodeChangeViewModel apply method" must {
    "translate the taxCodeChange object into a TaxCodePairs" in {
      val model = TaxCodeChangeViewModel(taxCodeChange, Map[String, BigDecimal]())

      model.pairs mustEqual TaxCodePairs(Seq(
        TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)),
        TaxCodePair(Some(previousTaxCodeRecord1), Some(currentTaxCodeRecord1))
      ))
    }

    "sets the changeDate to the mostRecentTaxCodeChangeDate" in {
      val model = TaxCodeChangeViewModel(taxCodeChange, Map[String, BigDecimal]())

      model.changeDate mustEqual currentTaxCodeRecord1.startDate
    }
  }

  "TaxCodeChangeViewModel getTaxCodeExplanations" must {
    "get the appropriate explanation and heading for a tax" when {
      "basisOfOperation is standard" in {
        val expected = DescriptionListViewModel(
          Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", previousTaxCodeRecord1.taxCode),
          ListMap(
            ("1185", Messages("tai.taxCode.amount", "11,850")),
            ("L", Messages("tai.taxCode.L"))
          )
        )

        val result = TaxCodeChangeViewModel.getTaxCodeExplanations(previousTaxCodeRecord1, Map[String, BigDecimal](), "current")

        result mustEqual(expected)
      }

      "basisOfOperation is emergency" in {
        val expected = DescriptionListViewModel(
          Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", fullYearTaxCode.taxCode + "X"),
          ListMap(
            ("1185", Messages("tai.taxCode.amount", "11,850")),
            ("L", Messages("tai.taxCode.L")),
            ("X", Messages("tai.taxCode.X"))
          )
        )

        val result = TaxCodeChangeViewModel.getTaxCodeExplanations(fullYearTaxCode, Map[String, BigDecimal](), "current")

        result mustEqual(expected)
      }

      "Using a scottish tax rate band" in {
        val taxCode = "D2"
        val scottishTaxCode = TaxCodeRecord(taxCode, startDate, startDate.plusMonths(1), OtherBasisOperation, "B Employer 1", false, Some("12345"), false)
        val scottishTaxRateBands = Map(taxCode -> BigDecimal(21.5))

        val expected = DescriptionListViewModel(
          Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", scottishTaxCode.taxCode),
          ListMap(
            (taxCode, Messages("tai.taxCode.DX", "21.5"))
          )
        )

        val result = TaxCodeChangeViewModel.getTaxCodeExplanations(scottishTaxCode, scottishTaxRateBands, "current")

        result mustEqual(expected)
      }
    }
  }
}
