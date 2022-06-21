/*
 * Copyright 2022 HM Revenue & Customs
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

import play.twirl.api.Html
import uk.gov.hmrc.tai.service.{NoTimeToProcess, ThreeWeeks}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{IncomeSourceViewModel, TaxAccountSummaryViewModel}

class IncomeTaxSummaryViewSpec extends TaiViewSpec {

  "Income tax summary page" must {

    behave like pageWithTitle("title")

    behave like pageWithCombinedHeaderNewFormat(
      preHeaderAnnouncementText = Some("This section is the income tax summary for"),
      preHeaderText = "Firstname Surname",
      mainHeaderText = "main heading")

    "display iForms status message when an iForm has not been fully processed" in {
      doc must haveElementWithId("isAnyFormInProgressBanner")
    }

    "display print button link with javascript print function " in {
      doc must haveLinkWithUrlWithID("print-link-btn", "javascript:window.print()")
    }

    "not display iForms status message when no iForms are in progress" in {
      val vm = TaxAccountSummaryViewModel(
        "main heading",
        "title",
        "£15,000",
        "£12,320",
        "5 April 2017",
        Seq(activeEmployment),
        Seq(pensionIncome),
        Seq(endedEmployment),
        displayIyaBanner = false,
        NoTimeToProcess,
        Seq(otherIncomeSourceViewModel),
        rtiAvailable = true,
        "0"
      )
      def view: Html = template(vm, appConfig)
      doc(view) must not(haveElementWithId("isAnyFormInProgressBanner"))
    }

    "display three outline sections: 'income from employment', 'employments that have ended', and 'income from private pensions'" when {

      "the associated collections within the view model are non empty" in {
        doc must haveDivWithId("incomeFromEmploymentSection")
        doc must haveElementAtPathWithText(
          "#incomeFromEmploymentSection h2",
          messages("tai.incomeTaxSummary.employment.section.heading"))
        doc must haveDivWithId("incomeFromPensionSection")
        doc must haveElementAtPathWithText(
          "#incomeFromPensionSection h2",
          messages("tai.incomeTaxSummary.pension.section.heading"))
        doc must haveDivWithId("endedIncomeSection")
        doc must haveElementAtPathWithText(
          "#endedIncomeSection h2",
          messages("tai.incomeTaxSummary.ceased.section.heading", vm.lastTaxYearEnd))
      }
    }

    "hide each of the three outline sections: 'income from employment', 'employments that have ended', and 'income from private pensions'" when {

      "the associated collections within the view model are empty" in {
        val docWithoutIncomeSections = doc(template(noSectionsVm, appConfig))
        docWithoutIncomeSections must not(haveDivWithId("incomeFromEmploymentSection"))
        docWithoutIncomeSections must not(haveDivWithId("incomeFromPensionSection"))
        docWithoutIncomeSections must not(haveDivWithId("endedIncomeSection"))
      }
    }

    "display an 'Annual Amounts' summary section" in {
      doc must haveDivWithId("annualAmountsSummarySection")
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection h2",
        messages("tai.incomeTaxSummary.annualAmounts.section.heading"))

    }

    "display a 'Something Missing' section" in {
      doc must haveDivWithId("addMissingIncomeSourceSection")
      doc must haveElementAtPathWithText(
        "#addMissingIncomeSourceSection h2",
        messages("tai.incomeTaxSummary.addMissingIncome.section.heading"))
    }

    "hide the 'In Year Adjustment' banner" when {
      "the corresponding boolean is unset within the view model" in {
        doc must not(haveElementWithId("inYearAdjustmentBanner"))
      }
    }

    "display an 'In Year Adjustment' banner" when {
      "the corresponding boolean is set within the view model" in {
        val vm = TaxAccountSummaryViewModel(
          "",
          "",
          "",
          "",
          "",
          Nil,
          Nil,
          Nil,
          displayIyaBanner = true,
          ThreeWeeks,
          Seq(otherIncomeSourceViewModel),
          rtiAvailable = true,
          "0")

        val docWithIyaBanner = doc(template(vm, appConfig))
        docWithIyaBanner must haveElementAtPathWithText(
          "#inYearAdjustmentBanner",
          s"${messages("tai.notifications.iya.banner.text")} ${messages("tai.notifications.iya.linkText")}")
      }
    }

    "display other income sources heading and details" when {
      "other income sources are available" in {
        doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxSummary.otherIncomeSources.heading"))

        doc(view) must haveHeadingH3WithText(otherIncomeSourceViewModel.name)
        doc(view) must haveSpanWithText("£123")
      }
    }
  }

  "income from employment section" must {

    "display the correct number of employment detail sections" when {

      "there is a single income source view model instance supplied within the 'employments' view model sequnce" in {
        doc.select("#incomeFromEmploymentSection h3").size() mustBe 1
      }

      "there are multiple income source view model instance supplied within the 'employments' view model sequnce" in {
        val vm = TaxAccountSummaryViewModel(
          "",
          "",
          "",
          "",
          "",
          Seq(
            activeEmployment.copy(name = "name1"),
            activeEmployment.copy(name = "name2"),
            activeEmployment.copy(name = "name3")),
          Nil,
          Nil,
          displayIyaBanner = false,
          ThreeWeeks,
          Seq(otherIncomeSourceViewModel),
          rtiAvailable = true,
          "0"
        )
        val docWithMultipleEmployments = doc(template(vm, appConfig))
        docWithMultipleEmployments.select("#incomeFromEmploymentSection h3").size mustBe 3

        docWithMultipleEmployments must haveElementAtPathWithId("div", "employment1")
        docWithMultipleEmployments must haveElementAtPathWithText(
          "#incomeFromEmploymentSection #employment1 h3",
          "name1")
        docWithMultipleEmployments must haveElementAtPathWithId("div", "employment2")
        docWithMultipleEmployments must haveElementAtPathWithText(
          "#incomeFromEmploymentSection #employment2 h3",
          "name2")
        docWithMultipleEmployments must haveElementAtPathWithId("div", "employment3")
        docWithMultipleEmployments must haveElementAtPathWithText(
          "#incomeFromEmploymentSection #employment3 h3",
          "name3")
        docWithMultipleEmployments must not(haveElementAtPathWithId("div", "employment4"))
      }
    }
  }

  "income from private pensions section" must {

    "display the correct number of pension employment detail sections" when {

      "there is a single income source view model instance supplied within the 'pensions' view model sequnce" in {
        doc.select("#incomeFromPensionSection h3").size() mustBe 1
      }

      "there are multiple income source view model instance supplied within the 'pensions' view model sequnce" in {
        val vm = TaxAccountSummaryViewModel(
          "",
          "",
          "",
          "",
          "",
          Nil,
          Seq(
            endedEmployment.copy(name = "name1"),
            endedEmployment.copy(name = "name2"),
            endedEmployment.copy(name = "name3"),
            endedEmployment.copy(name = "name4")),
          Nil,
          displayIyaBanner = false,
          ThreeWeeks,
          Seq(otherIncomeSourceViewModel),
          rtiAvailable = true,
          "0"
        )
        val docWithMultiplePensionIncomes = doc(template(vm, appConfig))
        docWithMultiplePensionIncomes.select("#incomeFromPensionSection h3").size mustBe 4

        docWithMultiplePensionIncomes must haveElementAtPathWithId("div", "pension1")
        docWithMultiplePensionIncomes must haveElementAtPathWithText("#incomeFromPensionSection #pension1 h3", "name1")
        docWithMultiplePensionIncomes must haveElementAtPathWithId("div", "pension2")
        docWithMultiplePensionIncomes must haveElementAtPathWithText("#incomeFromPensionSection #pension2 h3", "name2")
        docWithMultiplePensionIncomes must haveElementAtPathWithId("div", "pension3")
        docWithMultiplePensionIncomes must haveElementAtPathWithText("#incomeFromPensionSection #pension3 h3", "name3")
        docWithMultiplePensionIncomes must haveElementAtPathWithId("div", "pension4")
        docWithMultiplePensionIncomes must haveElementAtPathWithText("#incomeFromPensionSection #pension4 h3", "name4")
        docWithMultiplePensionIncomes must not(haveElementAtPathWithId("div", "pension5"))
      }
    }
  }

  "employments that have ended section" must {

    "display the rti is down message when rti is not available" in {
      val view = template(vm.copy(rtiAvailable = false), appConfig)
      doc(view) must haveParagraphWithText(messages("tai.rti.down.ceasedEmployments"))
    }

    "display the correct number of ended employment detail sections" when {

      "there is a single income source view model instance supplied within the 'ceasedEmployments' view model sequnce" in {
        doc.select("#endedIncomeSection h3").size() mustBe 1
      }

      "there are multiple income source view model instance supplied within the 'ceasedEmployments' view model sequnce" in {
        val vm = TaxAccountSummaryViewModel(
          "",
          "",
          "",
          "",
          "",
          Nil,
          Nil,
          Seq(
            pensionIncome.copy(name = "name1"),
            pensionIncome.copy(name = "name2"),
            pensionIncome.copy(name = "name3"),
            pensionIncome.copy(name = "name4")),
          displayIyaBanner = false,
          ThreeWeeks,
          Seq(otherIncomeSourceViewModel),
          rtiAvailable = true,
          "0"
        )
        val docWithMultipleEndedIncomes = doc(template(vm, appConfig))
        docWithMultipleEndedIncomes.select("#endedIncomeSection h3").size mustBe 4

        docWithMultipleEndedIncomes must haveElementAtPathWithId("div", "income1")
        docWithMultipleEndedIncomes must haveElementAtPathWithText("#endedIncomeSection #income1 h3", "name1")
        docWithMultipleEndedIncomes must haveElementAtPathWithId("div", "income2")
        docWithMultipleEndedIncomes must haveElementAtPathWithText("#endedIncomeSection #income2 h3", "name2")
        docWithMultipleEndedIncomes must haveElementAtPathWithId("div", "income3")
        docWithMultipleEndedIncomes must haveElementAtPathWithText("#endedIncomeSection #income3 h3", "name3")
        docWithMultipleEndedIncomes must haveElementAtPathWithId("div", "income4")
        docWithMultipleEndedIncomes must haveElementAtPathWithText("#endedIncomeSection #income4 h3", "name4")
        docWithMultipleEndedIncomes must not(haveElementAtPathWithId("div", "income5"))
      }
    }
  }

  "individual income detail sections" must {

    "display a title derived from the income source name" in {
      doc must haveElementAtPathWithText("#employment1 h3", activeEmployment.name)
    }

    "display a tax year amount" in {
      doc must haveElementAtPathWithText(
        "#employment1 p",
        s"${messages("tai.incomeTaxSummary.incomeAmount.prefix")} ${activeEmployment.amount}")
    }

    "show a tax code in link form when instructed" in {
      doc must haveElementAtPathWithText(
        "#employment1TaxCodeLink",
        messages("tai.incomeTaxSummary.taxCode.prefix", activeEmployment.taxCode))

      doc must haveElementAtPathWithAttribute(
        "#employment1TaxCodeLink",
        "href",
        controllers.routes.YourTaxCodeController.taxCode(1).url)
    }

    "omit a tax code when instructed" in {
      val vm = TaxAccountSummaryViewModel(
        "",
        "",
        "",
        "",
        "",
        Seq(activeEmployment.copy(displayTaxCode = false)),
        Nil,
        Nil,
        displayIyaBanner = false,
        ThreeWeeks,
        Seq(otherIncomeSourceViewModel),
        rtiAvailable = true,
        "0"
      )

      val document = doc(template(vm, appConfig))

      document must not(haveElementWithId("employment1TaxCodeLink"))
    }

    "show the Employer PAYE reference also referred to as ERN number" in {
      doc must haveElementAtPathWithText(
        "#employment1PayeNumber",
        messages("tai.income.details.ERN") +
          ":" +
          activeEmployment.taxDistrictNumber +
          "/" +
          activeEmployment.payeNumber)
    }

    "omit a payroll number when instructed" in {
      val vm = TaxAccountSummaryViewModel(
        "",
        "",
        "",
        "",
        "",
        Seq(activeEmployment.copy(displayPayrollNumber = false)),
        Nil,
        Nil,
        displayIyaBanner = false,
        ThreeWeeks,
        Seq(otherIncomeSourceViewModel),
        rtiAvailable = true,
        "0"
      )

      val document = doc(template(vm, appConfig))

      document must not(haveElementWithId("employment1PayrollNumber"))
    }

    "show an end date when instructed" in {
      val inactiveEmployment = activeEmployment.copy(displayEndDate = true, endDate = "31 July 2017")
      val vm = TaxAccountSummaryViewModel(
        "",
        "",
        "",
        "",
        "",
        Seq(inactiveEmployment),
        Nil,
        Nil,
        displayIyaBanner = false,
        ThreeWeeks,
        Seq(otherIncomeSourceViewModel),
        rtiAvailable = true,
        "0")

      val document = doc(template(vm, appConfig))
      document must haveElementAtPathWithText(
        "#employment1EndDate",
        messages("tai.incomeTaxSummary.endDate.prefix", inactiveEmployment.endDate))
    }

    "omit an end date when instructed" in {
      doc must not(haveElementWithId("employment1EndDate"))
    }

    "display a view details link when instructed" in {
      doc must haveElementAtPathWithText(
        "#employment1DetailsLink",
        s"${activeEmployment.detailsLinkLabel} ${messages("tai.updateOrRemove.fromOtherSources", "Company1")}")
      doc must haveElementAtPathWithAttribute("#employment1DetailsLink", "href", activeEmployment.detailsLinkUrl)
    }

    "omit a view details link when instructed" in {
      val vm = TaxAccountSummaryViewModel(
        "",
        "",
        "",
        "",
        "",
        Seq(activeEmployment.copy(displayDetailsLink = false)),
        Nil,
        Nil,
        displayIyaBanner = false,
        ThreeWeeks,
        Seq(otherIncomeSourceViewModel),
        rtiAvailable = true,
        "0"
      )

      val document = doc(template(vm, appConfig))
      document must not(haveElementWithId("employment1DetailsLink"))
    }
  }

  "annual amounts section" must {

    "display your total estimated income details" in {
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection h3",
        messages("tai.incomeTaxSummary.annualAmounts.section.totalIncomeHeading"))

      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection p",
        messages("tai.incomeTaxSummary.annualAmounts.section.totalIncomePara"))

      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection p",
        s"${messages("tai.incomeTaxSummary.generalAmount.prefix")} ${vm.totalEstimatedIncome}")

      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection a",
        messages("tai.incomeTaxSummary.annualAmounts.section.incomeTaxLink"))

      doc must haveElementAtPathWithAttribute(
        "#annualAmountsSummarySection a",
        "href",
        controllers.routes.EstimatedIncomeTaxController.estimatedIncomeTax().url)

    }

    "display tax free amount details" in {
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection h3",
        messages("tai.incomeTaxSummary.annualAmounts.section.taxFreeHeading"))
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection p",
        s"${messages("tai.incomeTaxSummary.annualAmounts.section.taxFreePara")}")
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection p",
        s"${messages("tai.incomeTaxSummary.generalAmount.prefix")} ${vm.taxFreeAmount}")
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection a",
        messages("tai.incomeTaxSummary.annualAmounts.section.taxFreeLink"))
      doc must haveElementAtPathWithAttribute(
        "#annualAmountsSummarySection a",
        "href",
        controllers.routes.TaxFreeAmountController.taxFreeAmount().url)
    }

    "display estimated income tax details" in {
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection h3",
        messages("tai.incomeTaxSummary.annualAmounts.section.incomeTaxHeading"))
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection p",
        s"${messages("tai.incomeTaxSummary.annualAmounts.section.incomeTaxPara")}")
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection p",
        s"${messages("tai.incomeTaxSummary.generalAmount.prefix")} ${vm.estimatedIncomeTaxAmount}")
      doc must haveElementAtPathWithText(
        "#annualAmountsSummarySection a",
        messages("tai.incomeTaxSummary.annualAmounts.section.incomeTaxLink"))
      doc must haveElementAtPathWithAttribute(
        "#annualAmountsSummarySection a",
        "href",
        controllers.routes.EstimatedIncomeTaxController.estimatedIncomeTax().url)
    }
  }

  "something missing section" must {

    "display an link to add a missing employer" in {
      doc must haveElementAtPathWithText(
        "#addMissingIncomeSourceSection a",
        messages("tai.incomeTaxSummary.addMissingIncome.section.employerLink"))
      doc must haveElementAtPathWithAttribute(
        "#addMissingIncomeSourceSection a",
        "href",
        controllers.employments.routes.AddEmploymentController.addEmploymentName().url)
    }
    "display an IForm link to add a missing pension" in {
      doc must haveElementAtPathWithText("#addMissingIncomeSourceSection a", messages("add.missing.pension"))
      doc must haveElementAtPathWithAttribute(
        "#addMissingIncomeSourceSection a",
        "href",
        controllers.pensions.routes.AddPensionProviderController.addPensionProviderName().url)
    }
    "display an IForm link to add a missing income source" in {
      doc must haveElementAtPathWithText(
        "#addMissingIncomeSourceSection a",
        messages("tai.incomeTaxSummary.addMissingIncome.section.otherLink"))
      doc must haveElementAtPathWithAttribute("#addMissingIncomeSourceSection a", "href", appConfig.otherIncomeLinkUrl)
    }
  }

  val activeEmployment =
    IncomeSourceViewModel(
      "Company1",
      "£23,000",
      "1150L",
      displayTaxCode = true,
      "123",
      "A100",
      "123456",
      displayPayrollNumber = true,
      "",
      displayEndDate = false,
      "view employment details",
      "fake/active/url",
      Some(controllers.routes.YourTaxCodeController.taxCode(1))
    )
  val endedEmployment =
    IncomeSourceViewModel(
      "Company2",
      "£25,000",
      "1150L",
      displayTaxCode = true,
      "",
      "",
      "",
      displayPayrollNumber = false,
      "31 July 2017",
      displayEndDate = true,
      "view income details",
      "fake/ended/url"
    )
  val pensionIncome =
    IncomeSourceViewModel(
      "PensionProvider1",
      "£14,000",
      "",
      displayTaxCode = false,
      "",
      "",
      "",
      displayPayrollNumber = false,
      "",
      displayEndDate = false,
      "view pension details",
      "fake/pension/url"
    )
  val employments: Seq[IncomeSourceViewModel] = Seq(
    IncomeSourceViewModel(
      "Company1",
      "£23,000",
      "1150L",
      displayTaxCode = true,
      "123",
      "A100",
      "123456",
      displayPayrollNumber = true,
      "",
      displayEndDate = false,
      "view income details",
      "fake/url"
    ))

  val otherIncomeSourceViewModel = IncomeSourceViewModel(
    "State Pension",
    "£123",
    "",
    displayTaxCode = false,
    "",
    "",
    "",
    displayPayrollNumber = false,
    "",
    displayEndDate = false,
    "",
    "",
    displayDetailsLink = false
  )

  val vm = TaxAccountSummaryViewModel(
    "main heading",
    "title",
    "£15,000",
    "£12,320",
    "5 April 2017",
    Seq(activeEmployment),
    Seq(pensionIncome),
    Seq(endedEmployment),
    displayIyaBanner = false,
    ThreeWeeks,
    Seq(otherIncomeSourceViewModel),
    rtiAvailable = true,
    "0"
  )

  val noSectionsVm = TaxAccountSummaryViewModel(
    "main heading",
    "title",
    "£15,000",
    "£12,320",
    "5 April 2017",
    Nil,
    Nil,
    Nil,
    displayIyaBanner = false,
    ThreeWeeks,
    Seq(otherIncomeSourceViewModel),
    rtiAvailable = true,
    "0"
  )

  private val template = inject[IncomeTaxSummaryView]

  override def view: Html = template(vm, appConfig)
}
