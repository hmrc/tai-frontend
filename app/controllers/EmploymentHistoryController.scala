package controllers

import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.service.{EmploymentService, TaxCodeChangeService}
import views.html.paye.{HistoricPayAsYouEarnView, RtiDisabledHistoricPayAsYouEarnView}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EmploymentHistoryController@Inject()(
                                            val config: ApplicationConfig,
                                            authenticate: AuthAction,
                                            validatePerson: ValidatePerson,
                                            mcc: MessagesControllerComponents,
                                            implicit val templateRenderer: TemplateRenderer,
                                            errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
  extends TaiBaseController(mcc) {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    ???
  }
}
