/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.auth.{AuthedUser, DataRequest}
import controllers.routes
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import play.twirl.api.Html
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel

class IncomeSourceSummaryViewSpec extends TaiViewSpec {

  private lazy val model = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Employer",
    Some(100),
    400,
    Some("1100L"),
    "EMPLOYER-1122",
    isPension = false,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "123",
    payeNumber = "AB12345"
  )

  private lazy val modelWithUpdateInProgressEmployment = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Employer",
    Some(100),
    400,
    Some("1100L"),
    "EMPLOYER-1122",
    isPension = false,
    estimatedPayJourneyCompleted = true,
    rtiAvailable = true,
    taxDistrictNumber = "123",
    payeNumber = "AB12345",
    isUpdateInProgress = true
  )

  private lazy val modelWithUpdateInProgressPension = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Pension",
    Some(100),
    400,
    Some("1100L"),
    "PENSION-1122",
    isPension = true,
    estimatedPayJourneyCompleted = true,
    rtiAvailable = true,
    taxDistrictNumber = "123",
    payeNumber = "AB12345",
    isUpdateInProgress = true
  )

  private lazy val pensionModel = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Pension",
    Some(100),
    400,
    Some("1100L"),
    "PENSION-1122",
    isPension = true,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "123",
    payeNumber = "AB12345"
  )

  private lazy val pensionDoc = Jsoup.parse(pensionView.toString())
  private val template: IncomeSourceSummaryView = inject[IncomeSourceSummaryView]

  protected implicit val dataRequest: DataRequest[AnyContent] = DataRequest(
    fakeRequest,
    taiUser = AuthedUser(
      Nino(nino.toString()),
      Some("saUtr"),
      None
    ),
    fullName = "",
    userAnswers = UserAnswers("", "")
  )

  override def view: Html = template(model)

  def pensionView: Html = template(pensionModel)

  def viewWithUpdateInProgressEmployment: Html = template(modelWithUpdateInProgressEmployment)

  def viewWithUpdateInProgressPension: Html = template(modelWithUpdateInProgressPension)

  lazy val docWithUpdateInProgressEmployment: Document = Jsoup.parse(viewWithUpdateInProgressEmployment.toString())
  lazy val docWithUpdateInProgressPension: Document = Jsoup.parse(viewWithUpdateInProgressPension.toString())

  "Income details spec" must {
    behave like pageWithCombinedHeaderNewFormatNew(
      model.displayName,
      messages(
        "tai.employment.income.details.mainHeading",
        model.empOrPensionName,
        TaxYearRangeUtil.currentTaxYearRangeBreak
      )
    )

    behave like pageWithTitle(
      messages("tai.employment.income.details.mainHeading.gaTitle", TaxYearRangeUtil.currentTaxYearRangeBreak)
    )

    "display headings" when {
      "income source is pension" in {
        pensionDoc must havePreHeadingWithTextGdsNew(pensionModel.displayName)

        pensionDoc must haveHeadingWithText(
          messages(
            "tai.pension.income.details.mainHeading",
            pensionModel.empOrPensionName,
            TaxYearRangeUtil.currentTaxYearRangeBreak
          )
        )
        pensionDoc.title must include(
          messages("tai.pension.income.details.mainHeading.gaTitle", TaxYearRangeUtil.currentTaxYearRangeBreak)
        )
      }
    }

    "display link to update or remove employer" when {
      "income source is employment" in {
        doc must haveParagraphWithText(
          messages("tai.employment.income.details.updateLinkText", "Employer").replaceAll("\u00A0", " ")
        )
        doc must haveLinkWithUrlWithID(
          "updateEmployer",
          controllers.employments.routes.EndEmploymentController.onPageLoad(model.empId).url
        )
      }

      "income source is pension" in {
        pensionDoc must haveParagraphWithText(
          messages("tai.pension.income.details.updateLinkText", "Pension").replaceAll("\u00A0", " ")
        )
        pensionDoc must haveLinkWithUrlWithID(
          "updatePension",
          controllers.pensions.routes.UpdatePensionProviderController.UpdatePension(model.empId).url
        )
      }
    }

    "display update message" when {
      "update is in progress and income source is employment" in {
        docWithUpdateInProgressEmployment must haveParagraphWithText(
          messages("tai.employment.income.details.updateLinkText", "Employer")
        )
        docWithUpdateInProgressEmployment must haveLinkWithUrlWithID(
          "updateEmployer",
          controllers.employments.routes.EndEmploymentController
            .onPageLoad(modelWithUpdateInProgressEmployment.empId)
            .url
        )
      }

      "update is in progress and income source is pension" in {
        docWithUpdateInProgressPension must haveParagraphWithText(
          messages("tai.pension.income.details.updateLinkText", "Pension")
        )
        docWithUpdateInProgressPension must haveLinkWithUrlWithID(
          "updatePension",
          controllers.pensions.routes.UpdatePensionProviderController
            .UpdatePension(modelWithUpdateInProgressPension.empId)
            .url
        )
      }
    }

    "display estimated income details" when {
      "income source is employment" in {
        doc must haveHeadingH2WithText(messages("tai.income.details.estimatedTaxableIncome"))
        doc must haveParagraphWithText(messages("tai.income.details.estimatedTaxableIncome.desc"))
        doc must haveSpanWithText("£" + model.estimatedTaxableIncome)
        doc must haveLinkWithText(messages("tai.income.details.updateTaxableIncome.full"))
        doc must haveLinkWithUrlWithID(
          "updateIncome",
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.onPageLoad(model.empId).url
        )
        doc mustNot haveParagraphWithText(messages("tai.income.details.updateInProgress"))
      }

      "income source is pension" in {
        pensionDoc must haveHeadingH2WithText(messages("tai.income.details.estimatedTaxableIncome"))
        pensionDoc must haveParagraphWithText(messages("tai.income.details.estimatedTaxableIncome.desc"))
        pensionDoc must haveSpanWithText("£" + pensionModel.estimatedTaxableIncome)
        pensionDoc must haveLinkWithText(messages("tai.income.details.updateTaxableIncome.full"))
        pensionDoc must haveLinkWithUrlWithID(
          "updateIncome",
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.onPageLoad(model.empId).url
        )
        doc mustNot haveParagraphWithText(messages("tai.income.details.updateInProgress"))

      }
    }

    "display income received to date" when {
      "income source is employment" in {
        doc must haveHeadingH2WithText(messages("tai.income.details.incomeReceivedToDate"))
        doc must haveParagraphWithText(
          messages(
            "tai.income.details.incomeReceivedToDate.desc",
            model.htmlNonBroken(model.startOfCurrentYear).replaceAll("\u00A0", " ")
          )
        )
        doc must haveSpanWithText("£" + model.incomeReceivedToDate)
        doc must haveLinkWithUrlWithID(
          "viewIncomeReceivedToDate",
          controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(model.empId).url
        )
      }

      "income source is pension" in {
        pensionDoc must haveHeadingH2WithText(messages("tai.income.details.incomeReceivedToDate"))
        pensionDoc must haveParagraphWithText(
          messages(
            "tai.income.details.incomeReceivedToDate.desc",
            model.htmlNonBroken(model.startOfCurrentYear).replaceAll("\u00A0", " ")
          )
        )
        pensionDoc must haveSpanWithText("£" + pensionModel.incomeReceivedToDate)
        pensionDoc must haveLinkWithUrlWithID(
          "viewIncomeReceivedToDate",
          controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(pensionModel.empId).url
        )
      }

      "rti is unavailable display rti down messages for employments" in {
        val model = IncomeSourceSummaryViewModel(
          1,
          "User Name",
          "Employer",
          Some(100),
          400,
          Some("1100L"),
          "EMPLOYER-1122",
          isPension = false,
          estimatedPayJourneyCompleted = true,
          rtiAvailable = false,
          taxDistrictNumber = "123",
          payeNumber = "AB12345"
        )

        val doc = Jsoup.parse(template(model).toString())
        doc must haveSpanWithText(messages("tai.rti.down"))
        doc must haveSpanWithText(messages("tai.rti.down.updateEmployment"))
      }

      "rti is unavailable display rti down messages for pensions" in {
        val model = IncomeSourceSummaryViewModel(
          1,
          "User Name",
          "Employer",
          Some(100),
          400,
          Some("1100L"),
          "EMPLOYER-1122",
          isPension = true,
          estimatedPayJourneyCompleted = true,
          rtiAvailable = false,
          taxDistrictNumber = "123",
          payeNumber = "AB12345"
        )

        val doc = Jsoup.parse(template(model).toString())
        doc must haveSpanWithText(messages("tai.rti.down"))
        doc must haveSpanWithText(messages("tai.rti.down.updatePension"))
      }
    }

    "display tax code" in {
      doc must haveHeadingH2WithText(messages("tai.taxCode"))
      doc must haveSpanWithText(model.taxCode.get)
      doc must haveLinkWithUrlWithID("understandTaxCode", routes.YourTaxCodeController.taxCode(model.empId).url)
    }

    "display payroll number" when {
      "income source is employment" in {
        doc must haveHeadingH2WithText(messages("tai.payRollNumber"))
        doc must haveParagraphWithText(model.pensionOrPayrollNumber)
      }

      "display ERN number" when {
        "income source is employment" in {
          doc must haveHeadingH3WithText(messages("tai.income.details.ERN"))
          doc must haveParagraphWithText(s"${model.taxDistrictNumber}/${model.payeNumber}")
        }
      }

      "display ERN number" when {
        "income source is pension" in {
          doc must haveHeadingH3WithText(messages("tai.income.details.ERN"))
          doc must haveParagraphWithText(s"${model.taxDistrictNumber}/${model.payeNumber}")
        }
      }

      "income source is pension" in {
        pensionDoc must haveHeadingH2WithText(messages("tai.pensionNumber"))
        pensionDoc must haveParagraphWithText(pensionModel.pensionOrPayrollNumber)
      }
    }
  }

}
