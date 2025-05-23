/*
 * Copyright 2025 HM Revenue & Customs
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

import com.google.inject.Inject
import controllers.auth.DataRequest
import play.api.Logging
import play.api.mvc.AnyContent
import uk.gov.hmrc.tai.config.ApplicationConfig

class ApiBackendChoice @Inject() (appConfig: ApplicationConfig) extends Logging {

  def isNewApiBackendEnabled(implicit request: DataRequest[AnyContent]): Boolean = {
    val allowedNinoGroup = appConfig.newApiBulkOnboarding

    val isNinoInAllowedGroup = allowedNinoGroup.contains(
      request.taiUser.nino.withoutSuffix.takeRight(1).toInt
    )

    if (request.getQueryString("newApi").isDefined) {
      logger.info(
        s"The nino ending `${request.taiUser.nino.nino.takeRight(3)}` is using the new API for path ${request.request.path}"
      )
    }

    request.getQueryString("newApi").isDefined || isNinoInAllowedGroup
  }
}
