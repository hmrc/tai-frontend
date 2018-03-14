package controllers

import builders.AuthBuilder
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.MessagesApi
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.service.{AuditService, CodingComponentService, TaiService, TaxAccountService}
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.gov.hmrc.tai.model.TaiRoot

import scala.concurrent.Future

class PotentialUnderpaymentControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val nino = new Generator().nextNino

  "potentialUnderpaymentPage method" must {
    "return the potentional underpayment page" when {
      "supplied with an authorised session" in {

      }
    }
  }


  private class SUT() extends PotentialUnderpaymentController {

    override def taiService: TaiService = mock[TaiService]

    override def codingComponentService: CodingComponentService = mock[CodingComponentService]

    override def taxAccountService: TaxAccountService = mock[TaxAccountService]

    override def auditService: AuditService = mock[AuditService]

    override protected def delegationConnector: DelegationConnector = mock[DelegationConnector]

    override def auditConnector: AuditConnector = mock[AuditConnector]

    override implicit def templateRenderer: TemplateRenderer = mock[TemplateRenderer]

    override implicit def partialRetriever: PartialRetriever = mock[PartialRetriever]

    override protected def authConnector: AuthConnector = mock[AuthConnector]

    val authority = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(authority)
    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(TaiRoot("", 1, "", "", None, "", "", false, None)))
  }
}
