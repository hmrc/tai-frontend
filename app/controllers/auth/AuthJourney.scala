/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import controllers.actions.ValidatePerson
import play.api.mvc.{ActionBuilder, AnyContent, DefaultActionBuilder}

@ImplementedBy(classOf[AuthJourneyImpl])
trait AuthJourney {
  val auth: ActionBuilder[AuthenticatedRequest, AnyContent]
}

class AuthJourneyImpl @Inject() (
  authAction: AuthAction,
  pertaxAuthAction: PertaxAuthAction,
  defaultActionBuilder: DefaultActionBuilder,
  validatePerson: ValidatePerson
) extends AuthJourney {

  val auth: ActionBuilder[AuthenticatedRequest, AnyContent] =
    defaultActionBuilder andThen pertaxAuthAction andThen authAction andThen validatePerson

}
