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

package uk.gov.hmrc.tai.viewModels

import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.filters.TaxAccountFilter
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants


case class IncomeSourceComparisonViewModel(employmentIncomeSourceDetail: Seq[IncomeSourceComparisonDetail],
                                           pensionIncomeSourceDetail: Seq[IncomeSourceComparisonDetail]) extends ViewModelHelper

object IncomeSourceComparisonViewModel extends ViewModelHelper with TaxAccountFilter{

  def apply(taxCodeIncomesCY:Seq[TaxCodeIncome],
            employmentsCY:Seq[Employment],
            taxCodeIncomesCYPlusOne:Seq[TaxCodeIncome],
            employmentsCYPlusOne:Seq[Employment]): IncomeSourceComparisonViewModel = {

    val employmentTaxCodeIncomes = taxCodeIncomesCY filter liveEmployment
    val employmentIncomeSourceDetailCY = incomeSourceDetail(employmentTaxCodeIncomes, employmentsCY,TaiConstants.CurrentTaxYear)

    val employmentTaxCodeIncomesCYPlusOne = taxCodeIncomesCYPlusOne filter liveEmployment
    val employmentIncomeSourceDetailCYPlusOne = incomeSourceDetail(
      employmentTaxCodeIncomesCYPlusOne, employmentsCYPlusOne,TaiConstants.CurrentTaxYearPlusOne)

    val pensionTaxCodeIncomes = taxCodeIncomesCY filter livePension
    val pensionIncomeSourceDetailCY = incomeSourceDetail(pensionTaxCodeIncomes, employmentsCY,TaiConstants.CurrentTaxYear)

    val pensionTaxCodeIncomesCYPlusOne = taxCodeIncomesCYPlusOne filter livePension
    val pensionIncomeSourceDetailCYPlusOne = incomeSourceDetail(
      pensionTaxCodeIncomesCYPlusOne, employmentsCYPlusOne,TaiConstants.CurrentTaxYearPlusOne)

    val employmentIncomeSourceComparisonDetailSeq = incomeSourceComparisionDetail(
      employmentIncomeSourceDetailCY, employmentIncomeSourceDetailCYPlusOne).sortBy(_.amountCY).reverse

    val pensionIncomeSourceComparisonDetailSeq = incomeSourceComparisionDetail(
      pensionIncomeSourceDetailCY, pensionIncomeSourceDetailCYPlusOne).sortBy(_.amountCY).reverse

    IncomeSourceComparisonViewModel(employmentIncomeSourceComparisonDetailSeq,pensionIncomeSourceComparisonDetailSeq)

  }

  private def incomeSourceDetail(taxCodeIncomes: Seq[TaxCodeIncome], employments: Seq[Employment], taxYearStatus:String): Seq[IncomeSourceDetail] = {
    taxCodeIncomes.flatMap { (t: TaxCodeIncome) =>
      t.employmentId.flatMap { (id: Int) =>
        employments.find(_.sequenceNumber == id).map{ e:Employment =>
          val amount = withPoundPrefixAndSign(MoneyPounds(t.amount, 0))
          IncomeSourceDetail(e.name,e.sequenceNumber, amount, taxYearStatus)
        }
      }
    }
  }

  private def incomeSourceComparisionDetail(incomeSourceDetailCY:Seq[IncomeSourceDetail],
                                            incomeSourceDetailCYPlusOne:Seq[IncomeSourceDetail]): Seq[IncomeSourceComparisonDetail] = {

    val incomeSourcesCombined = incomeSourceDetailCY ++ incomeSourceDetailCYPlusOne

    incomeSourcesCombined.groupBy(_.empId).toSeq.map{ (map: (Int, Seq[IncomeSourceDetail])) =>
      val incomeSourceDetailSeq = map._2
      incomeSourceDetailSeq.size match {
        case(1) => incomeSourceDetailSeq(0) match {
          case IncomeSourceDetail(name,_,amount,TaiConstants.CurrentTaxYear) => IncomeSourceComparisonDetail(name,amount,TaiConstants.notApplicable.toLowerCase())
          case IncomeSourceDetail(name,_,amount,TaiConstants.CurrentTaxYearPlusOne) => IncomeSourceComparisonDetail(name,TaiConstants.notApplicable.toLowerCase(),amount)
        }
        case(2) =>{
          val sortedSeq = incomeSourceDetailSeq.sortBy(_.taxYearStatus)
          IncomeSourceComparisonDetail(sortedSeq(0).name,sortedSeq(0).amount,sortedSeq(1).amount)
        }
      }
    }
  }

}

case class IncomeSourceComparisonDetail(name: String, amountCY:String, amountCYPlusOne:String)
case class IncomeSourceDetail(name:String,empId:Int, amount:String, taxYearStatus:String)