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

package uk.gov.hmrc.tai.connectors

import com.google.inject.{ImplementedBy, Inject}
import controllers.auth.OptionalDataRequest
import play.api.libs.json.Writes
import repositories.SessionRepository
import uk.gov.hmrc.tai.identifiers.TypedIdentifier
import uk.gov.hmrc.tai.util.CachedData

import scala.concurrent.{ExecutionContext, Future}

class DataCacheConnectorImpl @Inject()(sessionRepository: SessionRepository)(implicit ec: ExecutionContext)
    extends DataCacheConnector {

  override def save(cachedData: CachedData): Future[CachedData] =
    sessionRepository.upsert(cachedData.cacheMap).map(_ => cachedData)

  override def remove(cacheId: String): Future[Boolean] =
    sessionRepository.remove(cacheId)

  override def fetch(cacheId: String): Future[Option[CachedData]] =
    sessionRepository.get(cacheId).map(_.map(CachedData.apply))
}

@ImplementedBy(classOf[DataCacheConnectorImpl])
trait DataCacheConnector {

  def save[A, B](request: OptionalDataRequest[A], key: TypedIdentifier[B], value: B)(
    implicit writes: Writes[B]): Future[CachedData] = {
    val answer = request.cachedData getOrElse CachedData.empty(request.cacheId)

    save(answer.set(key, value))
  }

  def save(cachedData: CachedData): Future[CachedData]

  def remove(cacheId: String): Future[Boolean]

  def fetch(cacheId: String): Future[Option[CachedData]]
}
