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

package views.html.paye

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.TaxPeriodLabelService
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.HistoricPayAsYouEarnViewModel
import uk.gov.hmrc.tai.viewModels.HistoricPayAsYouEarnViewModel.EmploymentViewModel


class historicPayAsYouEarnSpec extends TaiViewSpec with TaxPeriodLabelService{

  private val currentYear: Int = TaxYear().year
  private val cyMinusOneTaxYear: TaxYear = TaxYear(currentYear - 1)
  private val cyMinusTwoTaxYear: TaxYear = TaxYear(currentYear - 2)
  private val cyMinusThreeTaxYear: TaxYear = TaxYear(currentYear - 3)
  private val cyMinusFourTaxYear: TaxYear = TaxYear(currentYear - 4)

  private val employment: EmploymentViewModel = EmploymentViewModel("test employment", 123.32, 1, false,Some("payrollNumber"))
  override def view: Html = views.html.paye.historicPayAsYouEarn(HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, true), 3, taxCodeChangeEnabled = true)

  private def createSut(vm: HistoricPayAsYouEarnViewModel, noOfPreviousYears: Int = 3): Html =  views.html.paye.historicPayAsYouEarn(vm, noOfPreviousYears, taxCodeChangeEnabled = true)

  "historicPayAsYouEarn view" should {

    behave like pageWithCombinedHeader(
      messages("tai.paye.lastTaxYear.preHeading"),
      messages("tai.paye.heading", taxPeriodLabel(cyMinusOneTaxYear.year)))

    behave like pageWithTitle(messages("tai.paye.heading", taxPeriodLabel(cyMinusOneTaxYear.year)))

    "contain correct header" in {
      val taxYear = cyMinusOneTaxYear
      val vm = HistoricPayAsYouEarnViewModel(taxYear, Nil, Nil, false, true)
      val view: Html = views.html.paye.historicPayAsYouEarn(HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, true), 3, taxCodeChangeEnabled = true)
      val newDoc = doc(view)

      newDoc.body.text must include(messages("tai.paye.lastTaxYear.preHeading"))
      newDoc.body.text must include(messages("tai.paye.heading", taxPeriodLabel(taxYear.year)))
    }

    "NOT display a link to view the tax code at the end of the year" when {
      "taxCodeChangeEnabled is false" in {
        val employment: EmploymentViewModel = EmploymentViewModel("test employment", 0.00, 1, false,Some("payrollNumber"))
        val view: Html = views.html.paye.historicPayAsYouEarn(HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true), 1, taxCodeChangeEnabled = false)

        doc(view).toString mustNot include(messages("tai.taxCode.description.link"))
      }

      "showTaxCodeDescription is false" in {
        val employment: EmploymentViewModel = EmploymentViewModel("test employment", 0.00, 1, false,Some("payrollNumber"))
        val view: Html = views.html.paye.historicPayAsYouEarn(HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, false), 1, taxCodeChangeEnabled = true)

        doc(view).toString mustNot include(messages("tai.taxCode.description.link"))
      }
    }

    "display a link to return to choose tax year page" in {
      doc must haveLinkWithUrlWithID("returnToChooseTaxYearLink", controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
    }

    "income from employment section" must {

      "have a heading for your income" when {
        "when you have a employment" in {
          val employment: EmploymentViewModel = EmploymentViewModel("test employment", 0.00, 1, false,Some("payrollNumber"))
          val view: Html = views.html.paye.historicPayAsYouEarn(HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true), 1, taxCodeChangeEnabled = true)
          doc(view) must haveH2HeadingWithText(messages("tai.paye.incomeEmployment.heading"))
          doc(view) mustNot haveH2HeadingWithText(messages("tai.incomeTaxSummary.pension.section.heading"))
        }

        "when you have a pension" in {
          val pension: EmploymentViewModel = EmploymentViewModel("test employment", 0.00, 1, true,Some("payrollNumber"))
          val view: Html = views.html.paye.historicPayAsYouEarn(HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Seq(pension), Nil, true, true), 1, taxCodeChangeEnabled = true)
          doc(view) mustNot haveH2HeadingWithText(messages("tai.paye.incomeEmployment.heading"))
          doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxSummary.pension.section.heading"))
        }

        "when you have a pension and employment" in {
          val employment: EmploymentViewModel = EmploymentViewModel("test employment", 0.00, 1, false,Some("payrollNumber"))
          val pension: EmploymentViewModel = EmploymentViewModel("test employment", 0.00, 1, true,Some("payrollNumber"))
          val view: Html = views.html.paye.historicPayAsYouEarn(HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Seq(pension), Seq(employment), true, true), 1, taxCodeChangeEnabled = true)
          doc(view) must haveH2HeadingWithText(messages("tai.paye.incomeEmployment.heading"))
          doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxSummary.pension.section.heading"))
        }
      }

      "not show employments" when {

        "the viewmodel contains zero employments" in {

          val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Nil, false, true)
          val sut: Html = createSut(vm)

          doc.select("#lastTaxYearIncome").size mustBe 0
        }
      }

      "show one employment with zero YTD totalIncome from zero payments" when {
        "the viewmodel contains one employment containing an AnnualAccount which has no payments or updates" in {

          val employment: EmploymentViewModel = EmploymentViewModel("employment", 0.00, 1, false,Some("payrollNumber"))
          val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true)

          val sut: Html = createSut(vm)
          val doc: Document = Jsoup.parse(sut.toString)

          doc must haveElementAtPathWithId("div", "employment1")
          doc must haveHeadingH3WithText("employment")
          doc must haveParagraphWithText("£0.00")

          doc must haveLinkElement(
            "employment1-checkDetailsLink",
            controllers.routes.YourIncomeCalculationController.yourIncomeCalculationHistoricYears(vm.taxYear,employment.id).url,
              messages("tai.paye.lastTaxYear.table.link")
          )
          doc must haveLinkElement(
            "p800Link",
            ApplicationConfig.taxYouPaidStatus,
            messages("tai.paye.lastTaxYear.checkIncomeTax.link")
          )
          doc must haveHeadingH3WithText(messages("tai.paye.lastTaxYear.incorrectInformation.title"))
        }
      }

      "show a payroll number" when {
        "the income source of type employer has a payroll number" in {
          val employment: EmploymentViewModel = EmploymentViewModel("employment", 0.00, 1, false,Some("payrollNumber"))
          val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true)

          val sut: Html = createSut(vm)
          val doc: Document = Jsoup.parse(sut.toString)

          doc must haveSpanWithText("Payroll number: payrollNumber")
          doc must haveSpanWithText("Your payroll number is payrollNumber.")
        }
      }
      "show a pension number" when {
        "the income source of type pension has a payroll number" in {
          val pension: EmploymentViewModel = EmploymentViewModel("employment", 0.00, 1, true, Some("pensionNumber"))
          val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Seq(pension), Nil, true, true)

          val sut: Html = createSut(vm)
          val doc: Document = Jsoup.parse(sut.toString)

          doc must haveSpanWithText("Pension number: pensionNumber")
          doc must haveSpanWithText("Your pension number is pensionNumber.")
        }
      }

      "doesnt show a payroll number" when {
        "the income source of type employer does not have a payroll number" in {
          val employment: EmploymentViewModel = EmploymentViewModel("employment", 0.00, 1, false,None)
          val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true)

          val sut: Html = createSut(vm)
          val doc: Document = Jsoup.parse(sut.toString)

          doc mustNot haveSpanWithText("Payroll number: payrollNumber")
          doc mustNot haveSpanWithText("Your payroll number is payrollNumber.")
        }
      }
      "doesnt show a pension number" when {
        "the income source of type pension does not have a payroll number" in {
          val pension: EmploymentViewModel = EmploymentViewModel("employment", 0.00, 1, true, None)
          val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Seq(pension), Nil, true, true)

          val sut: Html = createSut(vm)
          val doc: Document = Jsoup.parse(sut.toString)

          doc mustNot haveSpanWithText("Pension number: pensionNumber")
          doc mustNot haveSpanWithText("Your pension number is pensionNumber.")
        }
      }


      "show one pension" when {
        "the viewmodel contains one pension" in {

          val pension: EmploymentViewModel = EmploymentViewModel("pension", 0.00, 1, true,Some("payrollNumber"))
          val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Seq(pension), Nil, true, true)

          val sut: Html = createSut(vm)
          val doc: Document = Jsoup.parse(sut.toString)

          doc must haveElementAtPathWithId("div", "pension1")
          doc must haveHeadingH3WithText("pension")
          doc must haveParagraphWithText("£0.00")
          doc must haveLinkElement(
            "pension1-checkDetailsLink",
            controllers.routes.YourIncomeCalculationController.yourIncomeCalculationHistoricYears(vm.taxYear,employment.id).url,
            messages("tai.paye.lastTaxYear.table.link")
          )
          doc must haveLinkElement(
            "p800Link",
            ApplicationConfig.taxYouPaidStatus,
            messages("tai.paye.lastTaxYear.checkIncomeTax.link")
          )
          doc must haveHeadingH3WithText(messages("tai.paye.lastTaxYear.incorrectInformation.title"))
        }
      }

      "always show the update link even" when {

        "the viewmodel contains zero employments" in {

          val taxYear = cyMinusOneTaxYear

          val vm = HistoricPayAsYouEarnViewModel(taxYear, Nil, Nil, false, true)

          val sut: Html = createSut(vm)
          val doc: Document = Jsoup.parse(sut.toString)

          doc must haveLinkWithText(messages("tai.paye.lastTaxYear.incorrectInformation.link"))
          doc must haveLinkWithUrlWithID("updateEmployment", controllers.income.previousYears.routes.UpdateIncomeDetailsController.decision(vm.taxYear).url)
        }
      }

          "show one employment with YTD totalIncome from one payment" when {
            "one employment exists containing an AnnualAccount which has payments" in {

              val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true)

              val sut: Html = createSut(vm)
              val doc: Document = Jsoup.parse(sut.toString)

              doc must haveElementAtPathWithId("div", "employment1")
              doc must haveHeadingH3WithText("test employment")
              doc must haveParagraphWithText("£123.32")
              doc must haveLinkElement(
                "employment1-checkDetailsLink",
                controllers.routes.YourIncomeCalculationController.yourIncomeCalculationHistoricYears(vm.taxYear,employment.id).url,
                messages("tai.paye.lastTaxYear.table.link")
              )
              doc must haveLinkElement(
                "p800Link",
                ApplicationConfig.taxYouPaidStatus,
                messages("tai.paye.lastTaxYear.checkIncomeTax.link")
              )
            }
          }

          "show multiple employments with YTD totalIncome" when {
            "multiple employments exist containing an AnnualAccount which has payments" in {

              val employment1: EmploymentViewModel = EmploymentViewModel("test employment 1", 123.32, 1, false,Some("payrollNumber"))
              val employment2: EmploymentViewModel = EmploymentViewModel("test employment 2", 345.54, 2, false,Some("payrollNumber"))
              val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment1, employment2), true, true)

              val sut: Html = createSut(vm)
              val doc: Document = Jsoup.parse(sut.toString)

              doc must haveElementAtPathWithId("div", "employment1")
              doc must haveHeadingH3WithText("test employment 1")
              doc must haveParagraphWithText("£123.32")
              doc must haveLinkElement(
                "employment1-checkDetailsLink",
                controllers.routes.YourIncomeCalculationController.yourIncomeCalculationHistoricYears(vm.taxYear,employment.id).url,
                messages("tai.paye.lastTaxYear.table.link")
              )

              doc must haveElementAtPathWithId("div", "employment2")
              doc must haveHeadingH3WithText("test employment 2")
              doc must haveParagraphWithText("£345.54")
              doc must haveLinkElement(
                "employment2-checkDetailsLink",
                controllers.routes.YourIncomeCalculationController.yourIncomeCalculationHistoricYears(vm.taxYear,employment2.id).url,
                messages("tai.paye.lastTaxYear.table.link")
              )

              doc must haveLinkElement(
                "p800Link",
                ApplicationConfig.taxYouPaidStatus,
                messages("tai.paye.lastTaxYear.checkIncomeTax.link")
              )
            }
          }

    }

    "show the previous year income section" when {
      "the viewmodel has the realTimeStatus of Available" in {

        val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true)

        val sut: Html = createSut(vm)
        val doc: Document = Jsoup.parse(sut.toString)

        doc must haveElementAtPathWithId("div", "employment1")

        doc must haveLinkElement(
          "p800Link",
          ApplicationConfig.taxYouPaidStatus,
          messages("tai.paye.lastTaxYear.checkIncomeTax.link")
        )

        doc.select("#rtiDown").size mustBe 0
      }
    }

    "show an RTI down message" when {
      "there are no employments" in {

        val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Nil, false, true)

        val sut: Html = createSut(vm)
        val doc: Document = Jsoup.parse(sut.toString)

        doc.select("#rtiDown").size mustBe 1

        doc.select("#rtiDownMessage").text() mustBe messages("tai.rti_down_message")
        doc.select("#rtiDownContact").text() mustBe messages("tai.rti_down_message_contact")

        doc.select("#p800Link").size mustBe 1

        doc.select("#lastTaxYearIncome").size mustBe 0
        doc.select("#last-tax-year-table li").size() mustBe 0
      }
    }

    "not show p800 link" when {
      "the previous tax year is earlier than CY-1 such as CY-2" in {

        val vm = HistoricPayAsYouEarnViewModel(cyMinusTwoTaxYear, Nil, Nil, false, true)

        val sut: Html = createSut(vm)
        val doc: Document = Jsoup.parse(sut.toString)

        doc.select("#p800Link").size() mustBe 0
      }

      "the previous tax year is earlier than CY-1 such as CY-3" in {

        val vm = HistoricPayAsYouEarnViewModel(cyMinusThreeTaxYear, Nil, Nil, false, true)

        val sut: Html = createSut(vm)
        val doc: Document = Jsoup.parse(sut.toString)

        doc.select("#p800Link").size() mustBe 0
      }

      "the previous tax year is earlier than CY-1 such as CY-4" in {

        val vm = HistoricPayAsYouEarnViewModel(cyMinusFourTaxYear, Nil, Nil, false, true)

        val sut: Html = createSut(vm)
        val doc: Document = Jsoup.parse(sut.toString)

        doc.select("#p800Link").size() mustBe 0
      }
    }

    "always show p800 link" when {
      "the previous tax year is CY-1" in {

        val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Nil, false, true)

        val sut: Html = createSut(vm)
        val doc: Document = Jsoup.parse(sut.toString)

        doc must haveLinkWithUrlWithID("p800Link", ApplicationConfig.taxYouPaidStatus.toString)
        doc.select("#p800Link").size() mustBe 1
      }
    }

    "display navigation" when {
      "number of previous years to show is greater than zero" in {
        val employment: EmploymentViewModel = EmploymentViewModel("", 0.00, 0, false,Some("payrollNumber"))
        val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true)

        val sut: Html = createSut(vm)

        val doc: Document = Jsoup.parse(sut.toString)

        doc.select(".grid-1-3").size() mustBe 1
      }
    }

    "not display navigation" when {
      "number of previous years to show is 1" in {
        val employment: EmploymentViewModel = EmploymentViewModel("", 0.00, 0, false,Some("payrollNumber"))
        val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true)

        val sut: Html = createSut(vm, 1)

        val doc: Document = Jsoup.parse(sut.toString)

        doc.select(".grid-layout__column--1-3").size() mustBe 0
      }

      "number of previous years to show is less than 1" in {
        val employment: EmploymentViewModel = EmploymentViewModel("", 0.00, 0, false,Some("payrollNumber"))
        val vm = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, Nil, Seq(employment), true, true)

        val sut: Html = createSut(vm, 0)

        val doc: Document = Jsoup.parse(sut.toString)

        doc.select(".grid-layout__column--1-3").size() mustBe 0
      }
    }
  }

}
