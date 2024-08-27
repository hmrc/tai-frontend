package pages

import play.api.libs.json.JsPath
import uk.gov.hmrc.tai.util.constants.journeyCache.TrackSuccessfulJourneyConstants

object TrackingJourneyConstantsEstimatedPayPage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = TrackSuccessfulJourneyConstants.EstimatedPayKey

}
