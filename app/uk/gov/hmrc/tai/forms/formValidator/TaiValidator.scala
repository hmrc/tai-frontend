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

package uk.gov.hmrc.tai.forms.formValidator

import play.api.data.Forms._
import play.api.data.Mapping
import play.api.i18n.Messages
import org.joda.time.LocalDate

/**
  * Created by user02 on 6/9/14.
  */
object TaiValidator extends BaseValidator {

  def validateOptionalDate(): Mapping[Option[LocalDate]] =
    dateTuple(true)
}
