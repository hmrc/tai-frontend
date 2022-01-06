/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels

import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.filters.TaxAccountFilter
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.{Live, NotLive, TaxCodeIncome, TaxCodeIncomeSourceStatus}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants

case class IncomeSourceComparisonViewModel(
  employmentIncomeSourceDetail: Seq[IncomeSourceComparisonDetail],
  pensionIncomeSourceDetail: Seq[IncomeSourceComparisonDetail])
    extends ViewModelHelper

object IncomeSourceComparisonViewModel extends ViewModelHelper with TaxAccountFilter {

  def apply(
    taxCodeIncomesCY: Seq[TaxCodeIncome],
    employmentsCY: Seq[Employment],
    taxCodeIncomesCYPlusOne: Seq[TaxCodeIncome]): IncomeSourceComparisonViewModel = {

    val employmentTaxCodeIncomes = taxCodeIncomesCY filter isEmployment
    val employmentIncomeSourceDetailCY =
      incomeSourceDetailCy(employmentTaxCodeIncomes, employmentsCY)

    val employmentTaxCodeIncomesCYPlusOne = taxCodeIncomesCYPlusOne filter isEmployment
    val employmentIncomeSourceDetailCYPlusOne =
      incomeSourceDetailCyPlusOne(employmentTaxCodeIncomesCYPlusOne)

    val pensionTaxCodeIncomes = taxCodeIncomesCY filter isPension
    val pensionIncomeSourceDetailCY =
      incomeSourceDetailCy(pensionTaxCodeIncomes, employmentsCY)

    val pensionTaxCodeIncomesCYPlusOne = taxCodeIncomesCYPlusOne filter isPension
    val pensionIncomeSourceDetailCYPlusOne =
      incomeSourceDetailCyPlusOne(pensionTaxCodeIncomesCYPlusOne)

    val employmentIncomeSourceComparisonDetailSeq = incomeSourceComparisonDetail(
      employmentIncomeSourceDetailCY,
      employmentIncomeSourceDetailCYPlusOne).sortBy(_.amountCY).reverse

    val pensionIncomeSourceComparisonDetailSeq = incomeSourceComparisonDetail(
      pensionIncomeSourceDetailCY,
      pensionIncomeSourceDetailCYPlusOne).sortBy(_.amountCY).reverse

    IncomeSourceComparisonViewModel(employmentIncomeSourceComparisonDetailSeq, pensionIncomeSourceComparisonDetailSeq)
  }

  private def isLiveEmployment(id: Int, employments: Seq[Employment]): Boolean =
    employments.find(_.sequenceNumber == id) match {
      case Some(emp) => emp.employmentStatus == Live
      case _         => false
    }

  private def incomeSourceDetailCyPlusOne(taxCodeIncomes: Seq[TaxCodeIncome]): Seq[IncomeSourceDetail] =
    taxCodeIncomes.flatMap { taxCodeIncome =>
      val amount = withPoundPrefixAndSign(MoneyPounds(taxCodeIncome.amount, 0))
      taxCodeIncome.employmentId.map { id =>
        IncomeSourceDetail(taxCodeIncome.name, id, amount, TaiConstants.CurrentTaxYearPlusOne, false)
      }
    }

  private def incomeSourceDetailCy(
    taxCodeIncomes: Seq[TaxCodeIncome],
    employments: Seq[Employment]): Seq[IncomeSourceDetail] =
    taxCodeIncomes.flatMap { taxCodeIncome =>
      taxCodeIncome.employmentId.flatMap { id =>
        employments.find(_.sequenceNumber == id).map { employment =>
          val amount = withPoundPrefixAndSign(MoneyPounds(taxCodeIncome.amount, 0))

          IncomeSourceDetail(
            employment.name,
            employment.sequenceNumber,
            amount,
            TaiConstants.CurrentTaxYear,
            isLiveEmployment(id, employments))
        }
      }
    }

  private def incomeSourceComparisonDetail(
    incomeSourceDetailCY: Seq[IncomeSourceDetail],
    incomeSourceDetailCYPlusOne: Seq[IncomeSourceDetail]): Seq[IncomeSourceComparisonDetail] = {

    val incomeSourcesCombined = incomeSourceDetailCY ++ incomeSourceDetailCYPlusOne

    incomeSourcesCombined.groupBy(_.empId).toSeq.map { (map: (Int, Seq[IncomeSourceDetail])) =>
      val incomeSourceDetailSeq = map._2
      incomeSourceDetailSeq.size match {
        case (1) =>
          incomeSourceDetailSeq(0) match {
            case IncomeSourceDetail(name, id, amount, TaiConstants.CurrentTaxYear, isLive) =>
              IncomeSourceComparisonDetail(id, name, amount, TaiConstants.notApplicable.toLowerCase(), isLive)
            case IncomeSourceDetail(name, id, amount, TaiConstants.CurrentTaxYearPlusOne, isLive) =>
              IncomeSourceComparisonDetail(id, name, TaiConstants.notApplicable.toLowerCase(), amount, isLive)
          }
        case (2) => {
          val sortedSeq = incomeSourceDetailSeq.sortBy(_.taxYearStatus)
          IncomeSourceComparisonDetail(
            sortedSeq(0).empId,
            sortedSeq(0).name,
            sortedSeq(0).amount,
            sortedSeq(1).amount,
            sortedSeq(0).isLive)
        }
      }
    }
  }
}

case class IncomeSourceComparisonDetail(
  empId: Int,
  name: String,
  amountCY: String,
  amountCYPlusOne: String,
  isLive: Boolean)

case class IncomeSourceDetail(name: String, empId: Int, amount: String, taxYearStatus: String, isLive: Boolean)
