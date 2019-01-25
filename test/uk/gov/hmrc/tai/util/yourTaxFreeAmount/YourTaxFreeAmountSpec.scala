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

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel


class YourTaxFreeAmountSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with YourTaxFreeAmount {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val previousDate = new LocalDate(2017, 12, 12)
  val currentDate = new LocalDate(2018, 6, 5)

  def createYourTaxFreeAmountViewModel(): YourTaxFreeAmountViewModel = {

    val formattedPreviousDate = Dates.formatDate(previousDate)
    val formattedCurrentDate = createFormattedDate(currentDate)

    YourTaxFreeAmountViewModel(
      Some(TaxFreeInfo(formattedPreviousDate, 0, 0)),
      TaxFreeInfo(formattedCurrentDate, 0, 0),
      Seq.empty,
      Seq.empty
    )
  }

  def createFormattedDate(date: LocalDate): String = {
    TaxYearRangeUtil.dynamicDateRange(date, TaxYear().end)
  }

  "buildTaxFreeAmount" should {
    "have the correct date formatting" in {
      val expected = createYourTaxFreeAmountViewModel()

      val previous = CodingComponentsWithCarBenefits(previousDate, Seq.empty, Seq.empty)
      val current = CodingComponentsWithCarBenefits(currentDate, Seq.empty, Seq.empty)
       buildTaxFreeAmount(Some(previous), current, Map.empty) mustBe expected
    }
  }
}
