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

package controllers.actions

import cats.data.EitherT
import com.google.inject.ImplementedBy
import controllers.auth.{AuthenticatedRequest, InternalAuthenticatedRequest}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tai.model.admin.DesignatoryDetailsCheck
import uk.gov.hmrc.tai.model.domain.Address.emptyAddress
import uk.gov.hmrc.tai.model.domain.{Address, Person}
import uk.gov.hmrc.tai.service.PersonService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ValidatePersonImpl])
trait ValidatePerson extends ActionRefiner[InternalAuthenticatedRequest, AuthenticatedRequest]

@Singleton
class ValidatePersonImpl @Inject() (
  personService: PersonService,
  val messagesApi: MessagesApi,
  featureFlagService: FeatureFlagService
)(implicit ec: ExecutionContext)
    extends ValidatePerson with I18nSupport {

  override protected def refine[A](
    request: InternalAuthenticatedRequest[A]
  ): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val personNino = request.taiUser.nino
    val person =
      featureFlagService.get(DesignatoryDetailsCheck).flatMap { ff =>
        if (ff.isEnabled) {
          personService.personDetails(personNino).value
        } else {
          Future.successful(
            Right[UpstreamErrorResponse, Person](
              Person(
                nino = request.taiUser.nino,
                firstName = request.taiUser.firstName.getOrElse(""),
                surname = request.taiUser.lastName.getOrElse(""),
                isDeceased = false,
                address = emptyAddress
              )
            )
          )
        }
      }

    // TODO: PertaxAuthAction is already checking MCI_RECORD. isDeceased check can also be removed once DDCNL-8734 is complete
    EitherT(person).transform {
      case Right(person) => Right(AuthenticatedRequest(request, request.taiUser.toAuthedUser, person))
      case Left(_) =>
        Right(
          AuthenticatedRequest(
            request,
            request.taiUser.toAuthedUser,
            Person(
              nino = personNino,
              firstName = request.taiUser.firstName.getOrElse(""),
              surname = request.taiUser.lastName.getOrElse(""),
              isDeceased = false,
              address = Address(None, None, None, None, None)
            )
          )
        )
    }.value
  }
  override protected def executionContext: ExecutionContext = ec
}
