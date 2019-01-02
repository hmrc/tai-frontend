/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.Json
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import uk.gov.hmrc.tai.config.WSHttp
import uk.gov.hmrc.tai.model.Activity

object ActivityLoggerConnector extends ActivityLoggerConnector with ServicesConfig {
  override lazy val activityLoggerBaseUrl = baseUrl("activity-logger")
  override lazy val http = WSHttp
}

trait ActivityLoggerConnector {
  lazy val activityLoggerBaseUrl : String = ???
  lazy val http: HttpPost with HttpGet = ???


  def logActivity(activity: Activity, loggedUserNino: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val jsonRequest = Json.toJson(activity)
    http.POST(s"$activityLoggerBaseUrl/activity-logger/activities/log", jsonRequest)
  }
}
