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

package uk.gov.hmrc.tai.util

import play.api.Logger
import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.tai.identifiers.{JrsClaimsId, TypedIdentifier}
import uk.gov.hmrc.tai.model.JrsClaims

case class CachedData(cacheMap: CacheMap) {

  def getJrsClaims: Option[JrsClaims] = get(JrsClaimsId)

  def get[A](identifier: TypedIdentifier[A])(implicit rds: Reads[A]): Option[A] = {

    val entry = cacheMap.getEntry[A](identifier)
    if (entry.isEmpty) {
      logger.debug(s"GET $identifier is empty")
    } else {
      logger.trace(s"GET $identifier: $entry")
    }
    entry
  }

  def set[A](identifier: TypedIdentifier[A], value: A)(implicit writes: Writes[A]): CachedData = {

    logger.trace(s"SET $identifier: $value")
    val updatedAnswers = CachedData(cacheMap copy (data = cacheMap.data + (identifier.toString -> Json.toJson(value))))
    identifier.cleanup(Some(value), updatedAnswers)
  }

  private val logger = Logger(this.getClass)

}

object CachedData {
  def empty(id: String) = new CachedData(new CacheMap(id, Map.empty))
}
