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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import controllers.FakeTaiPlayApplication
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}

class TaxCodeChangeReasonsSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  val startDate = TaxYear().start
  val previousEmployer = "Previous Employer"
  val currentEmployer = "Current Employer"
  val employmentTaxCodeChangeReasons = new TaxCodeChangeReasons

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  def createTaxRecord(employerName: String): TaxCodeRecord =
    TaxCodeRecord(
      "taxCode",
      startDate,
      startDate.plusMonths(1),
      OtherBasisOfOperation,
      employerName,
      false,
      Some("12345"),
      false)

  def createPrimaryTaxRecord(employerName: String): TaxCodeRecord =
    TaxCodeRecord(
      "taxCode",
      startDate,
      startDate.plusMonths(1),
      OtherBasisOfOperation,
      employerName,
      false,
      Some("12345"),
      true)

  def createPensionTaxRecord(employerName: String): TaxCodeRecord =
    TaxCodeRecord(
      "taxCode",
      startDate,
      startDate.plusMonths(1),
      OtherBasisOfOperation,
      employerName,
      true,
      Some("12345"),
      false)

  def createPrimaryPensionTaxRecord(employerName: String): TaxCodeRecord =
    TaxCodeRecord(
      "taxCode",
      startDate,
      startDate.plusMonths(1),
      OtherBasisOfOperation,
      employerName,
      true,
      Some("12345"),
      true)

  def removedEmployer(employerName: String): String =
    messages("tai.taxCodeComparison.removeEmployer", employerName)

  def addedEmployer(employerName: String): String =
    messages("tai.taxCodeComparison.add.employer", employerName)

  def addedPension(employerName: String): String =
    messages("tai.taxCodeComparison.add.pension", employerName)

  def addSingleEmploymentCount: String =
    messages("tai.taxCodeComparison.employment.count", 1)

  def addMultipleEmploymentCount(count: Int): String =
    messages("tai.taxCodeComparison.employments.count", count)

  "reasons taxCodeReasons" when {
    "employment has changed" must {
      "return empty when nothing has changed" in {
        val taxRecord = Seq(createTaxRecord(previousEmployer), createPrimaryTaxRecord(currentEmployer))
        val taxCodeChange = TaxCodeChange(taxRecord, taxRecord)

        employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq.empty[String]
      }

      "return a reason when an employment been removed" in {
        val previous = Seq(createTaxRecord(previousEmployer), createTaxRecord(currentEmployer))
        val current = Seq(createTaxRecord(currentEmployer))
        val taxCodeChange = TaxCodeChange(previous, current)

        employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
          removedEmployer(previousEmployer),
          addSingleEmploymentCount)
      }

      "return a reason when an employment been added" in {
        val previous = Seq(createTaxRecord(previousEmployer))
        val current = Seq(createTaxRecord(previousEmployer), createTaxRecord(currentEmployer))

        val taxCodeChange = TaxCodeChange(previous, current)

        employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
          addedEmployer(currentEmployer),
          addMultipleEmploymentCount(2))
      }

      "return multiple reasons when employments have changed" in {
        val previous = Seq(createTaxRecord(previousEmployer), createTaxRecord(previousEmployer + "1"))
        val current = Seq(createTaxRecord(currentEmployer), createTaxRecord(currentEmployer + "1"))

        val taxCodeChange = TaxCodeChange(previous, current)

        employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
          removedEmployer(previousEmployer),
          removedEmployer(previousEmployer + "1"),
          addedEmployer(currentEmployer),
          addedEmployer(currentEmployer + "1"),
          addMultipleEmploymentCount(2)
        )
      }

      "return primary and secondary employment have changed at the same time" in {
        val previous = Seq(createPrimaryTaxRecord(previousEmployer), createTaxRecord(previousEmployer + "1"))
        val current = Seq(createPrimaryTaxRecord(currentEmployer), createTaxRecord(currentEmployer + "1"))

        val taxCodeChange = TaxCodeChange(previous, current)

        employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
          removedEmployer(previousEmployer),
          addedEmployer(currentEmployer),
          removedEmployer(previousEmployer + "1"),
          addedEmployer(currentEmployer + "1"),
          addMultipleEmploymentCount(2)
        )
      }

      "return an add and remove message when primary employment has changed" in {
        val previous = Seq(createPrimaryTaxRecord(previousEmployer))
        val current = Seq(createPrimaryTaxRecord(currentEmployer))

        val taxCodeChange = TaxCodeChange(previous, current)

        employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
          removedEmployer(previousEmployer),
          addedEmployer(currentEmployer),
          addSingleEmploymentCount
        )
      }

      "if you can match by employer name but can't match with payroll" should {
        "go to the generic message for primary tax records" in {
          val previous = createPrimaryTaxRecord(previousEmployer)
          val current = previous.copy(payrollNumber = Some("54321"))

          val taxCodeChange = TaxCodeChange(Seq(previous), Seq(current))

          employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
            Messages("taxCode.change.yourTaxCodeChanged.paragraph"))
        }

        "go to the generic message for secondary tax records" in {
          val previous = createTaxRecord(previousEmployer)
          val current = previous.copy(payrollNumber = Some("54321"))

          val taxCodeChange = TaxCodeChange(Seq(previous), Seq(current))

          employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
            Messages("taxCode.change.yourTaxCodeChanged.paragraph"))
        }
      }
    }

    "pensions has changed" must {
      "return a reason when primary has changed to a primary pension" in {
        val previous = Seq(createPrimaryTaxRecord(previousEmployer))
        val current = Seq(createPrimaryPensionTaxRecord(currentEmployer))

        val taxCodeChange = TaxCodeChange(previous, current)

        employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
          removedEmployer(previousEmployer),
          addedPension(currentEmployer),
          messages("tai.taxCodeComparison.pension.count", 1))
      }

      "return a reason when a secondary pension been added" in {
        val previous = Seq(createPensionTaxRecord(previousEmployer))
        val current = Seq(createPensionTaxRecord(previousEmployer), createPensionTaxRecord(currentEmployer))

        val taxCodeChange = TaxCodeChange(previous, current)

        employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
          addedPension(currentEmployer),
          messages("tai.taxCodeComparison.pensions.count", 2))
      }
    }

    "current employment(s) and pension(s) exist" must {
      "return how many current income sources there are" in {
        val previous = Seq(createPrimaryTaxRecord(previousEmployer))
        val current = Seq(createPrimaryPensionTaxRecord(currentEmployer), createTaxRecord(currentEmployer + "1"))

        val taxCodeChange = TaxCodeChange(previous, current)

        employmentTaxCodeChangeReasons.reasons(taxCodeChange) mustBe Seq(
          removedEmployer(previousEmployer),
          addedPension(currentEmployer),
          addedEmployer(currentEmployer + "1"),
          messages("tai.taxCodeComparison.incomeSources.count", 2)
        )
      }
    }
  }
}
