/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.BenefitInKind

class BenefitComponentTypeBinderSpec extends PlaySpec with FakeTaiPlayApplication {

  "BenefitComponentTypeBinder - bind" should {

    "return the BenefitComponentType" when {
      "the supplied value is a valid BenefitComponentType" in {

        val benefitComponentTypeBinder = createSut

        val benefitComponentTypeBinderResult = benefitComponentTypeBinder.bind("", "BenefitInKind")

        benefitComponentTypeBinderResult.isRight mustBe true

        benefitComponentTypeBinderResult.right map { tbr => tbr mustBe BenefitInKind }
      }
    }

    "return a message stating that the value isn't a valid BenefitComponentType" when {
      "the supplied value isn't a valid BenefitComponentType" in {

        val benefitComponentTypeBinder = createSut

        val benefitComponentTypeBinderResult = benefitComponentTypeBinder.bind("", "InvalidComponentType")

        benefitComponentTypeBinderResult.isLeft mustBe true

        benefitComponentTypeBinderResult.left map {
          _ mustBe s"The supplied value 'InvalidComponentType' is not a currently supported Benefit Type"
        }
      }
    }
  }

  "TaxYearObjectBinder - unbind" should {

    "return the tax year as a string" when {
      "a TaxYear object is supplied" in {

        val benefitComponentTypeBinder = createSut

        val benefitComponentTypeBinderResult = benefitComponentTypeBinder.unbind("", BenefitInKind)

        benefitComponentTypeBinderResult mustBe BenefitInKind.toString
      }
    }
  }

  private def createSut() = BenefitComponentTypeBinder.benefitComponentTypeBinder
}
