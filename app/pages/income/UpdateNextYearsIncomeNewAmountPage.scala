package pages.income

import pages.QuestionPage
import play.api.libs.json.JsPath
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants


case class UpdateNextYearsIncomeNewAmountPage(employmentId: Int) extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = UpdateNextYearsIncomeConstants.NewAmount

}
