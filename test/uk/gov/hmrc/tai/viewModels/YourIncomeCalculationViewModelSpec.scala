/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.viewModels.YourIncomeCalculationViewModel.pensionOrEmpMessage

class YourIncomeCalculationViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Your Income Calculation View Model" must {
    "return employment details" when {
      "employment type is Employment Income" in {
        val model = incomeCalculationViewModel()

        model.empId mustBe 2
        model.employerName mustBe "test employment"
        model.payments mustBe Seq(
          PaymentDetailsViewModel(new LocalDate().minusWeeks(1), 100, 50, 25),
          PaymentDetailsViewModel(new LocalDate().minusWeeks(4), 100, 50, 25)
        )
        model.latestPayment mustBe Some(
          LatestPayment(new LocalDate().minusWeeks(1), 400, 50, 25, Irregular)
        )
        model.endDate mustBe None
        model.isPension mustBe false
        model.rtiStatus mustBe Available
        model.employmentStatus mustBe Live
      }

      "employment type is Pension Income" in {
        val model = incomeCalculationViewModel(employmentType = PensionIncome)
        model.isPension mustBe true
      }

      "tax code income is not present" in {
        val model = incomeCalculationViewModel(hasTaxCodeIncome = false)

        model.empId mustBe 2
        model.employerName mustBe "test employment"
        model.payments mustBe Seq(
          PaymentDetailsViewModel(new LocalDate().minusWeeks(1), 100, 50, 25),
          PaymentDetailsViewModel(new LocalDate().minusWeeks(4), 100, 50, 25)
        )
        model.latestPayment mustBe Some(
          LatestPayment(new LocalDate().minusWeeks(1), 400, 50, 25, Irregular)
        )
        model.endDate mustBe None
        model.isPension mustBe false
        model.rtiStatus mustBe Available
        model.employmentStatus mustBe Ceased
      }
    }

    "show message" when {
      "total is not equal for employment" in {
        val model = incomeCalculationViewModel()

        model.messageWhenTotalNotEqual mustBe Some(Messages("tai.income.calculation.totalNotMatching.emp.message"))
      }

      "total is not equal for pension" in {
        val model = incomeCalculationViewModel(employmentType = PensionIncome)

        model.messageWhenTotalNotEqual mustBe Some(Messages("tai.income.calculation.totalNotMatching.pension.message"))
      }
    }

    "doesn't show message" when {
      "total is equal" in {
        val model = incomeCalculationViewModel(
          payments = Seq(firstPayment),
          paymentDetails = Seq(PaymentDetailsViewModel(firstPayment)))

        model.messageWhenTotalNotEqual mustBe None
      }
    }

    "show income calculation and estimation message" when {
      "tax code income is present and frequency is irregular" in {
        val model = incomeCalculationViewModel()

        model.incomeCalculationMessage mustBe ""
        model.incomeCalculationEstimateMessage mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", 1111))
      }

      "tax code income is present and frequency is monthly" in {
        val model = incomeCalculationViewModel(payments = Seq(firstPayment))

        model.incomeCalculationMessage mustBe Messages(
          "tai.income.calculation.rti.midYear.weekly",
          uk.gov.hmrc.tai.model.TaxYear().start.plusDays(1).toString("d MMMM yyyy"),
          firstPayment.date.toString("d MMMM yyyy"),
          MoneyPounds(firstPayment.amountYearToDate, 2).quantity
        )
        model.incomeCalculationEstimateMessage mustBe Some(Messages("tai.income.calculation.rti.emp.estimate", 1111))
      }

      "employment is ceased and end date and cessation pay is defined" in {
        val model =
          incomeCalculationViewModel(payments = Seq(firstPayment), cessationPay = Some(100), employmentStatus = Ceased)

        model.incomeCalculationMessage mustBe Messages(
          "tai.income.calculation.rti.ceased.emp",
          model.endDate.get.toString("d MMMM yyyy"),
          firstPayment.date.toString("d MMMM yyyy"),
          MoneyPounds(firstPayment.amountYearToDate, 2).quantity
        )
        model.incomeCalculationEstimateMessage mustBe None
      }
    }

    "doesn't not show income calculation and estimate message" when {
      "tax code income is not present" in {
        val model = incomeCalculationViewModel(hasTaxCodeIncome = false)

        model.incomeCalculationMessage mustBe ""
        model.incomeCalculationEstimateMessage mustBe None
      }
    }
  }

  "getSameMsg" must {
    "return none" when {
      "amountYearToDate is not same as amount" in {
        val employment =
          Employment("employment", Live, None, TaxYear().start.plusDays(1), None, Nil, "", "", 2, None, false, false)

        YourIncomeCalculationViewModel.sameIncomeCalculationMessage(employment, 100, 1000, "emp", None) mustBe None
      }
    }

    "return message" when {
      "amountYearToDate is not same as amount" in {
        val employment =
          Employment("employment", Live, None, TaxYear().start.minusMonths(1), None, Nil, "", "", 2, None, false, false)

        YourIncomeCalculationViewModel.sameIncomeCalculationMessage(employment, 100, 100, "emp", None) mustBe
          Some(
            messagesApi(
              "tai.income.calculation.rti.emp.same",
              Dates.formatDate(TaxYear().start),
              "",
              MoneyPounds(100, 0).quantity))
      }
    }
  }

  "ceasedIncomeCalculationMessage" must {
    "return message for ceased employment" when {
      "endDate and cessationPay is available" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          Some(100),
          false,
          false)
        CeasedIncomeMessages.ceasedIncomeCalculationMessage(Ceased, employment, "pension") mustBe
          Some(messagesApi("tai.income.calculation.rti.ceased.pension", Dates.formatDate(TaxYear().end)))
      }

      "cessationPay is not available" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        CeasedIncomeMessages.ceasedIncomeCalculationMessage(Ceased, employment, "pension") mustBe
          Some(messagesApi("tai.income.calculation.rti.ceased.pension.noFinalPay"))
      }
    }

    "return message for potentially ceased employment" when {
      "end date is not available" in {
        val employment =
          Employment("employment", Live, None, TaxYear().start.minusMonths(1), None, Nil, "", "", 2, None, false, false)
        CeasedIncomeMessages.ceasedIncomeCalculationMessage(PotentiallyCeased, employment, "pension") mustBe
          Some(messagesApi("tai.income.calculation.rti.ceased.pension.noFinalPay"))
      }
    }

    "return None" when {
      "employment is live" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        CeasedIncomeMessages.ceasedIncomeCalculationMessage(Live, employment, "pension") mustBe None
      }
    }
  }

  "ceasedIncomeCalculationEstimateMessage" must {
    "return message for ceased employment" when {
      "cessationPay is not available" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        CeasedIncomeMessages.ceasedIncomeCalculationEstimateMessage(Ceased, employment, 1000) mustBe
          Some(messagesApi("tai.income.calculation.rti.ceased.noFinalPay.estimate", MoneyPounds(1000, 0).quantity))
      }
    }

    "return message for potentially ceased employment" when {
      "end date is not available" in {
        val employment =
          Employment("employment", Live, None, TaxYear().start.minusMonths(1), None, Nil, "", "", 2, None, false, false)
        CeasedIncomeMessages.ceasedIncomeCalculationEstimateMessage(PotentiallyCeased, employment, 1000) mustBe
          Some(messagesApi("tai.income.calculation.rti.ceased.noFinalPay.estimate", MoneyPounds(1000, 0).quantity))
      }
    }

    "return None" when {
      "employment is live" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        CeasedIncomeMessages.ceasedIncomeCalculationEstimateMessage(Live, employment, 1000) mustBe None
      }
    }
  }

  "payFreqIncomeCalculationMessage" must {
    "return messages for start date before start of tax year" when {
      "payment frequency is monthly" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationMessage(employment, "emp", Some(Monthly), 1000, None) mustBe
          Some(messagesApi("tai.income.calculation.rti.continuous.weekly.emp", MoneyPounds(1000, 2).quantity, ""))
      }

      "payment frequency is Annually" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationMessage(employment, "emp", Some(Annually), 1000, None) mustBe
          Some(messagesApi("tai.income.calculation.rti.continuous.annually.emp", MoneyPounds(1000, 2).quantity))
      }

      "payment frequency is OneOff" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationMessage(employment, "emp", Some(OneOff), 1000, None) mustBe
          Some(messagesApi("tai.income.calculation.rti.oneOff.emp", MoneyPounds(1000, 2).quantity))
      }

      "payment frequency is Irregular" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationMessage(employment, "emp", Some(Irregular), 1000, None) mustBe None
      }
    }

    "return messages for start date after tax of start year" when {
      "payment frequency is monthly" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.plusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationMessage(employment, "emp", Some(Monthly), 1000, None) mustBe
          Some(
            messagesApi(
              "tai.income.calculation.rti.midYear.weekly",
              Dates.formatDate(employment.startDate),
              "",
              MoneyPounds(1000, 2).quantity))
      }

      "payment frequency is Annually" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.plusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationMessage(employment, "emp", Some(Annually), 1000, None) mustBe
          Some(
            messagesApi(
              "tai.income.calculation.rti.midYear.weekly",
              Dates.formatDate(employment.startDate),
              "",
              MoneyPounds(1000, 2).quantity))
      }

      "payment frequency is OneOff" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.plusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationMessage(employment, "emp", Some(OneOff), 1000, None) mustBe
          Some(messagesApi("tai.income.calculation.rti.oneOff.emp", MoneyPounds(1000, 2).quantity))
      }

      "payment frequency is Irregular" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.plusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationMessage(employment, "emp", Some(Irregular), 1000, None) mustBe None
      }
    }

    "return none" when {
      "payment frequency is none" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.plusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        PaymentFrequencyIncomeMessages.payFreqIncomeCalculationMessage(employment, "emp", None, 1000, None) mustBe None
      }
    }
  }

  "payFreqIncomeCalculationEstimateMessage" must {
    "return messages for start date before start of tax year" when {
      "payment frequency is monthly" in {
        PaymentFrequencyIncomeMessages.payFreqIncomeCalculationEstimateMessage("emp", Some(Monthly), None, 1000) mustBe
          Some(messagesApi("tai.income.calculation.rti.emp.estimate", MoneyPounds(1000, 0).quantity))
      }

      "payment frequency is Annually" in {
        PaymentFrequencyIncomeMessages.payFreqIncomeCalculationEstimateMessage("emp", Some(Annually), None, 1000) mustBe
          Some(messagesApi("tai.income.calculation.rti.emp.estimate", MoneyPounds(1000, 0).quantity))
      }

      "payment frequency is OneOff" in {
        PaymentFrequencyIncomeMessages.payFreqIncomeCalculationEstimateMessage("emp", Some(OneOff), None, 1000) mustBe
          Some(messagesApi("tai.income.calculation.rti.emp.estimate", MoneyPounds(1000, 0).quantity))
      }

      "payment frequency is Irregular" in {
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationEstimateMessage("emp", Some(Irregular), None, 1000) mustBe
          Some(messagesApi("tai.income.calculation.rti.irregular.emp", MoneyPounds(1000, 0).quantity))
      }
    }

    "return messages for start date after tax of start year" when {
      "payment frequency is monthly" in {
        PaymentFrequencyIncomeMessages.payFreqIncomeCalculationEstimateMessage("emp", Some(Monthly), None, 1000) mustBe
          Some(messagesApi("tai.income.calculation.rti.emp.estimate", MoneyPounds(1000, 0).quantity))
      }

      "payment frequency is OneOff" in {
        PaymentFrequencyIncomeMessages.payFreqIncomeCalculationEstimateMessage("emp", Some(OneOff), None, 1000) mustBe
          Some(messagesApi("tai.income.calculation.rti.emp.estimate", MoneyPounds(1000, 0).quantity))
      }

      "payment frequency is Irregular" in {
        PaymentFrequencyIncomeMessages
          .payFreqIncomeCalculationEstimateMessage("emp", Some(Irregular), None, 1000) mustBe
          Some(messagesApi("tai.income.calculation.rti.irregular.emp", MoneyPounds(1000, 0).quantity))
      }
    }

    "return none" when {
      "payment frequency is none" in {
        PaymentFrequencyIncomeMessages.payFreqIncomeCalculationEstimateMessage("emp", None, None, 1000) mustBe None
      }
    }
  }

  "manualUpdateIncomeCalculationMessage" must {
    "return messages for Manual telephone" when {
      "updateNotificationDate and updateActionDate is available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(ManualTelephone),
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(
            messagesApi(
              "tai.income.calculation.manual.update.phone",
              Dates.formatDate(taxCodeIncome.updateActionDate.get),
              Dates.formatDate(taxCodeIncome.updateNotificationDate.get)
            ))
      }

      "updateNotificationDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(ManualTelephone),
          None,
          Some(new LocalDate()))
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.phone.withoutDate"))
      }

      "updateActionDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(ManualTelephone),
          Some(new LocalDate().minusWeeks(4)),
          None
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.phone.withoutDate"))
      }

      "updateActionDate and updateNotificationDate are not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(ManualTelephone),
          None,
          None)
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.phone.withoutDate"))
      }
    }

    "return messages for letter" when {
      "updateNotificationDate and updateActionDate is available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Letter),
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(
            messagesApi(
              "tai.income.calculation.manual.update.letter",
              Dates.formatDate(taxCodeIncome.updateActionDate.get),
              Dates.formatDate(taxCodeIncome.updateNotificationDate.get)
            ))
      }

      "updateNotificationDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Letter),
          None,
          Some(new LocalDate()))
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.letter.withoutDate"))
      }

      "updateActionDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Letter),
          Some(new LocalDate().minusWeeks(4)),
          None
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.letter.withoutDate"))
      }

      "updateActionDate and updateNotificationDate are not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Letter),
          None,
          None)
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.letter.withoutDate"))
      }
    }

    "return messages for Email" when {
      "updateNotificationDate and updateActionDate is available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Email),
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(
            messagesApi(
              "tai.income.calculation.manual.update.email",
              Dates.formatDate(taxCodeIncome.updateActionDate.get),
              Dates.formatDate(taxCodeIncome.updateNotificationDate.get)
            ))
      }

      "updateNotificationDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Email),
          None,
          Some(new LocalDate()))
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.email.withoutDate"))
      }

      "updateActionDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Email),
          Some(new LocalDate().minusWeeks(4)),
          None
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.email.withoutDate"))
      }

      "updateActionDate and updateNotificationDate are not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Email),
          None,
          None)
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.email.withoutDate"))
      }
    }

    "return messages for AgentContact" when {
      "1" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(AgentContact),
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe Some(
          messagesApi("tai.income.calculation.agent"))
      }
    }

    "return messages for OtherForm" when {
      "updateNotificationDate and updateActionDate is available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(OtherForm),
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(
            messagesApi(
              "tai.income.calculation.manual.update.informationLetter",
              Dates.formatDate(taxCodeIncome.updateActionDate.get),
              Dates.formatDate(taxCodeIncome.updateNotificationDate.get)
            ))
      }

      "updateNotificationDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(OtherForm),
          None,
          Some(new LocalDate()))
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.informationLetter.withoutDate"))
      }

      "updateActionDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(OtherForm),
          Some(new LocalDate().minusWeeks(4)),
          None
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.informationLetter.withoutDate"))
      }

      "updateActionDate and updateNotificationDate are not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(OtherForm),
          None,
          None)
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.informationLetter.withoutDate"))
      }
    }

    "return messages for InformationLetter" when {
      "updateNotificationDate and updateActionDate is available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(InformationLetter),
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(
            messagesApi(
              "tai.income.calculation.manual.update.informationLetter",
              Dates.formatDate(taxCodeIncome.updateActionDate.get),
              Dates.formatDate(taxCodeIncome.updateNotificationDate.get)
            ))
      }

      "updateNotificationDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(InformationLetter),
          None,
          Some(new LocalDate())
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.informationLetter.withoutDate"))
      }

      "updateActionDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(InformationLetter),
          Some(new LocalDate().minusWeeks(4)),
          None
        )
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.informationLetter.withoutDate"))
      }

      "updateActionDate and updateNotificationDate are not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(InformationLetter),
          None,
          None)
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.informationLetter.withoutDate"))
      }
    }

    "return messages for Internet" when {
      "updateNotificationDate is available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Internet),
          Some(new LocalDate()),
          None)
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(
            messagesApi(
              "tai.income.calculation.manual.update.internet",
              Dates.formatDate(taxCodeIncome.updateNotificationDate.get)))
      }

      "updateNotificationDate is not available" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(Internet),
          None,
          None)
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe
          Some(messagesApi("tai.income.calculation.manual.update.internet.withoutDate"))
      }
    }

    "return none" when {
      "iabdUpdateSource is none" in {
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          None,
          None,
          None)
        ManualUpdateIncomeMessages.manualUpdateIncomeCalculationMessage(taxCodeIncome) mustBe None
      }
    }
  }

  "manualUpdateIncomeCalculationEstimateMessage" must {
    "return messages is not agent code" in {
      val taxCodeIncome = TaxCodeIncome(
        EmploymentIncome,
        Some(2),
        1111,
        "employment2",
        "150L",
        "test employment",
        Week1Month1BasisOfOperation,
        Live,
        Some(ManualTelephone),
        Some(new LocalDate().minusWeeks(4)),
        Some(new LocalDate())
      )
      ManualUpdateIncomeMessages.manualUpdateIncomeCalculationEstimateMessage(taxCodeIncome) mustBe
        Some(messagesApi("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount))
    }

    "return messages is agent code" in {
      val taxCodeIncome = TaxCodeIncome(
        EmploymentIncome,
        Some(2),
        1111,
        "employment2",
        "150L",
        "test employment",
        Week1Month1BasisOfOperation,
        Live,
        Some(AgentContact),
        Some(new LocalDate().minusWeeks(4)),
        Some(new LocalDate())
      )
      ManualUpdateIncomeMessages.manualUpdateIncomeCalculationEstimateMessage(taxCodeIncome) mustBe
        Some(messagesApi("tai.income.calculation.agent.estimate", taxCodeIncome.amount))
    }
  }

  "incomeExplanationMessage" must {
    "return messages" when {
      "getCeasedMsg returns both messages" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          None,
          None,
          None)
        YourIncomeCalculationViewModel
          .incomeExplanationMessage(Ceased, employment, "pension", taxCodeIncome, None, 1000, None) mustBe (
          (
            Some(messagesApi("tai.income.calculation.rti.ceased.pension.noFinalPay")),
            Some(messagesApi("tai.income.calculation.rti.ceased.noFinalPay.estimate", MoneyPounds(1111, 0).quantity))))
      }

      "getCeasedMsg returns first message" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          Some(100),
          false,
          false)
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          None,
          None,
          None)

        YourIncomeCalculationViewModel
          .incomeExplanationMessage(Ceased, employment, "pension", taxCodeIncome, None, 1000, None) mustBe
          (Some(
            messagesApi(
              "tai.income.calculation.rti.ceased.pension",
              employment.endDate.map(Dates.formatDate).getOrElse(""))), None)
      }

      "getManualUpdateMsg returns both messages" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          Some(ManualTelephone),
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )

        YourIncomeCalculationViewModel
          .incomeExplanationMessage(Live, employment, "pension", taxCodeIncome, None, 1000, None) mustBe
          (Some(
            messagesApi(
              "tai.income.calculation.manual.update.phone",
              Dates.formatDate(taxCodeIncome.updateActionDate.get),
              Dates.formatDate(taxCodeIncome.updateNotificationDate.get)
            )),
          Some(messagesApi("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))
      }

      "getSameMsg returns the first message" in {
        val employment =
          Employment("employment", Live, None, TaxYear().start.minusMonths(1), None, Nil, "", "", 2, None, false, false)
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          None,
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )

        YourIncomeCalculationViewModel
          .incomeExplanationMessage(Live, employment, "emp", taxCodeIncome, None, 1111, None) mustBe
          (Some(
            messagesApi(
              "tai.income.calculation.rti.emp.same",
              Dates.formatDate(TaxYear().start),
              "",
              MoneyPounds(1111, 0).quantity)), None)
      }

      "getPayFreqMsg returns both messages" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          None,
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )

        YourIncomeCalculationViewModel
          .incomeExplanationMessage(Live, employment, "emp", taxCodeIncome, Some(OneOff), 1000, None) mustBe
          (Some(messagesApi("tai.income.calculation.rti.oneOff.emp", MoneyPounds(1000, 2).quantity)),
          Some(messagesApi("tai.income.calculation.rti.emp.estimate", MoneyPounds(1111, 0).quantity)))
      }

      "getPayFreqMsg returns the first message" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          None,
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )

        YourIncomeCalculationViewModel
          .incomeExplanationMessage(Live, employment, "emp", taxCodeIncome, Some(Irregular), 1000, None) mustBe
          (None, Some(messagesApi("tai.income.calculation.rti.irregular.emp", MoneyPounds(1111, 0).quantity)))
      }
    }

    "returns default messages" when {
      "none of the conditions are matched" in {
        val employment = Employment(
          "employment",
          Live,
          None,
          TaxYear().start.minusMonths(1),
          Some(TaxYear().end),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)
        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          Live,
          None,
          Some(new LocalDate().minusWeeks(4)),
          Some(new LocalDate())
        )

        YourIncomeCalculationViewModel
          .incomeExplanationMessage(Live, employment, "emp", taxCodeIncome, None, 1000, None) mustBe
          (Some(messagesApi("tai.income.calculation.default.emp", Dates.formatDate(TaxYear().end))),
          Some(messagesApi("tai.income.calculation.default.estimate.emp", taxCodeIncome.amount)))
      }
    }
  }

  lazy val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  lazy val latestPayment = Payment(new LocalDate().minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)

  val paymentDetails = Seq(
    PaymentDetailsViewModel(latestPayment),
    PaymentDetailsViewModel(firstPayment)
  )

  private def incomeCalculationViewModel(
    realTimeStatus: RealTimeStatus = Available,
    payments: Seq[Payment] = Seq(latestPayment, firstPayment),
    employmentStatus: TaxCodeIncomeSourceStatus = Live,
    employmentType: TaxCodeIncomeComponentType = EmploymentIncome,
    hasTaxCodeIncome: Boolean = true,
    cessationPay: Option[BigDecimal] = None,
    paymentDetails: Seq[PaymentDetailsViewModel] = paymentDetails) = {
    val annualAccount = AnnualAccount("KEY", uk.gov.hmrc.tai.model.TaxYear(), realTimeStatus, payments, Nil)
    val employment = Employment(
      "test employment",
      Live,
      Some("EMPLOYER1"),
      uk.gov.hmrc.tai.model.TaxYear().start.plusDays(1),
      if (employmentStatus == Ceased) Some(LocalDate.parse("2017-08-08")) else None,
      Seq(annualAccount),
      "",
      "",
      2,
      cessationPay,
      false,
      false
    )
    val taxCodeIncome = if (hasTaxCodeIncome) {
      Some(
        TaxCodeIncome(
          employmentType,
          Some(2),
          1111,
          "employment2",
          "150L",
          "test employment",
          Week1Month1BasisOfOperation,
          employmentStatus))
    } else {
      None
    }

    YourIncomeCalculationViewModel(taxCodeIncome, employment, paymentDetails)
  }

}
