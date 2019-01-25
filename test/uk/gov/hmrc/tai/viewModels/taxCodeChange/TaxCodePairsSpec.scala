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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxCodeRecord
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation

import scala.util.Random

class TaxCodePairsSpec extends PlaySpec{

  "TaxCodePairs" should {
    "return the primary pair when only one pair exists" in {
      val model = TaxCodePairs(
        Seq(primaryFullYearTaxCode),
        Seq(primaryFullYearTaxCode)
      )
      model.pairs mustEqual Seq(TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)))
    }

    "return the primary pair first when multiple pairs exist" in {
      val model = TaxCodePairs(
        Seq(primaryFullYearTaxCode, previousTaxCodeRecord1, fullYearTaxCode),
        Seq(primaryFullYearTaxCode, currentTaxCodeRecord1, fullYearTaxCode)
      )
      model.pairs(0) mustEqual TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode))
    }

    "return the primary pair first when unmatched pairs exist" in {
      val model = TaxCodePairs(
        Seq(primaryFullYearTaxCode, previousTaxCodeRecord1, fullYearTaxCode),
        Seq(primaryFullYearTaxCode, currentTaxCodeRecord1, fullYearTaxCode)
      )
      model.pairs(0) mustEqual TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode))
    }

    "return the secondary pairs after the primary pairs" in {
      val model = TaxCodePairs(
        Seq(previousTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedPreviousTaxCode),
        Seq(currentTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedCurrentTaxCode)
      )

      model.pairs(1) mustEqual TaxCodePair(Some(previousTaxCodeRecord1), Some(currentTaxCodeRecord1))
      model.pairs(2) mustEqual TaxCodePair(Some(fullYearTaxCode), Some(fullYearTaxCode))
    }

    "return the unmatched current and previous taxCodes given one in current and one in previous" in {
      val model = TaxCodePairs(
        Seq(unmatchedPreviousTaxCode),
        Seq(unmatchedCurrentTaxCode)
      )
      model.pairs mustEqual Seq(
        TaxCodePair(Some(unmatchedPreviousTaxCode), None),
        TaxCodePair(None, Some(unmatchedCurrentTaxCode))
      )
    }

    "return all tax codes pairs ordered by primary, secondary, unmatched previous, unmatched current" in {
      val model = TaxCodePairs(
        Seq(previousTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedPreviousTaxCode),
        Seq(currentTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedCurrentTaxCode)
      )
      model.pairs mustEqual Seq(
        TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)),
        TaxCodePair(Some(previousTaxCodeRecord1), Some(currentTaxCodeRecord1)),
        TaxCodePair(Some(fullYearTaxCode), Some(fullYearTaxCode)),
        TaxCodePair(Some(unmatchedPreviousTaxCode), None),
        TaxCodePair(None, Some(unmatchedCurrentTaxCode))
      )
    }

    "return pairs based on employment name when duplicate payroll numbers exist" in {
      val dateOfTaxCodeChange = startDate.plusMonths(1)
      val secondaryEmployer1Before = TaxCodeRecord("code", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation,"Employer 1", false, Some("A-1234"), false)
      val secondaryEmployer2Before = TaxCodeRecord("code", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation,"Employer 2", false, Some("A-1234"), false)
      val secondaryEmployer1After = secondaryEmployer1Before.copy(startDate =  startDate.plusMonths(1), endDate = TaxYear().end)
      val secondaryEmployer2After = secondaryEmployer2Before.copy(startDate =  startDate.plusMonths(1), endDate = TaxYear().end)

      val model = TaxCodePairs(
        Seq(primaryFullYearTaxCode, secondaryEmployer1Before, secondaryEmployer2Before),
        Seq(primaryFullYearTaxCode, secondaryEmployer1After, secondaryEmployer2After)
      )

      model.pairs mustEqual Seq(
        TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)),
        TaxCodePair(Some(secondaryEmployer1Before), Some(secondaryEmployer1After)),
        TaxCodePair(Some(secondaryEmployer2Before), Some(secondaryEmployer2After))
      )

    }

    "return pairs based on employment name when no payroll numbers exist for one employment name" in {
      val dateOfTaxCodeChange = startDate.plusMonths(1)
      val secondaryEmployer1Before = TaxCodeRecord("code", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation, "Employer 1", false, None, false)
      val secondaryEmployer2Before = TaxCodeRecord("code", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation,"Employer 2", false, None, false)
      val secondaryEmployer1After = secondaryEmployer1Before.copy(startDate =  startDate.plusMonths(1), endDate = TaxYear().end)
      val secondaryEmployer2After = secondaryEmployer2Before.copy(startDate =  startDate.plusMonths(1), endDate = TaxYear().end)

      val model = TaxCodePairs(
        Seq(primaryFullYearTaxCode, secondaryEmployer1Before, secondaryEmployer2Before),
        Seq(primaryFullYearTaxCode, secondaryEmployer1After, secondaryEmployer2After)
      )

      model.pairs mustEqual Seq(
        TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)),
        TaxCodePair(Some(secondaryEmployer1Before), Some(secondaryEmployer1After)),
        TaxCodePair(Some(secondaryEmployer2Before), Some(secondaryEmployer2After))
      )

    }

    "return no duplicating pairs when payroll numbers don't exists but employment names are different" in {
      val dateOfTaxCodeChange = startDate.plusMonths(1)
      val secondaryEmployer1Before = TaxCodeRecord("code", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation,"Employer 1", false, None, false)
      val secondaryEmployer2Before = TaxCodeRecord("code", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation,"Employer 2", false, None, false)
      val secondaryEmployer3After = TaxCodeRecord("code", startDate.plusMonths(1), TaxYear().end, OtherBasisOfOperation,"Employer 3", false, None, false)
      val secondaryEmployer4After = TaxCodeRecord("code", startDate.plusMonths(1), TaxYear().end, OtherBasisOfOperation,"Employer 4", false, None, false)

      val model = TaxCodePairs(
        Seq(primaryFullYearTaxCode, secondaryEmployer1Before, secondaryEmployer2Before),
        Seq(primaryFullYearTaxCode, secondaryEmployer3After, secondaryEmployer4After)
      )

      model.pairs mustEqual Seq(
        TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)),
        TaxCodePair(Some(secondaryEmployer1Before), None),
        TaxCodePair(Some(secondaryEmployer2Before), None),
        TaxCodePair(None, Some(secondaryEmployer3After)),
        TaxCodePair(None, Some(secondaryEmployer4After))
      )

    }

    "return non duplicating pairs when payroll numbers don't exist and employment name is the same by split by primary and secondary" in {
      val dateOfTaxCodeChange = startDate.plusMonths(1)
      val primaryEmployer1Before = TaxCodeRecord("code", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation,"Employer 1", false, None, true)
      val secondaryEmployer2Before = TaxCodeRecord("code", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation,"Employer 1", false, None, false)

      val primaryEmployer1After = TaxCodeRecord("code", startDate.plusMonths(1), TaxYear().end, OtherBasisOfOperation,"Employer 1", false, None, true)
      val secondaryEmployer2After = TaxCodeRecord("code", startDate.plusMonths(1), TaxYear().end, OtherBasisOfOperation,"Employer 1", false, None, false)

      val model = TaxCodePairs(
        Seq(primaryEmployer1Before, secondaryEmployer2Before),
        Seq(primaryEmployer1After, secondaryEmployer2After)
      )

      model.pairs mustEqual Seq(
        TaxCodePair(Some(primaryEmployer1Before), Some(primaryEmployer1After)),
        TaxCodePair(Some(secondaryEmployer2Before), Some(secondaryEmployer2After))
      )
    }


    "return the best possible match when multiple payroll numbers are missing for a employer" in {
      val dateOfTaxCodeChange = startDate.plusMonths(1)
      val secondaryEmployer1ABefore = TaxCodeRecord("code 1a", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation,"Employer 1", false, None, false)
      val secondaryEmployer1BBefore = TaxCodeRecord("code 1b", startDate, dateOfTaxCodeChange.minusDays(1), OtherBasisOfOperation,"Employer 1", false, None, false)

      val secondaryEmployer1AAfter = TaxCodeRecord("code 1a - after", dateOfTaxCodeChange, TaxYear().end, OtherBasisOfOperation, "Employer 1", false, None, false)
      val secondaryEmployer1BAfter = TaxCodeRecord("code 1b - after", dateOfTaxCodeChange, TaxYear().end, OtherBasisOfOperation, "Employer 1", false, None, false)

      val orderings = Table(
        ("previous", "current"),
        (Seq(secondaryEmployer1ABefore, secondaryEmployer1BBefore), Seq(secondaryEmployer1AAfter, secondaryEmployer1BAfter)),
        (Seq(secondaryEmployer1BBefore, secondaryEmployer1ABefore), Seq(secondaryEmployer1AAfter, secondaryEmployer1BAfter)),

        (Seq(secondaryEmployer1ABefore, secondaryEmployer1BBefore), Seq(secondaryEmployer1BAfter, secondaryEmployer1AAfter)),
        (Seq(secondaryEmployer1BBefore, secondaryEmployer1ABefore), Seq(secondaryEmployer1BAfter, secondaryEmployer1AAfter))
      )

      val possibleOrder1 = Seq(
        TaxCodePair(Some(secondaryEmployer1ABefore), Some(secondaryEmployer1AAfter)),
        TaxCodePair(Some(secondaryEmployer1BBefore), Some(secondaryEmployer1BAfter))
      )

      val possibleOrder2 = Seq(
        TaxCodePair(Some(secondaryEmployer1ABefore), Some(secondaryEmployer1BAfter)),
        TaxCodePair(Some(secondaryEmployer1BBefore), Some(secondaryEmployer1AAfter))
      )

      val possibleOrder3 = Seq(
        TaxCodePair(Some(secondaryEmployer1BBefore), Some(secondaryEmployer1AAfter)),
        TaxCodePair(Some(secondaryEmployer1ABefore), Some(secondaryEmployer1BAfter))
      )

      val possibleOrder4 = Seq(
        TaxCodePair(Some(secondaryEmployer1BBefore), Some(secondaryEmployer1BAfter)),
        TaxCodePair(Some(secondaryEmployer1ABefore), Some(secondaryEmployer1AAfter))
      )


      forAll(orderings) { (p: Seq[TaxCodeRecord], c: Seq[TaxCodeRecord]) =>
        val pairs = TaxCodePairs(p, c).pairs
        assert(Set(possibleOrder1, possibleOrder2, possibleOrder3, possibleOrder4).contains(pairs))
      }
    }
  }

  val nino = generateNino
  val startDate = TaxYear().start
  val previousTaxCodeRecord1 = TaxCodeRecord("code", startDate, startDate.plusMonths(1), OtherBasisOfOperation,"A Employer 1", false, Some("A-1234"), false)
  val currentTaxCodeRecord1 = previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYear().end)
  val fullYearTaxCode = TaxCodeRecord("code", startDate, TaxYear().end, OtherBasisOfOperation, "B Employer 1", false, Some("B-1234"), false)
  val primaryFullYearTaxCode = fullYearTaxCode.copy(employerName = "C", payrollNumber = Some("C-1234"), primary = true)
  val unmatchedPreviousTaxCode = TaxCodeRecord("Unmatched Previous", startDate, startDate.plusMonths(1), OtherBasisOfOperation,"D", false, Some("D Payroll Id"), false)
  val unmatchedCurrentTaxCode = TaxCodeRecord("Unmatched Current", startDate.plusMonths(1), TaxYear().end, OtherBasisOfOperation,"E", false,Some("E Payroll id"), false)

  private def generateNino: Nino = new Generator(new Random).nextNino
}
