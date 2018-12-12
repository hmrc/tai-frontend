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

package uk.gov.hmrc.tai.util

import uk.gov.hmrc.tai.model.domain.{AllowanceComponentType, DeductionComponentType, TaxComponentType}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

case class CodingComponentPair(componentType: TaxComponentType, employmentId: Option[Int], previous: BigDecimal, current: BigDecimal)

case class MungedCodingComponents(munged: Seq[CodingComponentPair] = Seq.empty)

object MungedCodingComponents extends isPersonalAllowance {
  def apply(previousCodingComponents: Seq[CodingComponent],
            currentCodingComponents: Seq[CodingComponent]): MungedCodingComponents = {

    val currentDeductions = getDeductions(currentCodingComponents)
    val currentAdditions = getAllowances(currentCodingComponents)
    val previousDeductions = getDeductions(previousCodingComponents)
    val previousAdditions = getAllowances(previousCodingComponents)

    if (currentDeductions.isEmpty && currentAdditions.isEmpty && previousDeductions.isEmpty && previousAdditions.isEmpty) {
      return MungedCodingComponents(Seq.empty)
    }

    val additionPairs = currentAdditions.map( addition => {
      previousAdditions.find(matchingCodingComponents(_, addition)) match {
        case Some(previous) => CodingComponentPair(addition.componentType, addition.employmentId, previous.amount, addition.amount)
        case None => CodingComponentPair(addition.componentType, addition.employmentId, 0, addition.amount)
      }
    })

    val previousPairs: Seq[CodingComponentPair] = previousAdditions.flatMap(addition => {
      additionPairs.find(matchingCodingComponents(_, addition)) match {
        case Some(previous) => None
        case None => Some(CodingComponentPair(addition.componentType, addition.employmentId, addition.amount, 0))
      }
    })

    MungedCodingComponents(additionPairs ++ previousPairs)
  }

//  def pairCodingComponents(previous: Seq[CodingComponent], current: Seq[CodingComponent]): Seq[CodingComponentPair] = {
//
//  }

  private def matchingCodingComponents(lhs: CodingComponent, rhs: CodingComponent): Boolean = {
    lhs.employmentId == rhs.employmentId &&
      lhs.componentType == rhs.componentType
  }

  private def matchingCodingComponents(lhs: CodingComponentPair, rhs: CodingComponent): Boolean = {
    lhs.employmentId == rhs.employmentId &&
      lhs.componentType == rhs.componentType
  }

  private def getDeductions(codingComponents: Seq[CodingComponent]): Seq[CodingComponent] = {
    codingComponents.filter({
      _.componentType match {
        case _: AllowanceComponentType => false
        case _ => true
      }
    })
  }

  private def getAllowances(codingComponents: Seq[CodingComponent]): Seq[CodingComponent] = {
    codingComponents.filterNot(isPersonalAllowanceComponent).filter({
      _.componentType match {
        case _: DeductionComponentType => false
        case _ => true
      }
    })
  }
}