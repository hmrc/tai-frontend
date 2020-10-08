/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{OtherBasisOfOperation, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.viewModels.DescriptionListViewModel
import utils.BaseSpec

import scala.collection.immutable.ListMap

class TaxCodeChangeViewModelSpec extends BaseSpec {

  val endOfTaxYear = TaxYear().end
  val startDate = TaxYear().start
  val previousTaxCodeRecord1 = TaxCodeRecord(
    "1185L",
    startDate,
    startDate.plusMonths(1),
    OtherBasisOfOperation,
    "A Employer 1",
    false,
    Some("1234"),
    false)
  val currentTaxCodeRecord1 =
    previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = endOfTaxYear)
  val fullYearTaxCode = TaxCodeRecord(
    "1185L",
    startDate,
    endOfTaxYear,
    Week1Month1BasisOfOperation,
    "B Employer 1",
    false,
    Some("12345"),
    false)
  val primaryFullYearTaxCode = fullYearTaxCode.copy(employerName = "C", pensionIndicator = false, primary = true)

  val taxCodeChange = TaxCodeChange(
    Seq(previousTaxCodeRecord1, primaryFullYearTaxCode),
    Seq(currentTaxCodeRecord1, primaryFullYearTaxCode)
  )

  "TaxCodeChangeViewModel apply method" must {
    "translate the taxCodeChange object into a TaxCodePairs" in {
      val model = TaxCodeChangeViewModel(taxCodeChange, Map[String, BigDecimal]())

      val primaryPairs = Seq(TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)))
      val secondaryPairs = Seq(TaxCodePair(Some(previousTaxCodeRecord1), Some(currentTaxCodeRecord1)))

      model.pairs mustEqual TaxCodePairs(
        primaryPairs,
        secondaryPairs,
        Seq.empty,
        Seq.empty
      )
    }

    "sets the changeDate to the mostRecentTaxCodeChangeDate" in {
      val model = TaxCodeChangeViewModel(taxCodeChange, Map[String, BigDecimal]())

      model.changeDate mustEqual currentTaxCodeRecord1.startDate
    }

    "ga custom dimension for 'Multiple Secondary Employments without payroll number'" should {
      val gaCustomDimensionYes = Map("taxCodeChangeEdgeCase" -> "Yes")
      val gaCustomDimensionNo = Map("taxCodeChangeEdgeCase"  -> "No")

      "be No" when {
        "when it does not occur in current or previous" in {
          val startDate = TaxYear().start
          val endDate = startDate.plusMonths(1)
          val changeDate = startDate.plusMonths(1).plusDays(1)

          val previousTaxCodeRecord1 = TaxCodeRecord(
            "1185L",
            startDate,
            endDate,
            OtherBasisOfOperation,
            "A Employer 1",
            false,
            Some("1234"),
            false)
          val currentTaxCodeRecord1 = previousTaxCodeRecord1.copy(startDate = changeDate, endDate = endOfTaxYear)

          val previousTCR2Primary = TaxCodeRecord(
            "1185L",
            startDate,
            endOfTaxYear,
            Week1Month1BasisOfOperation,
            "B Employer 1",
            false,
            Some("12345"),
            true)
          val currentTCR2Primary = previousTCR2Primary.copy(startDate = changeDate, endDate = endOfTaxYear)

          val taxCodeChange = TaxCodeChange(
            Seq(previousTaxCodeRecord1, previousTCR2Primary),
            Seq(currentTaxCodeRecord1, currentTCR2Primary)
          )

          val model = TaxCodeChangeViewModel(taxCodeChange, Map.empty[String, BigDecimal])

          model.gaDimensions mustEqual gaCustomDimensionNo + ("taxCodeChangeDate" -> currentTaxCodeRecord1.startDate
            .toString(TaiConstants.EYU_DATE_FORMAT))

        }
      }

      "be Yes" when {
        "there are multiple for current" in {

          val startDate = TaxYear().start
          val endDate = startDate.plusMonths(1)
          val changeDate = startDate.plusMonths(1).plusDays(1)

          val employerName = "A Employer 1"

          val previousTaxCodeRecord1 =
            TaxCodeRecord("1185L", startDate, endDate, OtherBasisOfOperation, employerName, false, None, false)
          val currentTaxCodeRecord1 =
            previousTaxCodeRecord1.copy(startDate = changeDate, endDate = endOfTaxYear, payrollNumber = Some("1234"))

          val previousTaxCodeRecord2 = TaxCodeRecord(
            "BR",
            startDate,
            startDate.plusMonths(1),
            OtherBasisOfOperation,
            employerName,
            false,
            None,
            false)

          val taxCodeChange = TaxCodeChange(
            Seq(previousTaxCodeRecord1, previousTaxCodeRecord2),
            Seq(currentTaxCodeRecord1)
          )

          val model = TaxCodeChangeViewModel(taxCodeChange, Map.empty[String, BigDecimal])

          model.gaDimensions mustEqual gaCustomDimensionYes + ("taxCodeChangeDate" -> currentTaxCodeRecord1.startDate
            .toString(TaiConstants.EYU_DATE_FORMAT))

        }

        "there are multiple for previous" in {
          val startDate = TaxYear().start
          val endDate = startDate.plusMonths(1)
          val changeDate = startDate.plusMonths(1).plusDays(1)

          val employerName = "A Employer 1"

          val previousTaxCodeRecord1 = TaxCodeRecord(
            "1185L",
            startDate,
            startDate.plusMonths(1),
            OtherBasisOfOperation,
            employerName,
            false,
            None,
            false)
          val currentTaxCodeRecord1 =
            previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = endOfTaxYear)

          val previousTaxCodeRecord2 = TaxCodeRecord(
            "BR",
            startDate,
            startDate.plusMonths(1),
            OtherBasisOfOperation,
            employerName,
            false,
            None,
            false)
          val currentTaxCodeRecord2Primary = previousTaxCodeRecord2
            .copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = endOfTaxYear, primary = true)

          val taxCodeChange = TaxCodeChange(
            Seq(previousTaxCodeRecord1, previousTaxCodeRecord2),
            Seq(currentTaxCodeRecord1, currentTaxCodeRecord2Primary)
          )

          val model = TaxCodeChangeViewModel(taxCodeChange, Map.empty[String, BigDecimal])

          model.gaDimensions mustEqual gaCustomDimensionYes + ("taxCodeChangeDate" -> currentTaxCodeRecord1.startDate
            .toString(TaiConstants.EYU_DATE_FORMAT))
        }

      }
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

        val result =
          TaxCodeChangeViewModel
            .getTaxCodeExplanations(previousTaxCodeRecord1, Map[String, BigDecimal](), "current", appConfig)

        result mustEqual (expected)
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

        val result =
          TaxCodeChangeViewModel
            .getTaxCodeExplanations(fullYearTaxCode, Map[String, BigDecimal](), "current", appConfig)

        result mustEqual (expected)
      }

      "Using a scottish tax rate band" in {
        val taxCode = "D2"
        val scottishTaxCode = TaxCodeRecord(
          taxCode,
          startDate,
          startDate.plusMonths(1),
          OtherBasisOfOperation,
          "B Employer 1",
          false,
          Some("12345"),
          false)
        val scottishTaxRateBands = Map(taxCode -> BigDecimal(21.5))

        val expected = DescriptionListViewModel(
          Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", scottishTaxCode.taxCode),
          ListMap(
            (taxCode, Messages("tai.taxCode.DX", "21.5"))
          )
        )

        val result =
          TaxCodeChangeViewModel.getTaxCodeExplanations(scottishTaxCode, scottishTaxRateBands, "current", appConfig)

        result mustEqual (expected)
      }
    }
  }
}
