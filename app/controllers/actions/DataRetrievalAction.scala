/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.auth.{DataRequest, IdentifierRequest}
import play.api.mvc.ActionTransformer
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.model.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject() (
  val journeyCacheRepository: JourneyCacheRepository
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction {

  override protected def transform[A](request: IdentifierRequest[A]): Future[DataRequest[A]] = {
    val nino: String = (request.request.taiUser.nino, request.request.taiUser.trustedHelper) match {
      case (thisUserNino, None)     => thisUserNino.nino
      case (thisUserNino, Some(th)) => th.principalNino.getOrElse(thisUserNino.nino)
    }

    journeyCacheRepository
      .get(request.userId, nino)
      .map {
        _.fold(
          DataRequest(
            request.request,
            request.request.taiUser,
            request.request.fullName,
            UserAnswers(request.userId, nino)
          )
        )(
          DataRequest(request.request, request.request.taiUser, request.request.fullName, _)
        )
      }
  }
}

trait DataRetrievalAction extends ActionTransformer[IdentifierRequest, DataRequest]
