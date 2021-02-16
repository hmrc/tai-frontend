/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Inject
import controllers.auth.{AuthenticatedRequest, OptionalDataRequest}
import javax.inject.Singleton
import play.api.mvc.ActionTransformer
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.tai.connectors.DataCacheConnector

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataRetrievalActionProvider @Inject()(val dataCacheConnector: DataCacheConnector)(implicit ec: ExecutionContext) {

  def getData(): DataRetrievalAction =
    new DataRetrievalActionImpl(dataCacheConnector)
}

private[actions] class DataRetrievalActionImpl(
  val dataCacheConnector: DataCacheConnector
)(implicit ec: ExecutionContext)
    extends DataRetrievalAction {

  override protected def transform[A](request: AuthenticatedRequest[A]): Future[OptionalDataRequest[A]] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    val id = request.cacheId

    dataCacheConnector.fetch(id).map { data =>
      OptionalDataRequest(request, id, data)
    }
  }

  override protected def executionContext: ExecutionContext = ec
}

private[actions] trait DataRetrievalAction extends ActionTransformer[AuthenticatedRequest, OptionalDataRequest]
