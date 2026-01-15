/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.tai.model.domain.IabdDetails
import utils.BaseSpec

import java.time.LocalDate

class TaxAccountHelperSpec extends BaseSpec {

  private def call(
    iabds: Seq[IabdDetails],
    taxDate: Option[LocalDate] = None,
    empId: Option[Int] = None
  ): Option[BigDecimal] =
    TaxAccountHelper.getIabdLatestEstimatedIncome(iabds, taxDate, empId)

  private val estimatedPayCode = IabdDetails.newEstimatedPayCode

  "getIabdLatestEstimatedIncome" must {

    "return None" when {

      "no IABDs are provided" in {
        call(Seq.empty) mustBe None
      }

      "no IABD has the new estimated pay type" in {
        val iabds = Seq(
          IabdDetails(
            employmentSequenceNumber = Some(1),
            source = None,
            `type` = Some(999),
            receiptDate = None,
            captureDate = Some(LocalDate.now),
            grossAmount = Some(BigDecimal(1000))
          )
        )

        call(iabds) mustBe None
      }

      "all matching IABDs are before the tax account date" in {
        val taxDate = LocalDate.of(2024, 3, 1)

        val iabds = Seq(
          IabdDetails(
            employmentSequenceNumber = Some(1),
            source = None,
            `type` = Some(estimatedPayCode),
            receiptDate = None,
            captureDate = Some(LocalDate.of(2024, 2, 1)),
            grossAmount = Some(BigDecimal(20000))
          )
        )

        call(iabds, Some(taxDate), Some(1)) mustBe None
      }

      "the IABD has no gross amount" in {
        val iabds = Seq(
          IabdDetails(
            employmentSequenceNumber = Some(1),
            source = None,
            `type` = Some(estimatedPayCode),
            receiptDate = None,
            captureDate = Some(LocalDate.now),
            grossAmount = None
          )
        )

        call(iabds) mustBe None
      }
    }

    "return the latest estimated income" when {

      "multiple estimated pay IABDs exist after the tax account date" in {
        val taxDate = LocalDate.of(2024, 1, 1)

        val iabds = Seq(
          IabdDetails(
            employmentSequenceNumber = Some(1),
            source = None,
            `type` = Some(estimatedPayCode),
            receiptDate = None,
            captureDate = Some(LocalDate.of(2024, 2, 1)),
            grossAmount = Some(BigDecimal(20000))
          ),
          IabdDetails(
            employmentSequenceNumber = Some(1),
            source = None,
            `type` = Some(estimatedPayCode),
            receiptDate = None,
            captureDate = Some(LocalDate.of(2024, 3, 1)),
            grossAmount = Some(BigDecimal(25000))
          )
        )

        call(iabds, Some(taxDate), Some(1)) mustBe Some(BigDecimal(25000))
      }

      "employment id is not provided" in {
        val iabds = Seq(
          IabdDetails(
            employmentSequenceNumber = Some(1),
            source = None,
            `type` = Some(estimatedPayCode),
            receiptDate = None,
            captureDate = Some(LocalDate.now.minusDays(1)),
            grossAmount = Some(BigDecimal(20000))
          ),
          IabdDetails(
            employmentSequenceNumber = Some(2),
            source = None,
            `type` = Some(estimatedPayCode),
            receiptDate = None,
            captureDate = Some(LocalDate.now),
            grossAmount = Some(BigDecimal(30000))
          )
        )

        call(iabds) mustBe Some(BigDecimal(30000))
      }
    }

    "filter by employment id" when {

      "an employment id is provided" in {
        val iabds = Seq(
          IabdDetails(
            employmentSequenceNumber = Some(1),
            source = None,
            `type` = Some(estimatedPayCode),
            receiptDate = None,
            captureDate = Some(LocalDate.now),
            grossAmount = Some(BigDecimal(20000))
          ),
          IabdDetails(
            employmentSequenceNumber = Some(2),
            source = None,
            `type` = Some(estimatedPayCode),
            receiptDate = None,
            captureDate = Some(LocalDate.now),
            grossAmount = Some(BigDecimal(30000))
          )
        )

        call(iabds, None, Some(2)) mustBe Some(BigDecimal(30000))
      }
    }

    "use a fallback tax account date" when {

      "the tax account date is missing" in {
        val recentDate = LocalDate.now.minusDays(10)

        val iabds = Seq(
          IabdDetails(
            employmentSequenceNumber = Some(1),
            source = None,
            `type` = Some(estimatedPayCode),
            receiptDate = None,
            captureDate = Some(recentDate),
            grossAmount = Some(BigDecimal(18000))
          )
        )

        call(iabds, None, Some(1)) mustBe Some(BigDecimal(18000))
      }
    }
  }
}
