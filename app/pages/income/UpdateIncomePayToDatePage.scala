package pages.income

import pages.QuestionPage
import play.api.libs.json.JsPath
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateIncomeConstants

object UpdateIncomePayToDatePage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = UpdateIncomeConstants.PayToDateKey

}
