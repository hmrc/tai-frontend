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

package uk.gov.hmrc.tai.service.yourTaxFreeAmount

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain.TaxCodeChange
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{AllowancesAndDeductionPairs, EmploymentTaxCodeChangeReasons, IabdTaxCodeChangeReasons}

class TaxCodeChangeReasonsServiceSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val iabdTaxCodeChangeReasons = mock[IabdTaxCodeChangeReasons]
  val employmentTaxCodeChangeReasons = mock[EmploymentTaxCodeChangeReasons]

  class TaxCodeChangeReasonsServiceTest() extends TaxCodeChangeReasonsService(
    iabdTaxCodeChangeReasons,
    employmentTaxCodeChangeReasons
  )

  val service = new TaxCodeChangeReasonsServiceTest

  val iabdPairs = mock[AllowancesAndDeductionPairs]
  val taxCodeChange = mock[TaxCodeChange]

  "reasons" must {
    "combine the tax code change reasons" in {
      val iabdReasons = Seq("iabd changed")
      val employmentReasons = Seq("employment changed")

      when(iabdTaxCodeChangeReasons.reasons(any())(any())).thenReturn(iabdReasons)
      when(employmentTaxCodeChangeReasons.reasons(any())(any())).thenReturn(employmentReasons)

      service.combineTaxCodeChangeReasons(iabdPairs, taxCodeChange) mustBe employmentReasons ++ iabdReasons
    }

    "show only unique tax code change reasons" in {
      val someReason = Seq("reason 1", "reason 1")

      when(iabdTaxCodeChangeReasons.reasons(any())(any())).thenReturn(someReason)
      when(employmentTaxCodeChangeReasons.reasons(any())(any())).thenReturn(someReason)

      service.combineTaxCodeChangeReasons(iabdPairs, taxCodeChange) mustBe Seq("reason 1")
    }
  }

  "isAGenericReason" must {
    "be false" when {
      "there are less than or equal to 4 reasons" in {
        val reasons = Seq("reason 1", "reason 2", "reason 3", "reason 4")
        service.isAGenericReason(reasons) mustBe false
      }
    }

    "be true" when {
      "there are zero reasons" in {
        service.isAGenericReason(Seq.empty) mustBe true
      }

      "there are more than 4 reasons" in {
        val reasons = Seq("reason 1", "reason 2", "reason 3", "reason 4", "reason 5")
        service.isAGenericReason(reasons) mustBe true
      }

      "there is a generic reason in the reasons" in {
        val genericReason = messagesApi("taxCode.change.yourTaxCodeChanged.paragraph")
        val reasons = Seq("reason 1", genericReason)
        service.isAGenericReason(reasons) mustBe true
      }
    }
  }

}
