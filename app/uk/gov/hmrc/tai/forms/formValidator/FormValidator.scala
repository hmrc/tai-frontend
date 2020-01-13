/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.forms.formValidator

import play.api.data.FormError

trait FormValidator {

  def validate[T](x: T, validations: Seq[((T) => Boolean, String)], formErrorKey: String): Seq[FormError] =
    validations.foldLeft(Seq.empty[FormError]) { (errors, validator) =>
      if (validator._1(x)) {
        errors
      } else {
        errors :+ FormError(formErrorKey, validator._2)
      }

    }
}

object FormValidator extends FormValidator
