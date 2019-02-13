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

package views.html

import controllers.routes
import org.jsoup.Jsoup
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{CompanyBenefitViewModel, IncomeSourceSummaryViewModel}

class IncomeSourceSummaryViewSpec extends TaiViewSpec {
  "Income details spec" must {
    behave like pageWithCombinedHeader(
      model.displayName,
      messages("tai.employment.income.details.mainHeading",
        model.empOrPensionName,
        model.startOfCurrentYear.replace(" ", "\u00A0"),
        model.endOfCurrentYear.replace(" ", "\u00A0"
        )
      )
    )

    behave like pageWithTitle(
      messages("tai.employment.income.details.mainHeading.gaTitle",
        model.startOfCurrentYear,
        model.endOfCurrentYear
      )
    )

    "display headings" when {
      "income source is pension" in {
        pensionDoc must havePreHeadingWithText(pensionModel.displayName)

        pensionDoc must haveHeadingWithText(messages("tai.pension.income.details.mainHeading", pensionModel.empOrPensionName,
          pensionModel.startOfCurrentYear.replace(" ", "\u00A0"), pensionModel.endOfCurrentYear.replace(" ", "\u00A0")))

        pensionDoc.title must include( messages("tai.pension.income.details.mainHeading.gaTitle",
          pensionModel.startOfCurrentYear, pensionModel.endOfCurrentYear) )
      }
    }

    "display link to update or remove employer" when {
      "income source is employment" in {
        doc must haveParagraphWithText(messages("tai.employment.income.details.updateLinkText"))
        doc must haveLinkWithUrlWithID("updateEmployer",
          controllers.employments.routes.EndEmploymentController.employmentUpdateRemove(model.empId).url)
      }

      "income source is pension" in {
        pensionDoc must haveParagraphWithText(messages("tai.pension.income.details.updateLinkText"))
        pensionDoc must haveLinkWithUrlWithID("updatePension",
          controllers.pensions.routes.UpdatePensionProviderController.redirectUpdatePension(model.empId).url)
      }
    }

    "display estimated income details" when {
      "income source is employment" in {
        doc must haveHeadingH2WithText(messages("tai.income.details.estimatedTaxableIncome"))
        doc must haveParagraphWithText(messages("tai.income.details.estimatedTaxableIncome.desc"))
        doc must haveSpanWithText("£" + model.estimatedTaxableIncome)
        doc must haveLinkWithText(messages("tai.income.details.updateTaxableIncome.update"))
        doc must haveLinkWithUrlWithID("updateIncome",
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.estimatedPayLandingPage(model.empId).url)
      }

      "income source is pension" in {
        pensionDoc must haveHeadingH2WithText(messages("tai.income.details.estimatedTaxableIncome"))
        pensionDoc must haveParagraphWithText(messages("tai.income.details.estimatedTaxableIncome.desc"))
        pensionDoc must haveSpanWithText("£" + pensionModel.estimatedTaxableIncome)
        pensionDoc must haveLinkWithText(messages("tai.income.details.updateTaxableIncome.update"))
        pensionDoc must haveLinkWithUrlWithID("updateIncome",
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.estimatedPayLandingPage(model.empId).url)
      }
    }

    "display income received to date" when {
      "income source is employment" in {
        doc must haveHeadingH2WithText(messages("tai.income.details.incomeReceivedToDate"))
        doc must haveParagraphWithText(messages("tai.income.details.incomeReceivedToDate.desc", model.htmlNonBroken(model.startOfCurrentYear)))
        doc must haveSpanWithText("£" + model.incomeReceivedToDate)
        doc must haveLinkWithUrlWithID("viewIncomeReceivedToDate",
          controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(model.empId).url)
      }

      "income source is pension" in {
        pensionDoc must haveHeadingH2WithText(messages("tai.income.details.incomeReceivedToDate"))
        pensionDoc must haveParagraphWithText(messages("tai.income.details.incomeReceivedToDate.desc", model.htmlNonBroken(model.startOfCurrentYear)))
        pensionDoc must haveSpanWithText("£" + pensionModel.incomeReceivedToDate)
        pensionDoc must haveLinkWithUrlWithID("viewIncomeReceivedToDate",
          controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(pensionModel.empId).url)
      }
    }

    "display tax code" in {
      doc must haveHeadingH2WithText(messages("tai.taxCode"))
      doc must haveSpanWithText(model.taxCode)
      doc must haveLinkWithUrlWithID("understandTaxCode",
        routes.YourTaxCodeController.taxCodes().toString)
    }

    "display payroll number" when {
      "income source is employment" in {
        doc must haveHeadingH2WithText(messages("tai.payRollNumber"))
        doc must haveParagraphWithText(model.pensionOrPayrollNumber)
      }

      "income source is pension" in {
        pensionDoc must haveHeadingH2WithText(messages("tai.pensionNumber"))
        pensionDoc must haveParagraphWithText(pensionModel.pensionOrPayrollNumber)
      }
    }

    "display a company benefit section" in {
      doc must haveSectionWithId("companyBenefitsSection")
      doc must haveH2HeadingWithIdAndText("companyBenefitsHeading", messages("tai.income.details.companyuBenefitsHeading", "Employer"))
    }

    "use conditional logic to display the company benefits section" which {
      "hides the section when the income type is pension" in {
        val testDoc = Jsoup.parse(views.html.IncomeSourceSummary(model.copy(isPension = true)).toString)
        testDoc must not(haveSectionWithId("companyBenefitsSection"))
      }
      "displays the scetion otherwise" in {
        val testDoc = Jsoup.parse(views.html.IncomeSourceSummary(model.copy(isPension = false)).toString)
        testDoc must haveSectionWithId("companyBenefitsSection")
      }
    }

    "use conditional logic to display a company benefits list" which {
      "displays the list when benefits are present in the view model" in {
        val testDoc = Jsoup.parse(views.html.IncomeSourceSummary(modelWithCompanyBenefits).toString)
        testDoc must haveElementAtPathWithId("#companyBenefitsSection ul", "companyBenefitList")
      }
      "does not display the list when benefits are absent from the view model" in {
        doc must not(haveElementAtPathWithId("#companyBenefitsSection ul", "companyBenefitList"))
      }
      "displays a 'no company benefits' message when benefits are absent from the view model" in {
        doc must haveElementWithId("noCompanyBenefitsMessage")
      }
    }

    "display the appropriate number of company benefit list entries" in {
      val testDoc = Jsoup.parse(views.html.IncomeSourceSummary(modelWithCompanyBenefits).toString)
      testDoc must haveElementWithId("companyBenefitList")
      testDoc must haveElementAtPathWithId("#companyBenefitList div", "companyBenefitTerm1")
      testDoc must haveElementAtPathWithId("#companyBenefitList div", "companyBenefitTerm2")
      testDoc must haveElementAtPathWithId("#companyBenefitList div", "companyBenefitTerm3")
      testDoc must not(haveElementAtPathWithId("#companyBenefitList div", "companyBenefitTerm4"))
    }

    "display the appropriate content with a specific company benefit list entry" in {
      val testDoc = Jsoup.parse(views.html.IncomeSourceSummary(modelWithCompanyBenefits).toString)
      testDoc must haveElementAtPathWithText("#companyBenefitTerm1", s"${messages("tai.income.details.benefit.name.announce")} ben1")
      testDoc must haveElementAtPathWithText("#companyBenefitDescription1 span", s"${messages("tai.income.details.benefit.amount.announce", "£100")}")
      testDoc must haveElementAtPathWithText("#companyBenefitDescription1 span", "£100")
      testDoc must haveElementAtPathWithText("#companyBenefitChangeLinkDescription1 a span", s"${messages("tai.updateOrRemove")} ben1")
      testDoc must haveLinkWithUrlWithID("changeCompanyBenefitLink1", "url1")
    }

    "display a link to add a missing company benefit" in {
      doc must haveElementAtPathWithId("#companyBenefitsSection a", "addMissingCompanyBenefitLink")
      doc must haveLinkWithUrlWithID("addMissingCompanyBenefitLink", controllers.routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.CompanyBenefitsIform).url)
    }

    "use conditional logic to display a link to add a company car" which {
      "displays the link when the view model flag is set" in {
        val testDoc = Jsoup.parse(views.html.IncomeSourceSummary(model.copy(displayAddCompanyCarLink = true)).toString)
        testDoc must haveLinkWithUrlWithID("addMissingCompanyCarLink", controllers.routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.CompanyCarsIform).url)
      }
      "hides the link when the view model flag is not set" in {
        val testDoc = Jsoup.parse(views.html.IncomeSourceSummary(model.copy(displayAddCompanyCarLink = false)).toString)
        testDoc must not(haveElementWithId("addMissingCompanyCarLink"))
      }
    }

    "display a link to return to income tax summary" in {
      doc must haveLinkWithUrlWithID("taxableIncomeLink", controllers.routes.TaxAccountSummaryController.onPageLoad.url)
    }

  }

  private lazy val model = IncomeSourceSummaryViewModel(1, "User Name", "Employer", 100, 400, "1100L", "EMPLOYER-1122", false)
  private lazy val companyBenefits = Seq(
    CompanyBenefitViewModel("ben1", BigDecimal(100.20), "url1"),
    CompanyBenefitViewModel("ben2", BigDecimal(3002.23), "url2"),
    CompanyBenefitViewModel("ben3", BigDecimal(22.44), "url3")
  )
  private lazy val modelWithCompanyBenefits = model.copy(benefits = companyBenefits)
  private lazy val pensionModel = IncomeSourceSummaryViewModel(1, "User Name", "PENSION", 100, 400, "1100L", "PENSION-1122", true)
  private lazy val pensionDoc = Jsoup.parse(pensionView.toString())

  override def view: Html = views.html.IncomeSourceSummary(model)

  def pensionView: Html = views.html.IncomeSourceSummary(pensionModel)
}
