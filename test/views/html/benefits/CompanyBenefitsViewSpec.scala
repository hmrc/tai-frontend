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

package views.html.benefits

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel
import uk.gov.hmrc.tai.viewModels.CompanyBenefitViewModel

class CompanyBenefitsViewSpec extends TaiViewSpec {

  private lazy val modelWithCompanyBenefits = model.copy(benefits = companyBenefits)
  private val template: CompanyBenefitsView = inject[CompanyBenefitsView]
  override def view: Html = template(model)

  private lazy val companyBenefits = Seq(
    CompanyBenefitViewModel("ben1", BigDecimal(100.20), "url1"),
    CompanyBenefitViewModel("ben2", BigDecimal(3002.23), "url2"),
    CompanyBenefitViewModel("ben3", BigDecimal(22.44), "url3")
  )

  private lazy val model = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Employer",
    100,
    400,
    "1100L",
    "EMPLOYER-1122",
    isPension = false,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrctNumber = "123",
    payeNumber = "AB12345"
  )

  private lazy val modelWithUpdateInProgressEmployment = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Employer",
    100,
    400,
    "1100L",
    "EMPLOYER-1122",
    isPension = false,
    estimatedPayJourneyCompleted = true,
    rtiAvailable = true,
    taxDistrctNumber = "123",
    payeNumber = "AB12345",
    isUpdateInProgress = true
  )

  def viewWithUpdateInProgressEmployment: Html = template(modelWithUpdateInProgressEmployment)
  lazy val docWithUpdateInProgressEmployment: Document = Jsoup.parse(viewWithUpdateInProgressEmployment.toString())

  "display a company benefit section" in {
    doc must haveDivWithId("companyBenefitsSection")
  }

  "use conditional logic to display the company benefits section" which {
    "displays the section otherwise" in {
      val testDoc = Jsoup.parse(template(model.copy(isPension = false)).toString)
      testDoc must haveDivWithId("companyBenefitsSection")
    }
  }

  "use conditional logic to display a company benefits list" which {
    "displays the list when benefits are present in the view model" in {
      val testDoc = Jsoup.parse(template(modelWithCompanyBenefits).toString)
      testDoc must haveElementAtPathWithId("#companyBenefitsSection dl", "companyBenefitList")
    }

    "does not display the list when benefits are absent from the view model" in {
      doc must not(haveElementAtPathWithId("#companyBenefitsSection dl", "companyBenefitList"))
    }

    "displays a 'no company benefits' message when benefits are absent from the view model" in {
      doc must haveElementWithId("noCompanyBenefitsMessage")
    }
  }

  "display the appropriate number of company benefit list entries" in {
    val testDoc = Jsoup.parse(template(modelWithCompanyBenefits).toString)
    testDoc must haveElementWithId("companyBenefitList")
    testDoc must haveElementAtPathWithId("#companyBenefitList dt", "companyBenefitTerm1")
    testDoc must haveElementAtPathWithId("#companyBenefitList dt", "companyBenefitTerm2")
    testDoc must haveElementAtPathWithId("#companyBenefitList dt", "companyBenefitTerm3")
    testDoc must not(haveElementAtPathWithId("#companyBenefitList dt", "companyBenefitTerm4"))
  }

  "display the appropriate content with a specific company benefit list entry" in {
    val testDoc = Jsoup.parse(template(modelWithCompanyBenefits).toString)
    testDoc must haveElementAtPathWithText(
      "#companyBenefitTerm1",
      s"ben1"
    )
    testDoc must haveElementAtPathWithText(
      "#companyBenefitDescription1",
      "£100"
    )
    testDoc must haveElementAtPathWithText("#companyBenefitDescription1", "£100")
    testDoc must haveElementAtPathWithText(
      "#companyBenefitChangeLinkDescription1 a span",
      s"${messages("tai.updateOrRemove")} ben1"
    )
    testDoc must haveLinkWithUrlWithID("changeCompanyBenefitLink1", "url1")
  }

  "display a link to add a missing company benefit" in {
    doc must haveElementAtPathWithId("#companyBenefitsSection a", "addMissingCompanyBenefitLink")
    doc must haveLinkWithUrlWithID(
      "addMissingCompanyBenefitLink",
      controllers.routes.ExternalServiceRedirectController
        .auditInvalidateCacheAndRedirectService(TaiConstants.CompanyBenefitsIform)
        .url
    )
  }

  "use conditional logic to display a link to add a company car" which {
    "displays the link when the view model flag is set" in {
      val testDoc = Jsoup.parse(template(model.copy(displayAddCompanyCarLink = true)).toString)
      testDoc must haveLinkWithUrlWithID(
        "addMissingCompanyCarLink",
        controllers.routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.CompanyCarsIform)
          .url
      )
    }
    "hides the link when the view model flag is not set" in {
      val testDoc = Jsoup.parse(template(model.copy(displayAddCompanyCarLink = false)).toString)
      testDoc must not(haveElementWithId("addMissingCompanyCarLink"))
    }
  }

  "display a link to return to income tax summary" in {
    doc must haveLinkWithUrlWithClass(
      "govuk-back-link",
      controllers.routes.TaxAccountSummaryController.onPageLoad().url
    )
  }

}
