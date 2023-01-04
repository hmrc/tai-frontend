/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils
import builders.UserBuilder
import controllers.auth.AuthedUser
import controllers.{FakeAuthAction, FakeTaiPlayApplication}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig

import scala.reflect.ClassTag

trait BaseSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with I18nSupport {

  def inject[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  lazy val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]
  lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]
  lazy val servicesConfig: ServicesConfig = inject[ServicesConfig]
  lazy val langUtils: LanguageUtils = inject[LanguageUtils]

  implicit lazy val messagesApi: MessagesApi = inject[MessagesApi]
  implicit lazy val provider: MessagesProvider = inject[MessagesProvider]
  implicit lazy val lang: Lang = Lang("en")
  implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

  val nino: Nino = FakeAuthAction.nino

  implicit val templateRenderer: TemplateRenderer = mocks.MockTemplateRenderer
  implicit val authedUser: AuthedUser = UserBuilder()
  implicit val hc: HeaderCarrier = HeaderCarrier()

}
