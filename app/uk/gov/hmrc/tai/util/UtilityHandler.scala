package uk.gov.hmrc.tai.util

import com.google.inject.Inject
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.tai.config.ApplicationConfig
import views.html.InternalServerErrorView

import scala.concurrent.ExecutionContext

class UtilityHandler @Inject() ( // TODO - DELETE?
                                 internalServerErrorView: InternalServerErrorView,
                                 mcc: MessagesControllerComponents,
                                 appConfig: ApplicationConfig
                               )(implicit ec: ExecutionContext)
  extends Logging
    with I18nSupport {

  def errorToResponse()(implicit request: Request[_]): Result =
    InternalServerError(internalServerErrorView(appConfig))

  override def messagesApi: MessagesApi = mcc.messagesApi
}
