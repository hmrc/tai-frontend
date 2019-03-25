/*
 * Copyright 2019 HM Revenue & Customs
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

import uk.gov.hmrc.tai.model.CodingComponentPair
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

case class AllowancesAndDeductionPairs(allowances: Seq[CodingComponentPair], deductions: Seq[CodingComponentPair]) {
  def totalAllowances: (BigDecimal, BigDecimal) = {
    (allowances.flatMap(_.previous).sum, allowances.flatMap(_.current).sum)
  }
}

object AllowancesAndDeductionPairs {

  def fromCodingComponents(previousCodingComponents: Seq[CodingComponent], currentCodingComponents: Seq[CodingComponent]): AllowancesAndDeductionPairs = {
    val pairs = pairCodingComponents(previousCodingComponents, currentCodingComponents)
    val allowances = getAllowances(pairs)
    val deductions = getDeductions(pairs)

    AllowancesAndDeductionPairs(allowances, deductions)
  }

  private def pairCodingComponents(previous: Seq[CodingComponent], current: Seq[CodingComponent]): Seq[CodingComponentPair] = {
    val pairs = findPairs(previous, current)
    val nonPairs = findNonPairs(pairs, previous)

    pairs ++ nonPairs
  }

  private def findPairs(previous: Seq[CodingComponent], current: Seq[CodingComponent]): Seq[CodingComponentPair] = {
    current.map( addition => {
      previous.find(matchingCodingComponents(_, addition)) match {
        case Some(previousMatched) => CodingComponentPair(addition.componentType, addition.employmentId, Some(previousMatched.amount), Some(addition.amount))
        case None => CodingComponentPair(addition.componentType, addition.employmentId, None, Some(addition.amount))
      }
    })
  }

  private def findNonPairs(pairs: Seq[CodingComponentPair], rest: Seq[CodingComponent]): Seq[CodingComponentPair] = {
    rest.flatMap(addition => {
      pairs.find(matchingCodingComponents(_, addition)) match {
        case Some(_) => None
        case None => Some(CodingComponentPair(addition.componentType, addition.employmentId, Some(addition.amount), None))
      }
    })
  }

  private def matchingCodingComponents(lhs: CodingComponent, rhs: CodingComponent): Boolean = {
    lhs.employmentId == rhs.employmentId &&
      lhs.componentType == rhs.componentType
  }

  private def matchingCodingComponents(lhs: CodingComponentPair, rhs: CodingComponent): Boolean = {
    lhs.employmentId == rhs.employmentId &&
      lhs.componentType == rhs.componentType
  }

  private def getDeductions(codingComponents: Seq[CodingComponentPair]): Seq[CodingComponentPair] = {
    codingComponents.filter({
      _.componentType match {
        case _: AllowanceComponentType => false
        case _ => true
      }
    })
  }

  private def getAllowances(codingComponents: Seq[CodingComponentPair]): Seq[CodingComponentPair] = {
    codingComponents.filterNot(component => PersonalAllowance.isA(component.componentType)).filter({
      _.componentType match {
        case _: AllowanceComponentType => true
        case _ => false
      }
    })
  }
}