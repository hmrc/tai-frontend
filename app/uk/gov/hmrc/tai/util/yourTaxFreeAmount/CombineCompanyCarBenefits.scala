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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import play.api.i18n.Messages

case class CombinedCarGrossAmountPairs(previous: BigDecimal, current: BigDecimal, carDescription: String)

case class CombineCompanyCarBenefits(combinedCarGrossAmountPairs: Seq[CombinedCarGrossAmountPairs])

object CombineCompanyCarBenefits {
  def apply(companyCarBenefitPairs: CompanyCarBenefitPairs)
           (implicit messages: Messages): Seq[CombinedCarGrossAmountPairs] = {
    val previous = companyCarBenefitPairs.previous
    val current = companyCarBenefitPairs.current

    if(hasNoBenefits(companyCarBenefitPairs)) {
      Seq.empty
    } else if (previous.map(_.carDescription) == current.map(_.carDescription)) {
      Seq(CombinedCarGrossAmountPairs(
        getAmount(previous),
        getAmount(current),
        getCarDescription(current))
      )
    } else if (previous.isEmpty) {
      Seq(makeCurrentPair(current))
    } else if (current.isEmpty) {
      Seq(makePreviousPair(previous))
    } else {
      Seq(makePreviousPair(previous),
        makeCurrentPair(current)
      )
    }
  }

  private def hasNoBenefits(companyCarBenefitPairs: CompanyCarBenefitPairs): Boolean = {
    companyCarBenefitPairs.previous.isEmpty && companyCarBenefitPairs.current.isEmpty
  }

  private def makeCurrentPair(current: Option[CarGrossAmountPairs])(implicit messages: Messages):
  CombinedCarGrossAmountPairs = {
    CombinedCarGrossAmountPairs(0, getAmount(current), getCarDescription(current))
  }

  private def makePreviousPair(previous: Option[CarGrossAmountPairs])(implicit messages: Messages):
  CombinedCarGrossAmountPairs = {
    CombinedCarGrossAmountPairs(getAmount(previous), 0, getCarDescription(previous))
  }

  private def getAmount(pairs: Option[CarGrossAmountPairs]): BigDecimal = {
    pairs.map(_.grossAmount).getOrElse(0)
  }

  private def getCarDescription(pairs: Option[CarGrossAmountPairs])(implicit messages: Messages): String = {
    pairs.map(_.carDescription).getOrElse(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"))
  }
}
