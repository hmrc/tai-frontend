/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.mvc.{Call, RequestHeader, Result, Results}

object CheckYourAnswersEditMode {
  private val EditParameter = "edit"

  def isEditing(request: RequestHeader): Boolean =
    request.getQueryString(EditParameter).contains("true")

  def preserve(call: Call, request: RequestHeader): Call =
    if (isEditing(request)) {
      val separator = if (call.url.contains("?")) "&" else "?"
      Call(call.method, s"${call.url}$separator$EditParameter=true")
    } else {
      call
    }

  def redirect(call: Call, request: RequestHeader): Result =
    Results.Redirect(preserve(call, request))

  def redirectAfterEdit(request: RequestHeader, checkYourAnswers: Call, continue: Call): Result =
    if (isEditing(request)) Results.Redirect(checkYourAnswers) else redirect(continue, request)
}
