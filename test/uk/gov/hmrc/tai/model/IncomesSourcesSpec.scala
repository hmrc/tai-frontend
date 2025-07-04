/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model

import org.scalacheck.Gen
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain._
import utils.BaseSpec

import java.time.LocalDate

class IncomesSourcesSpec extends BaseSpec with ScalaCheckDrivenPropertyChecks with TableDrivenPropertyChecks {

  val employmentTaxCodeIncome =
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
  val pensionTaxCodeIncome    =
    TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

  def rtiAvailableIncomeSource: Gen[Seq[TaxedIncome]] = {
    val rtiAvailableStatuses: Gen[RealTimeStatus] =
      Gen.oneOf(Available, Unavailable)

    val employmentGen: Gen[Employment] =
      for {
        employerName   <- Gen.alphaNumStr
        realTimeStatus <- rtiAvailableStatuses
      } yield Employment(
        employerName,
        Live,
        Some("1ABC"),
        Some(LocalDate.of(2017, 3, 1)),
        None,
        Seq(AnnualAccount(7, uk.gov.hmrc.tai.model.TaxYear(), realTimeStatus, Nil, Nil)),
        "DIST1",
        "PAYE1",
        1,
        None,
        false,
        false,
        EmploymentIncome
      )

    for {
      employment  <- employmentGen
      taxedIncome <- Gen.listOf(TaxedIncome(Some(employmentTaxCodeIncome), employment))
    } yield taxedIncome
  }

  def rtiUnAvailableIncomeSource: Gen[Seq[TaxedIncome]] = {
    val employment =
      Employment(
        "employer",
        Live,
        Some("1ABC"),
        Some(LocalDate.of(2017, 3, 1)),
        None,
        Seq(AnnualAccount(7, uk.gov.hmrc.tai.model.TaxYear(), TemporarilyUnavailable, Nil, Nil)),
        "DIST1",
        "PAYE1",
        1,
        None,
        false,
        false,
        EmploymentIncome
      )

    for {
      taxedIncome <- Gen.listOf(TaxedIncome(Some(employmentTaxCodeIncome), employment))
    } yield taxedIncome
  }

  "rti is available if all income sources have an Avialable status" in {
    forAll(rtiAvailableIncomeSource, rtiAvailableIncomeSource, rtiAvailableIncomeSource) {
      (pensionIncome, employmentIncome, ceasedIncome) =>
        val incomeSources =
          IncomeSources(pensionIncome, employmentIncome, ceasedIncome)

        incomeSources.isRtiAvailable mustBe true
    }
  }

  "rti is unavailable if pension income is unavailable" in {
    forAll(rtiUnAvailableIncomeSource, rtiAvailableIncomeSource, rtiAvailableIncomeSource) {
      (pensionIncome, employmentIncome, ceasedIncome) =>
        whenever(pensionIncome.nonEmpty) {
          val incomeSources =
            IncomeSources(pensionIncome, employmentIncome, ceasedIncome)

          incomeSources.isRtiAvailable mustBe false
        }
    }
  }

  "rti is unavailable if employment income is unavailable" in {
    forAll(rtiAvailableIncomeSource, rtiUnAvailableIncomeSource, rtiAvailableIncomeSource) {
      (pensionIncome, employmentIncome, ceasedIncome) =>
        whenever(employmentIncome.nonEmpty) {
          val incomeSources =
            IncomeSources(pensionIncome, employmentIncome, ceasedIncome)

          incomeSources.isRtiAvailable mustBe false
        }
    }
  }

  "rti is unavailable if ceased income is unavailable" in {
    forAll(rtiAvailableIncomeSource, rtiAvailableIncomeSource, rtiUnAvailableIncomeSource) {
      (pensionIncome, employmentIncome, ceasedIncome) =>
        whenever(ceasedIncome.nonEmpty) {
          val incomeSources =
            IncomeSources(pensionIncome, employmentIncome, ceasedIncome)

          incomeSources.isRtiAvailable mustBe false
        }
    }
  }

  "rti is unavailable if all incomes are unavailable" in {
    forAll(rtiUnAvailableIncomeSource, rtiUnAvailableIncomeSource, rtiUnAvailableIncomeSource) {
      (pensionIncome, employmentIncome, ceasedIncome) =>
        whenever(pensionIncome.nonEmpty && employmentIncome.nonEmpty && ceasedIncome.nonEmpty) {
          val incomeSources =
            IncomeSources(pensionIncome, employmentIncome, ceasedIncome)

          incomeSources.isRtiAvailable mustBe false
        }
    }
  }
}
