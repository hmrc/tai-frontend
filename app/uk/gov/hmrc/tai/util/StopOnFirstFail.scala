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

import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

object StopOnFirstFail {

  def apply[T](constraints: Constraint[T]*): Constraint[T] = Constraint { (field: T) =>
    constraints.toList dropWhile (_(field) == Valid) match {
      case Nil             => Valid
      case constraint :: _ => constraint(field)
    }
  }

  def constraint[T](message: String, validator: T => Boolean): Constraint[T] =
    Constraint((data: T) => if (validator(data)) Valid else Invalid(Seq(ValidationError(message))))
}
