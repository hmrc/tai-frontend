/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.benefit

import java.util.Locale

import controllers.routes
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Play.current
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants


case class CompanyCarCheckAnswersViewModel(carModel: String,
                                           carProvider: String,
                                           dateGivenBack: String,
                                           dateFuelBenefitStopped: String = "",
                                           taxYearStart: String,
                                           taxYearEnd: String
                                          ) {
  lazy val showFuelBenefits: Boolean = !dateFuelBenefitStopped.isEmpty

  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {

    val line1 = Seq(
      CheckYourAnswersConfirmationLine(
        Messages("tai.companyCar.checkAnswers.table.rowOne.description"),
        dateGivenBack,
        routes.CompanyCarController.getCompanyCarEndDate.url)
    )

    if(showFuelBenefits) {
      line1 :+ CheckYourAnswersConfirmationLine(
        Messages("tai.companyCar.checkAnswers.table.rowTwo.description"),
        dateFuelBenefitStopped,
        routes.CompanyCarController.getFuelBenefitEndDate.url)
    } else {
      line1
    }
  }
}

object CompanyCarCheckAnswersViewModel extends JourneyCacheConstants {

  def apply(cacheMap: Map[String, String], taxYear: TaxYear)(implicit messages: Messages): CompanyCarCheckAnswersViewModel = {

    def convertToDatePatternWithMonthAsLetters(date: String): String = {
      Dates.formatDate(new LocalDate(date))
    }

    val viewModel = for {
      model <- cacheMap.get(CompanyCar_CarModelKey)
      provider <- cacheMap.get(CompanyCar_CarProviderKey)
      dateGivenBack <- cacheMap.get(CompanyCar_DateGivenBackKey)
    } yield CompanyCarCheckAnswersViewModel(
      model,
      provider,
      convertToDatePatternWithMonthAsLetters(dateGivenBack),
      cacheMap.get(CompanyCar_DateFuelBenefitStoppedKey).map(x => convertToDatePatternWithMonthAsLetters(x)).getOrElse(""),
      taxYear.start.getYear.toString,
      taxYear.end.getYear.toString)

    viewModel.getOrElse(throw new RuntimeException("Could not create CompanyCarCheckAnswersViewModel from cache with missing values"))
  }
}
