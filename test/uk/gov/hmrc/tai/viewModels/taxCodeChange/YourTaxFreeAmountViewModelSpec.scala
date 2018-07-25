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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.time.TaxYearResolver

/**
  * Created by digital032748 on 25/07/18.
  */
class YourTaxFreeAmountViewModelSpec extends PlaySpec with FakeTaiPlayApplication with ViewModelHelper with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "YourTaxFreeAmountViewModel" must {

    "return a range of dates as a formatted string" in {

      val p2IssueDate = new LocalDate()
      val viewModel = YourTaxFreeAmountViewModel(p2IssueDate)
      val expectedDateRange = messagesApi("tai.taxYear",htmlNonBroken(Dates.formatDate(p2IssueDate)),
        htmlNonBroken(Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)))

      viewModel.taxCodeDateRange mustBe expectedDateRange
    }

  }

}
