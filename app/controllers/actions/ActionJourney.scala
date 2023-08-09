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

import com.google.inject.ImplementedBy
import controllers.auth.{AuthAction, AuthenticatedRequest, DataRequest}
import play.api.mvc.{ActionBuilder, AnyContent}

import javax.inject.Inject

@ImplementedBy(classOf[ActionJourneyImpl])
trait ActionJourney { // TODO - Needs a better name
  val setJourneyCache: ActionBuilder[DataRequest, AnyContent]
  val authAndValidate: ActionBuilder[AuthenticatedRequest, AnyContent]
}

class ActionJourneyImpl @Inject() (
  authAction: AuthAction,
  validatePerson: ValidatePerson,
  identifierAction: IdentifierAction,
  dataRetrievalAction: DataRetrievalAction
) extends ActionJourney {
  override val setJourneyCache: ActionBuilder[DataRequest, AnyContent] =
    authAction andThen validatePerson andThen identifierAction andThen dataRetrievalAction
  override val authAndValidate: ActionBuilder[AuthenticatedRequest, AnyContent] =
    authAction andThen validatePerson
}
