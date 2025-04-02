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

package controllers.auth

import play.api.i18n.Messages
import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.tai.model.domain.Person

case class AuthenticatedRequest[A](request: Request[A], taiUser: AuthedUser, person: Person)
    extends WrappedRequest[A](request) {

  def fullName(implicit messages: Messages): String = {
    val defaultName = messages("label.your_account")

    taiUser.trustedHelper match {
      case Some(helper) => helper.principalName
      case None =>
        val trimmed = person.name.trim
        if (trimmed.nonEmpty) trimmed else defaultName
    }
  }
}

case class InternalAuthenticatedRequest[A](request: Request[A], taiUser: AuthedUser) extends WrappedRequest[A](request)
