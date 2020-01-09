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

package uk.gov.hmrc.tai.binders

import play.api.mvc.PathBindable
import uk.gov.hmrc.tai.model.domain.BenefitComponentType

object BenefitComponentTypeBinder {

  implicit def benefitComponentTypeBinder = new PathBindable[BenefitComponentType] {

    override def bind(key: String, value: String): Either[String, BenefitComponentType] =
      BenefitComponentType(value) map {
        Right(_)
      } getOrElse {
        Left(s"The supplied value '$value' is not a currently supported Benefit Type")
      }

    override def unbind(key: String, value: BenefitComponentType) = value.toString
  }
}
