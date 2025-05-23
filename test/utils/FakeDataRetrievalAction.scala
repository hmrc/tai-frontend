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

import controllers.actions.DataRetrievalAction
import controllers.auth.{DataRequest, IdentifierRequest}
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.Helpers
import uk.gov.hmrc.tai.model.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class FakeDataRetrievalAction(dataToReturn: UserAnswers) extends DataRetrievalAction { // TODO - Delete?

  lazy val messagesApi: MessagesApi = Helpers.stubMessagesApi()

  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  override protected def transform[A](request: IdentifierRequest[A]): Future[DataRequest[A]] =
    Future(
      DataRequest(
        request.request.request,
        request.request.taiUser,
        request.request.fullName,
        dataToReturn
      )
    )

  override protected implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
