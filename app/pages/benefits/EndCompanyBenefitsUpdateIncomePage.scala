package pages.benefits

import pages.QuestionPage
import play.api.libs.json.JsPath
import uk.gov.hmrc.tai.util.constants.TaiConstants


case class EndCompanyBenefitsUpdateIncomePage(empId: Int) extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = TaiConstants.UpdateIncomeConfirmedAmountKey

}