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
import builders.{RequestBuilder, UserBuilder}
import controllers.auth.{AuthJourney, AuthedUser, AuthenticatedRequest, InternalAuthenticatedRequest}
import controllers.{FakeAuthRetrievals, FakeTaiPlayApplication}
import org.jsoup.nodes.Element
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.UserAnswers

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait BaseSpec
    extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with I18nSupport with BeforeAndAfterEach
    with ScalaFutures {

  def inject[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  lazy val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]
  lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]
  lazy val servicesConfig: ServicesConfig = inject[ServicesConfig]
  lazy val langUtils: LanguageUtils = inject[LanguageUtils]

  implicit lazy val messagesApi: MessagesApi = inject[MessagesApi]
  implicit lazy val provider: MessagesProvider = inject[MessagesProvider]
  implicit lazy val lang: Lang = Lang("en")
  implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

  protected lazy val mockAuthJourney: AuthJourney = mock[AuthJourney]

  val nino: Nino = FakeAuthRetrievals.nino

  val userAnswers: UserAnswers = UserAnswers(
    RequestBuilder.uuid,
    nino.nino,
    Json.obj(
      "end-employment-employmentId" -> 1
    )
  )

  protected implicit val authedUser: AuthedUser = UserBuilder()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  override implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  implicit class ElemUtil(elem: Element) {
    def toStringBreak = elem.toString.replaceAll("&nbsp;", " ")
  }

  implicit class StringUtils(str: String) {
    def replaceU00A0 = str.replace("\u00A0", " ")
    def replaceNbsp = str.replaceAll("&nbsp;", " ")
  }

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagService)
    reset(mockAuthJourney)

    when(mockAuthJourney.authWithValidatePerson).thenReturn(new ActionBuilder[AuthenticatedRequest, AnyContent] {
      private val user =
        AuthedUser(
          Nino(nino.toString()),
          Some("saUtr"),
          None
        )
      override def invokeBlock[A](
        request: Request[A],
        block: AuthenticatedRequest[A] => Future[Result]
      ): Future[Result] =
        block(AuthenticatedRequest(request, user, fakePerson(nino)))

      override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = ec
    })

    when(mockAuthJourney.authWithoutValidatePerson).thenReturn(
      new ActionBuilder[InternalAuthenticatedRequest, AnyContent] {
        private val user =
          AuthedUser(
            Nino(nino.toString()),
            Some("saUtr"),
            None
          )

        override def invokeBlock[A](
          request: Request[A],
          block: InternalAuthenticatedRequest[A] => Future[Result]
        ): Future[Result] =
          block(InternalAuthenticatedRequest(request, user))

        override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

        override protected def executionContext: ExecutionContext = ec
      }
    )

  }

}
