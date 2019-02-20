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

package uk.gov.hmrc.tai.service

import controllers.FakeTaiPlayApplication
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation

class ReasonsForTaxCodeChangeServiceSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  val startDate = TaxYear().start
  val previousEmployer = "Previous Employer"
  val currentEmployer = "Current Employer"
  val reasonsService = new ReasonsForTaxCodeChangeService

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  def createTaxRecord(employerName: String): TaxCodeRecord = {
    TaxCodeRecord("taxCode", startDate, startDate.plusMonths(1), OtherBasisOfOperation, employerName, false, Some("12345"), false)
  }

  def createPrimaryTaxRecord(employerName: String): TaxCodeRecord = {
    TaxCodeRecord("taxCode", startDate, startDate.plusMonths(1), OtherBasisOfOperation, employerName, false, Some("12345"), true)
  }

  def removedEmployer(employerName: String): String = {
    messages("tai.taxCodeComparison.removeEmployer", employerName)
  }

  def addedEmployer(employerName: String): String = {
    messages("tai.taxCodeComparison.addEmployer", employerName)
  }

  "reasons taxCodeReasons" must {
    "return empty when nothing has changed" in {
      val taxRecord = Seq(createTaxRecord(previousEmployer), createPrimaryTaxRecord(currentEmployer))
      val taxCodeChange = TaxCodeChange(taxRecord, taxRecord)

      reasonsService.reasons(taxCodeChange) mustBe Seq.empty[String]
    }

    "return a reason when an employment been removed" in {
      val previous = Seq(createTaxRecord(previousEmployer), createTaxRecord(currentEmployer))
      val current = Seq(createTaxRecord(currentEmployer))
      val taxCodeChange = TaxCodeChange(previous, current)

      reasonsService.reasons(taxCodeChange) mustBe Seq(removedEmployer(previousEmployer))
    }

    "return a reason when an employment been added" in {
      val previous = Seq(createTaxRecord(previousEmployer))
      val current = Seq(createTaxRecord(previousEmployer), createTaxRecord(currentEmployer))

      val taxCodeChange = TaxCodeChange(previous, current)

      reasonsService.reasons(taxCodeChange) mustBe Seq(addedEmployer(currentEmployer))
    }

    "return multiple reasons when employments have changed" in {
      val previous = Seq(createTaxRecord(previousEmployer), createTaxRecord(previousEmployer + "1"))
      val current = Seq(createTaxRecord(currentEmployer), createTaxRecord(currentEmployer + "1"))

      val taxCodeChange = TaxCodeChange(previous, current)

      reasonsService.reasons(taxCodeChange) mustBe Seq(
        removedEmployer(previousEmployer), removedEmployer(previousEmployer + "1"),
        addedEmployer(currentEmployer), addedEmployer(currentEmployer + "1")
      )
    }

    "return primary and secondary employment have changed at the same time" in {
      val previous = Seq(createPrimaryTaxRecord(previousEmployer), createTaxRecord(previousEmployer + "1"))
      val current = Seq(createPrimaryTaxRecord(currentEmployer), createTaxRecord(currentEmployer + "1"))

      val taxCodeChange = TaxCodeChange(previous, current)

      reasonsService.reasons(taxCodeChange) mustBe Seq(
        removedEmployer(previousEmployer), addedEmployer(currentEmployer),
        removedEmployer(previousEmployer + "1"), addedEmployer(currentEmployer + "1")
      )
    }

    "return an add and remove message when primary employment has changed" in {
      val previous = Seq(createPrimaryTaxRecord(previousEmployer))
      val current = Seq(createPrimaryTaxRecord(currentEmployer))

      val taxCodeChange = TaxCodeChange(previous, current)

      reasonsService.reasons(taxCodeChange) mustBe Seq(
        removedEmployer(previousEmployer),
        addedEmployer(currentEmployer)
      )
    }

    "if you can match by employer name but can't match with payroll" should {
      "go to the generic message for primary tax records" in {
        val previous = createPrimaryTaxRecord(previousEmployer)
        val current = previous.copy(payrollNumber = Some("54321"))

        val taxCodeChange = TaxCodeChange(Seq(previous), Seq(current))

        reasonsService.reasons(taxCodeChange) mustBe Seq(Messages("taxCode.change.yourTaxCodeChanged.paragraph"))
      }

      "go to the generic message for secondary tax records" in {
        val previous = createTaxRecord(previousEmployer)
        val current = previous.copy(payrollNumber = Some("54321"))

        val taxCodeChange = TaxCodeChange(Seq(previous), Seq(current))

        reasonsService.reasons(taxCodeChange) mustBe Seq(Messages("taxCode.change.yourTaxCodeChanged.paragraph"))
      }
    }
  }
}
