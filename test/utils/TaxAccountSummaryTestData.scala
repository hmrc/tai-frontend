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

package utils

import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._

import java.time.LocalDate

trait TaxAccountSummaryTestData {

  private val startDate: LocalDate =
    TaxYear().start.withMonth(3).withDayOfMonth(1)

  private val endDate: LocalDate =
    TaxYear().end.withMonth(4).withDayOfMonth(21)

  val employment = Employment(
    "Employer name",
    Live,
    Some("123ABC"),
    startDate,
    Some(endDate),
    Seq.empty[AnnualAccount],
    "DIST123",
    "PAYE543",
    1,
    None,
    false,
    false
  )

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(1111, 2222, 333.32, 444.44, 111.11)

  val nonMatchingSequenceNumber = 998

  val nonTaxCodeIncome = NonTaxCodeIncome(
    Some(
      uk.gov.hmrc.tai.model.domain.income
        .UntaxedInterest(UntaxedInterestIncome, None, 100, "Untaxed Interest", Seq.empty[BankAccount])),
    Seq(
      OtherNonTaxCodeIncome(Profit, None, 100, "Profit")
    )
  )

  val liveEmployment1 =
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
  val liveEmployment2 =
    TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment", "BR", "employer2", Week1Month1BasisOfOperation, Live)
  val livePension3 =
    TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)
  val livePension4 =
    TaxCodeIncome(PensionIncome, Some(4), 4444, "employment", "BR", "employer4", Week1Month1BasisOfOperation, Live)
  val potentiallyCeasedEmployment9 = TaxCodeIncome(
    EmploymentIncome,
    Some(9),
    1111,
    "employment",
    "1150L",
    "employer9",
    OtherBasisOfOperation,
    PotentiallyCeased)
  val ceasedEmployment10 = TaxCodeIncome(
    EmploymentIncome,
    Some(10),
    2222,
    "employment",
    "BR",
    "employer10",
    Week1Month1BasisOfOperation,
    Ceased)
  val empEmployment1 = Employment(
    "Employer name1",
    Live,
    Some("1ABC"),
    startDate,
    None,
    Seq.empty[AnnualAccount],
    "DIST1",
    "PAYE1",
    1,
    None,
    false,
    false)
  val empEmployment2 = Employment(
    "Employer name2",
    Live,
    Some("1ABC"),
    startDate,
    None,
    Seq.empty[AnnualAccount],
    "DIST2",
    "PAYE2",
    2,
    None,
    false,
    false)
  val pensionEmployment3 = Employment(
    "Pension name1",
    Live,
    Some("3ABC"),
    startDate,
    None,
    Seq.empty[AnnualAccount],
    "DIST3",
    "PAYE3",
    3,
    None,
    false,
    false)
  val pensionEmployment4 = Employment(
    "Pension name2",
    Live,
    Some("4ABC"),
    startDate,
    None,
    Seq.empty[AnnualAccount],
    "DIST4",
    "PAYE4",
    4,
    None,
    false,
    false)
  val empEmployment9 = Employment(
    "Employer name3",
    Live,
    Some("9ABC"),
    startDate,
    None,
    Seq.empty[AnnualAccount],
    "DIST9",
    "PAYE9",
    9,
    None,
    false,
    false)
  val empEmployment10 = Employment(
    "Employer name4",
    Live,
    Some("10ABC"),
    startDate,
    Some(endDate),
    Seq.empty[AnnualAccount],
    "DIST10",
    "PAYE10",
    10,
    None,
    false,
    false
  )

  val livePensionIncomeSources: Seq[TaxedIncome] = Seq(
    TaxedIncome(livePension3, pensionEmployment3),
    TaxedIncome(livePension4, pensionEmployment4)
  )

  val liveEmploymentIncomeSources: Seq[TaxedIncome] = Seq(
    TaxedIncome(liveEmployment1, empEmployment1),
    TaxedIncome(liveEmployment2, empEmployment2)
  )

  val ceasedEmploymentIncomeSources: Seq[TaxedIncome] = Seq(
    TaxedIncome(potentiallyCeasedEmployment9, empEmployment9),
    TaxedIncome(ceasedEmployment10, empEmployment10)
  )
  val taxCodeIncome =
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)

  val taxCodeIncomeCeased =
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Ceased)

  val payment = Payment(
    LocalDate.now,
    BigDecimal(123.45),
    BigDecimal(678.90),
    BigDecimal(123.12),
    BigDecimal(444.44),
    BigDecimal(555.55),
    BigDecimal(666.66),
    Monthly)
  val annualAccount = AnnualAccount(uk.gov.hmrc.tai.model.TaxYear(), Available, Seq(payment), Nil)
  val ceasedEmployment = Employment(
    "Ceased employer name",
    Ceased,
    Some("123ABC"),
    startDate,
    Some(endDate),
    Seq(annualAccount),
    "DIST123",
    "PAYE543",
    1,
    None,
    false,
    false
  )

  val nonMatchedEmployments = Seq(
    ceasedEmployment.copy(sequenceNumber = nonMatchingSequenceNumber),
    Employment(
      "Pension name1",
      Live,
      Some("3ABC"),
      startDate,
      None,
      Seq.empty[AnnualAccount],
      "DIST3",
      "PAYE3",
      999,
      None,
      false,
      false)
  )

}

object TaxAccountSummaryTestData extends TaxAccountSummaryTestData
