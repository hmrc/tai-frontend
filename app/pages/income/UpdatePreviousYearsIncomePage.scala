package pages.income

import pages.QuestionPage
import play.api.libs.json.JsPath
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdatePreviousYearsIncomeConstants

object UpdatePreviousYearsIncomePage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = UpdatePreviousYearsIncomeConstants.IncomeDetailsKey

}
