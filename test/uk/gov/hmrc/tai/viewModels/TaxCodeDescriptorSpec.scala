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

package uk.gov.hmrc.tai.viewModels

import builders.AuthBuilder
import controllers.FakeTaiPlayApplication
import controllers.i18n.TaiLanguageController
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar._
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service.PersonService
import uk.gov.hmrc.urls.Link

import scala.collection.immutable.ListMap
import scala.concurrent.Future

class TaxCodeDescriptorSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "standAloneTaxCodeExplanation" must {
    "return the correct explanation" when {
      Seq("0T", "BR", "NT").foreach(taxCode => {
        s"tax code is ${taxCode}" in {
          TaxCodeDescriptorConcrete.describeTaxCode(taxCode, OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
            ListMap(taxCode -> Messages(s"tai.taxCode.${taxCode}"))
        }
      })
    }
  }

  "scottish standAloneTaxCodeExplanation without defined scottish bands" must {
    "return the correct explanation" when {
      Seq("D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8").foreach(taxCode => {
        s"tax code is ${taxCode}" in {
          TaxCodeDescriptorConcrete.describeTaxCode(taxCode, OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
            ListMap(taxCode -> Messages(s"tai.taxCode.DX", 0))
        }
      })
    }
  }

  "scottish standAloneTaxCodeExplanation with defined scottish bands" must {
    "return the correct explanation" when {
      val scottishTaxBands = Map(
        "D1" -> BigDecimal(10),
        "D2" -> BigDecimal(20),
        "D3" -> BigDecimal(30),
        "D4" -> BigDecimal(40),
        "D5" -> BigDecimal(50),
        "D6" -> BigDecimal(60),
        "D7" -> BigDecimal(70),
        "D8" -> BigDecimal(80)
      )
      Seq("D1", "D2", "D3", "D4", "D5", "D6","D7", "D8").foreach(taxCode => {
        s"tax code is ${taxCode}" in {
          TaxCodeDescriptorConcrete.describeTaxCode(taxCode, OtherBasisOfOperation, scottishTaxBands) mustBe
            ListMap(taxCode -> Messages(s"tai.taxCode.DX", scottishTaxBands(taxCode)))
        }
      })
    }
  }

  "scottishTaxCodeExplanation" must {
    "return the correct explanation" when {
      "tax code is prefixed with a S" in {
        TaxCodeDescriptorConcrete.describeTaxCode("S1", OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
          ListMap("S" -> Messages(s"tai.taxCode.S",
            Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value=Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml))
      }
    }
  }

  "welshTaxCodeExplanation" must {
    "return the correct explanation" when {
      "tax code is prefixed with a C with a link to the english tax explanation page  when user is viewing page in English" in {
        TaxCodeDescriptorConcrete.describeTaxCode("C1", OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
          ListMap("C" -> Messages(s"tai.taxCode.C",
            Link.toExternalPage(url = ApplicationConfig.welshRateIncomeTaxUrl, value=Some(Messages("tai.taxCode.welshIncomeText.link"))).toHtml))
      }

      "tax code is prefixed with a C with a link to the welsh tax explanation page  when user is viewing page in Welsh" in {
        TaxCodeDescriptorConcrete.describeTaxCode("C1", OtherBasisOfOperation, Map.empty[String, BigDecimal])(Messages.apply(Lang("cy"), messagesApi)) mustBe
          ListMap("C" -> Messages.apply(Lang("cy"), messagesApi)(s"tai.taxCode.C",
            Link.toExternalPage(url = ApplicationConfig.welshRateIncomeTaxWelshUrl, value=Some(Messages.apply(Lang("cy"), messagesApi)("tai.taxCode.welshIncomeText.link"))).toHtml))
      }
    }
  }

  "untaxedTaxCodeExplanation" must {
    "return the correct explanation" when {
      "tax code is prefixed with a K" in {
        TaxCodeDescriptorConcrete.describeTaxCode("K10", OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
          ListMap("K" -> Messages(s"tai.taxCode.K"),
            10.toString -> Messages(s"tai.taxCode.untaxedAmount", 100))
      }
    }
  }

  "suffixTaxCodeExplanation" must {
    "return the correct explanation" when {
      Seq("L", "M", "0N").foreach(taxCode => {
        s"tax code ${taxCode} has suffix ${taxCode.last}" in {
          TaxCodeDescriptorConcrete.describeTaxCode(taxCode, OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
            ListMap("0" -> Messages(s"tai.taxCode.amount", 0),
              taxCode.last.toString -> Messages(s"tai.taxCode.${taxCode.last.toString}"))
        }
      })

      s"tax code 10N has suffix N" in {
        TaxCodeDescriptorConcrete.describeTaxCode("10N", OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
          ListMap("10" -> Messages(s"tai.taxCode.amount", 100),
            "N" -> Messages(s"tai.taxCode.N"))
      }

      s"tax code 01T has suffix T" in {
        TaxCodeDescriptorConcrete.describeTaxCode("01T", OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
          ListMap("1" -> Messages(s"tai.taxCode.amount", 10),
            "T" -> Messages(s"tai.taxCode.T"))
      }

      s"tax code 10T has suffix T" in {
        TaxCodeDescriptorConcrete.describeTaxCode("10T", OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
          ListMap("10" -> Messages(s"tai.taxCode.amount", 100),
            "T" -> Messages(s"tai.taxCode.T"))
      }
    }
  }

  "emergencyTaxCodeExplanation" must {
    "return the correct explanation" when {
      "on an emergency basisOperation" in {
        TaxCodeDescriptorConcrete.describeTaxCode("10T", Week1Month1BasisOfOperation, Map.empty[String, BigDecimal]) mustBe
          ListMap(
            "10" -> Messages(s"tai.taxCode.amount", 100),
            "T" -> Messages(s"tai.taxCode.T"),
            "X" -> Messages("tai.taxCode.X")
          )
      }

      "not on an emergency basisOperation" in {
        TaxCodeDescriptorConcrete.describeTaxCode("10T", OtherBasisOfOperation, Map.empty[String, BigDecimal]) mustBe
          ListMap(
            "10" -> Messages(s"tai.taxCode.amount", 100),
            "T" -> Messages(s"tai.taxCode.T")
          )
      }
    }
  }

  private val taxCodeIncomes1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
  private val taxCodeIncomes2 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "BR", "employer2", Week1Month1BasisOfOperation, Live)
  private val nino = new Generator().nextNino
  private val scottishTaxRateBands = Map.empty[String, BigDecimal]

  object TaxCodeDescriptorConcrete extends TaxCodeDescriptor

  private class SUT(welshEnabled: Boolean = true) extends TaiLanguageController {
    override val personService: PersonService = mock[PersonService]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever
    override val authConnector: AuthConnector = mock[AuthConnector]
    override val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val isWelshEnabled = welshEnabled

    val authority = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(authority)
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
  }
}
