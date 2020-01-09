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

package uk.gov.hmrc.tai.binders

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.TaxYear

class TaxYearObjectBinderSpec extends PlaySpec with FakeTaiPlayApplication {

  "TaxYearObjectBinder - bind" should {

    "return the tax year" when {
      "the supplied tax year isn't later than cy+1" in {

        val testTaxYear: TaxYear = TaxYear(currentYear + 1)

        val taxYearBinder = createSut

        val taxBinderResult = taxYearBinder.bind("", testTaxYear.year.toString)

        taxBinderResult.isRight mustBe true

        taxBinderResult.right map { tbr =>
          tbr mustBe testTaxYear
        }
      }

      "the supplied tax year isn't earlier than cy-4" in {

        val testTaxYear: TaxYear = TaxYear(currentYear - 4)

        val taxYearBinder = createSut

        val taxBinderResult = taxYearBinder.bind("", testTaxYear.year.toString)

        taxBinderResult.isRight mustBe true

        taxBinderResult.right map { tbr =>
          tbr mustBe testTaxYear
        }
      }
    }

    "return a message stating that the tax year cannot be later than cy+1" when {
      "the supplied tax year is later than cy+1" in {

        val testTaxYear: TaxYear = TaxYear(currentYear + 2)

        val taxYearBinder = createSut

        val taxBinderResult = taxYearBinder.bind("", testTaxYear.year.toString)

        taxBinderResult.isLeft mustBe true

        taxBinderResult.left map { tbr =>
          tbr mustBe s"The supplied value '${testTaxYear.year}' is not a currently supported tax year"
        }
      }
    }

    "return a message stating that the tax year cannot be earlier than cy-4" when {
      "the supplied tax year is earlier than cy-4" in {

        val testTaxYear: TaxYear = TaxYear(currentYear - 5)

        val taxYearBinder = createSut

        val taxBinderResult = taxYearBinder.bind("", testTaxYear.year.toString)

        taxBinderResult.isLeft mustBe true

        taxBinderResult.left map { tbr =>
          tbr mustBe s"The supplied value '${testTaxYear.year}' is not a currently supported tax year"
        }
      }
    }

    "return a message stating that the supplied value is not a valid tax year" when {
      "the supplied tax year is later than cy+1" in {

        val nonTaxYear: String = "NonTaxYearValue"

        val taxYearBinder = createSut

        val taxBinderResult = taxYearBinder.bind("taxYear", nonTaxYear)

        taxBinderResult.isLeft mustBe true

        taxBinderResult.left map { tbr =>
          tbr mustBe s"The supplied value '$nonTaxYear' is not a valid tax year"
        }
      }
    }
  }

  "TaxYearObjectBinder - unbind" should {

    "return the tax year as a string" when {
      "a TaxYear object is supplied" in {

        val testTaxYear: TaxYear = TaxYear(currentYear + 1)

        val taxYearBinder = createSut

        val taxBinderResult = taxYearBinder.unbind("", testTaxYear)

        taxBinderResult mustBe testTaxYear.year.toString
      }
    }
  }

  private val currentYear: Int = TaxYear().year

  private def createSut() = TaxYearObjectBinder.taxYearObjectBinder
}
