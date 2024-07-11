package pages.benefits

import pages.QuestionPage
import play.api.libs.json.JsPath
import uk.gov.hmrc.tai.util.constants.journeyCache.EndCompanyBenefitConstants


case object EndCompanyBenefitsStopDatePage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = EndCompanyBenefitConstants.BenefitStopDateKey

}