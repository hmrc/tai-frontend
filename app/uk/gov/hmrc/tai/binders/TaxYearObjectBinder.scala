/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.mvc.PathBindable
import uk.gov.hmrc.tai.model.TaxYear

object TaxYearObjectBinder {

  implicit def taxYearObjectBinder = new PathBindable[TaxYear] {

    override def bind(key: String, value: String): Either[String, TaxYear] =
      try {

        val taxYearRequired: TaxYear = TaxYear(value)

        val currentYear: Int = TaxYear().year
        val latestSupportedTaxYear = TaxYear().next

        val numberOfPreviousYearsToShow = 5
        val earliestSupportedTaxYear = TaxYear(currentYear - numberOfPreviousYearsToShow)

        if (taxYearRequired >= earliestSupportedTaxYear && taxYearRequired <= latestSupportedTaxYear) {
          Right(taxYearRequired)
        } else {
          Left(s"The supplied value '${taxYearRequired.year}' is not a currently supported tax year")
        }
      } catch {
        case _: Throwable => Left(s"The supplied value '$value' is not a valid tax year")
      }

    override def unbind(key: String, value: TaxYear): String = value.year.toString
  }
}
