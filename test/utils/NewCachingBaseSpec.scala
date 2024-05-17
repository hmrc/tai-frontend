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
import controllers.FakeTaiPlayApplication
import controllers.actions.{DataRetrievalAction, IdentifierAction}
import controllers.auth.{AuthJourney, AuthedUser}
import org.jsoup.nodes.Element
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.i18n._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.UserAnswers

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.util.Random

class NewCachingBaseSpec
    extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with I18nSupport with BeforeAndAfterEach {

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId, nino)

  protected def applicationBuilder(userAnswers: UserAnswers = emptyUserAnswers): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[AuthJourney].toInstance(new FakeAuthJourney(userAnswers)),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[JourneyCacheNewRepository].toInstance(mockRepository)
      )
      .configure(additionalConfiguration)

  protected def applicationBuilderWithoutRepository(
    userAnswers: UserAnswers = emptyUserAnswers
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[AuthJourney].toInstance(new FakeAuthJourney(userAnswers)),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
      .configure(additionalConfiguration)

  val userAnswersId: String = "id"

  def inject[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  lazy val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]
  lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]
  lazy val servicesConfig: ServicesConfig = inject[ServicesConfig]
  lazy val langUtils: LanguageUtils = inject[LanguageUtils]
  lazy val mockRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  implicit lazy val messagesApi: MessagesApi = inject[MessagesApi]
  implicit lazy val provider: MessagesProvider = inject[MessagesProvider]
  implicit lazy val lang: Lang = Lang("en")
  implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

  protected val nino: String = new Generator(new Random).nextNino.nino

  implicit val authedUser: AuthedUser = UserBuilder()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  override implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  implicit class ElemUtil(elem: Element) {
    def toStringBreak = elem.toString.replaceAll("&nbsp;", " ")
  }

  implicit class StringUtils(str: String) {
    def replaceU00A0 = str.replace("\u00A0", " ")
    def replaceNbsp = str.replaceAll("&nbsp;", " ")
  }

}
