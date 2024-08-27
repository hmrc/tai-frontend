package pages.income

import pages.QuestionPage
import play.api.libs.json.JsPath
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateIncomeConstants

object UpdateIncomePayPeriodPage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = UpdateIncomeConstants.PayPeriodKey

}
