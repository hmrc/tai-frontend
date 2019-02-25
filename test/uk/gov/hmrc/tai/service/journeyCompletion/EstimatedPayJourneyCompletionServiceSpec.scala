package uk.gov.hmrc.tai.service.journeyCompletion

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.connectors.JourneyCacheConnector

class EstimatedPayJourneyCompletionServiceSpec extends PlaySpec with MockitoSugar {

  private def createTestService = new EstimatedPayJourneyCompletionServiceTest

  val journeyCacheConnector: JourneyCacheConnector = mock[JourneyCacheConnector]

  private class EstimatedPayJourneyCompletionServiceTest extends EstimatedPayJourneyCompletionServiceSpec(
    journeyCacheConnector
  )


  "Estimated Pay Journey Completed Service" must {

    "add a cache entry upon successful completion of a journey" in {



    }

  }

}
