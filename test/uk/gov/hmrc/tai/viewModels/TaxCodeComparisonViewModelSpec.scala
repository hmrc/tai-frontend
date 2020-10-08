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

package uk.gov.hmrc.tai.viewModels

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import utils.BaseSpec

class TaxCodeComparisonViewModelSpec extends BaseSpec {

  "Tax code comparison view model" must {

    "return the same quantity of tax codes within each generated TaxCodeDetail as there are tax years requested" in {

      val prevYearMinus2Request =
        TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(new LocalDate().minusYears(2)), currentTaxCodeIncomes)
      val prevYearRequest = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().prev, Nil)
      val currentYearRequest = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), Nil)
      val nextYearRequest = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, Nil)

      val result =
        TaxCodeComparisonViewModel(Seq(prevYearMinus2Request, prevYearRequest, currentYearRequest, nextYearRequest))

      result.employmentTaxCodes.map { tcd =>
        tcd.taxCodes.length mustBe 4
      }
      result.pensionTaxCodes.map { tcd =>
        tcd.taxCodes.length mustBe 4
      }
    }

    "return Live income and pension sources tax codes" when {
      "current year and next year income sources passed to view model" in {
        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), currentTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nextYearTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.employmentTaxCodes must contain {
          TaxCodeDetail("employer2", Seq("BR", "NBR"))
        }
        taxCodeDetails.employmentTaxCodes must contain {
          TaxCodeDetail("employer1", Seq("1150L", "1250L"))
        }

        taxCodeDetails.pensionTaxCodes must contain {
          TaxCodeDetail("pension2", Seq("BR", "PBR"))
        }

        taxCodeDetails.pensionTaxCodes must contain {
          TaxCodeDetail("pension1", Seq("1150L", "1250L"))
        }

      }
    }

    "return 'not applicable' placeholder tax code values" when {
      "two tax years are requested, but there is a missing income source record for a the earlier tax year" in {
        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), currentTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nextYearMissingTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.employmentTaxCodes must contain {
          TaxCodeDetail("employer2", Seq("BR", "NBR"))
        }
        taxCodeDetails.employmentTaxCodes must contain {
          TaxCodeDetail("employer1", Seq("1150L", Messages("tai.incomeTaxComparison.incomeSourceAbsent")))
        }
        taxCodeDetails.pensionTaxCodes must contain {
          TaxCodeDetail("pension2", Seq("BR", "PBR"))
        }
        taxCodeDetails.pensionTaxCodes must contain {
          TaxCodeDetail("pension1", Seq("1150L", "1250L"))
        }

      }

      "multiple tax years are requested, but there is a missing income source record for the later tax year" in {
        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), currentMissingTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nextYearTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.employmentTaxCodes must contain {
          TaxCodeDetail("employer2", Seq("BR", "NBR"))
        }
        taxCodeDetails.employmentTaxCodes must contain {
          TaxCodeDetail("employer1", Seq(Messages("tai.incomeTaxComparison.incomeSourceAbsent"), "1250L"))
        }
        taxCodeDetails.pensionTaxCodes must contain {
          TaxCodeDetail("pension2", Seq("BR", "PBR"))
        }
        taxCodeDetails.pensionTaxCodes must contain {
          TaxCodeDetail("pension1", Seq("1150L", "1250L"))
        }
      }
    }

    "return hasScottishTaxCodeNextYear as true" when {
      "cy plus one employment tax code start with S" in {
        val cyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live)
        )
        val nyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "S1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            1111,
            "employment",
            "11150L",
            "employer1",
            OtherBasisOfOperation,
            Live)
        )

        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), cyTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nyTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.hasScottishTaxCodeNextYear mustBe true

      }

      "cy plus one pension tax code start with S" in {
        val cyTaxCodeIncomes = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
        )
        val nyTaxCodeIncomes = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 1111, "employment", "S1150L", "employer1", OtherBasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "11150L", "employer1", OtherBasisOfOperation, Live)
        )

        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), cyTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nyTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.hasScottishTaxCodeNextYear mustBe true
      }
    }

    "return hasScottishTaxCodeNextYear as false" when {
      "cy employment tax code start with S" in {
        val cyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "S1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live)
        )
        val nyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "S1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            1111,
            "employment",
            "11150L",
            "employer1",
            OtherBasisOfOperation,
            Live)
        )

        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), cyTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nyTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.hasScottishTaxCodeNextYear mustBe false

      }

      "cy pension tax code start with S" in {
        val cyTaxCodeIncomes = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 1111, "employment", "S1150L", "employer1", OtherBasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
        )
        val nyTaxCodeIncomes = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 1111, "employment", "S1150L", "employer1", OtherBasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "11150L", "employer1", OtherBasisOfOperation, Live)
        )

        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), cyTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nyTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.hasScottishTaxCodeNextYear mustBe false
      }

      "no employment tax code start with S" in {
        val cyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live)
        )
        val nyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            1111,
            "employment",
            "11150L",
            "employer1",
            OtherBasisOfOperation,
            Live)
        )

        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), cyTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nyTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.hasScottishTaxCodeNextYear mustBe false

      }

      "no pension tax code start with S" in {
        val cyTaxCodeIncomes = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
        )
        val nyTaxCodeIncomes = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "11150L", "employer1", OtherBasisOfOperation, Live)
        )

        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), cyTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nyTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.hasScottishTaxCodeNextYear mustBe false
      }

      "cy employment starts with S and cy plus one pension starts with S" in {
        val cyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "S1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
        )
        val nyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "S11150L", "employer1", OtherBasisOfOperation, Live)
        )

        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), cyTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nyTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.hasScottishTaxCodeNextYear mustBe false
      }

      "cy pension starts with S and cy plus one employment starts with S" in {
        val cyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "S1150L", "employer1", OtherBasisOfOperation, Live)
        )
        val nyTaxCodeIncomes = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment",
            "S1150L",
            "employer1",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(PensionIncome, Some(2), 1111, "employment", "11150L", "employer1", OtherBasisOfOperation, Live)
        )

        val currentYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear(), cyTaxCodeIncomes)
        val nextYearDetails = TaxCodeIncomesForYear(uk.gov.hmrc.tai.model.TaxYear().next, nyTaxCodeIncomes)

        val taxCodeDetails = TaxCodeComparisonViewModel(Seq(nextYearDetails, currentYearDetails))

        taxCodeDetails.hasScottishTaxCodeNextYear mustBe false
      }
    }
  }

  val currentTaxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(11), 1111, "employment", "1150L", "pension1", OtherBasisOfOperation, Live),
    TaxCodeIncome(
      PensionIncome,
      Some(12),
      1111,
      "employment",
      "1150L",
      "pension3",
      OtherBasisOfOperation,
      PotentiallyCeased),
    TaxCodeIncome(PensionIncome, Some(13), 1111, "employment", "1150L", "pension3", OtherBasisOfOperation, Ceased),
    TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment", "BR", "employer2", Week1Month1BasisOfOperation, Live),
    TaxCodeIncome(
      JobSeekerAllowanceIncome,
      None,
      6666,
      "employment",
      "BR",
      "employer6",
      Week1Month1BasisOfOperation,
      Live),
    TaxCodeIncome(OtherIncome, Some(7), 7777, "employment", "1150L", "employer7", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, None, 2222, "employment", "BR", "pension2", Week1Month1BasisOfOperation, Live)
  )

  val currentMissingTaxCodeIncomes = Seq(
    TaxCodeIncome(PensionIncome, Some(11), 1111, "employment", "1150L", "pension1", OtherBasisOfOperation, Live),
    TaxCodeIncome(
      PensionIncome,
      Some(12),
      1111,
      "employment",
      "1150L",
      "pension3",
      OtherBasisOfOperation,
      PotentiallyCeased),
    TaxCodeIncome(PensionIncome, Some(13), 1111, "employment", "1150L", "pension3", OtherBasisOfOperation, Ceased),
    TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment", "BR", "employer2", Week1Month1BasisOfOperation, Live),
    TaxCodeIncome(
      JobSeekerAllowanceIncome,
      None,
      6666,
      "employment",
      "BR",
      "employer6",
      Week1Month1BasisOfOperation,
      Live),
    TaxCodeIncome(OtherIncome, Some(7), 7777, "employment", "1150L", "employer7", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, None, 2222, "employment", "BR", "pension2", Week1Month1BasisOfOperation, Live)
  )

  val nextYearTaxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1250L", "employer1", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(11), 1111, "employment", "1250L", "pension1", OtherBasisOfOperation, Live),
    TaxCodeIncome(
      PensionIncome,
      Some(12),
      1111,
      "employment",
      "1250L",
      "pension1",
      OtherBasisOfOperation,
      PotentiallyCeased),
    TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment", "NBR", "employer2", Week1Month1BasisOfOperation, Live),
    TaxCodeIncome(
      JobSeekerAllowanceIncome,
      Some(6),
      6666,
      "employment",
      "ABR",
      "employer6",
      Week1Month1BasisOfOperation,
      Live),
    TaxCodeIncome(OtherIncome, Some(7), 7777, "employment", "2150L", "employer7", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, None, 2222, "employment", "PBR", "pension2", Week1Month1BasisOfOperation, Live)
  )

  val nextYearMissingTaxCodeIncomes = Seq(
    TaxCodeIncome(PensionIncome, Some(11), 1111, "employment", "1250L", "pension1", OtherBasisOfOperation, Live),
    TaxCodeIncome(
      PensionIncome,
      Some(12),
      1111,
      "employment",
      "1250L",
      "pension1",
      OtherBasisOfOperation,
      PotentiallyCeased),
    TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment", "NBR", "employer2", Week1Month1BasisOfOperation, Live),
    TaxCodeIncome(
      JobSeekerAllowanceIncome,
      Some(6),
      6666,
      "employment",
      "ABR",
      "employer6",
      Week1Month1BasisOfOperation,
      Live),
    TaxCodeIncome(OtherIncome, Some(7), 7777, "employment", "2150L", "employer7", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, None, 2222, "employment", "PBR", "pension2", Week1Month1BasisOfOperation, Live)
  )
}
