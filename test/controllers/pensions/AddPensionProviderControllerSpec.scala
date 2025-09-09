/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.pensions

import builders.RequestBuilder
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, times, verify, when}
import pages.addPensionProvider.*
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.pensions.PensionAddDateForm
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.AddPensionProvider
import uk.gov.hmrc.tai.service.*
import uk.gov.hmrc.tai.util.constants.AddPensionNumberConstants.*
import uk.gov.hmrc.tai.util.constants.{AddPensionFirstPayChoiceConstants, AuditConstants, FormValuesConstants}
import utils.{FakeAuthJourney, NewCachingBaseSpec}
import views.html.CanWeContactByPhoneView
import views.html.pensions.*

import java.time.LocalDate
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class AddPensionProviderControllerSpec extends NewCachingBaseSpec {

  private def createSUT(userAnswersAsArg: Option[UserAnswers]) = new SUT(userAnswersAsArg)

  val pensionProviderService: PensionProviderService = mock[PensionProviderService]
  val auditService: AuditService                     = mock[AuditService]
  val personService: PersonService                   = mock[PersonService]

  private class SUT(
    userAnswersAsArg: Option[UserAnswers] = None
  ) extends AddPensionProviderController(
        pensionProviderService,
        auditService,
        mock[AuditConnector],
        new FakeAuthJourney(userAnswersAsArg.getOrElse(userAnswers)),
        mcc,
        inject[CanWeContactByPhoneView],
        inject[AddPensionConfirmationView],
        inject[AddPensionCheckYourAnswersView],
        inject[AddPensionNumberView],
        inject[AddPensionErrorView],
        inject[AddPensionReceivedFirstPayView],
        inject[AddPensionNameView],
        inject[AddPensionStartDateView],
        mockRepository
      ) {}

  val userAnswers: UserAnswers = UserAnswers(
    RequestBuilder.uuid,
    nino,
    Json.obj(
      "pensionProviderName" -> "TEST-Employer"
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository)
    when(mockRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(userAnswers)))
  }

  "addPensionProviderName" must {
    "show the pensionProvider name form page" when {
      "the request has an authorised session and no previous value in cache" in {
        val sut = createSUT(Some(userAnswers))

        val result = sut.addPensionProviderName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title()  must include(Messages("tai.addPensionProvider.addNameForm.title"))
        doc.toString must not include "testPensionName123"
      }
    }
  }

  "addPensionProviderName" must {
    "show the pensionProvider name form page" when {
      "the request has an authorised session and previous value exists in cache" in {
        val expectedName = "testPensionName123"
        val sut          =
          createSUT(Some(userAnswers.copy(data = Json.obj(AddPensionProviderNamePage.toString -> expectedName))))

        val result = sut.addPensionProviderName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title()  must include(Messages("tai.addPensionProvider.addNameForm.title"))
        doc.toString must include(expectedName)
      }
    }
  }

  "submitPensionProviderName" must {
    "redirect to the received first pay page" when {
      "the form submission is valid" in {
        val sut = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString         -> "pensionProvider",
                AddPensionProviderFirstPaymentPage.toString -> ""
              )
            )
          )
        )

        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))

        val result = sut.submitPensionProviderName()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(("pensionProviderName", "the pension provider"))
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.receivedFirstPay().url
      }
    }

    "reload the page with errors" when {
      "the form entry is invalid" in {
        val sut    = createSUT(Some(userAnswers))
        val result = sut.submitPensionProviderName()(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(("pensionProviderName", ""))
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.addNameForm.title"))
      }
    }

    "store the pensionProvider name in the cache" when {
      "the name is valid" in {
        val sut                 = createSUT(Some(userAnswers))
        val pensionProviderName = "the pension provider"

        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        Await.result(
          sut.submitPensionProviderName()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(("pensionProviderName", pensionProviderName))
          ),
          5 seconds
        )
      }
    }
  }

  "receivedFirstPay" must {
    "show the first pay choice page" when {
      "the request has an authorised session and no previous value is held in the cache" in {
        val pensionProviderName = "Pension Provider"
        val sut                 =
          createSUT(Some(userAnswers.copy(data = Json.obj(AddPensionProviderNamePage.toString -> pensionProviderName))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
      }

      "the request has an authorised session and a previous value of 'No' is held in the cache" in {
        val pensionProviderName = "Pension Provider"
        val sut                 = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString         -> pensionProviderName,
                AddPensionProviderFirstPaymentPage.toString -> FormValuesConstants.NoValue
              )
            )
          )
        )

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithOnlySession("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
        doc.select("input[id=firstPayChoice-2][checked]").size() mustBe 1
      }

      "the request has an authorised session and a previous value of 'Yes' is held in the cache" in {
        val pensionProviderName = "Pension Provider"
        val sut                 = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString         -> pensionProviderName,
                AddPensionProviderFirstPaymentPage.toString -> FormValuesConstants.YesValue
              )
            )
          )
        )

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
        doc.select("input[id=firstPayChoice][checked]").size() mustBe 1
      }

      "redirect to the tax summary page if a value is missing from the cache " in {

        val sut = createSUT(Some(userAnswers.copy(data = Json.obj("invalidData" -> "invalidData"))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

      }

    }
  }

  "submit first pay choice" must {

    "redirect user to first payment date page" when {
      "yes is selected" in {
        val sut = createSUT(Some(userAnswers))

        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))

        val result = sut.submitFirstPay()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(AddPensionFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.YesValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate().url
      }
    }

    "redirect user to an error page" when {
      "no is selected (indicating no payment has yet been received)" in {
        val sut = createSUT(Some(userAnswers))

        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))

        val result = sut.submitFirstPay()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(AddPensionFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.NoValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController
          .cantAddPension()
          .url
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT(Some(userAnswers))

        val result = sut.submitFirstPay()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(AddPensionFirstPayChoiceConstants.FirstPayChoice -> "")
        )
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
      }
    }
    "raise an audit event" when {
      "no is selected" in {
        val sut = createSUT(Some(userAnswers))

        Await.result(
          sut.cantAddPension()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(AddPensionFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.NoValue)
          ),
          5 seconds
        )

        verify(auditService, times(1)).createAndSendAuditEvent(
          meq(AuditConstants.AddPensionCantAddPensionProvider),
          meq(Map("nino" -> authedUser.nino.nino))
        )(any(), any())
      }
    }
  }

  "addPensionProviderStartDate" must {
    "show the pension start date form page" when {
      "the request has an authorised session and no previously cached date present" in {
        val sut = createSUT(Some(userAnswers))

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title()  must include(Messages("tai.addPensionProvider.startDateForm.pagetitle"))
        doc.toString must not include "2037"
      }

      "the request has an authorised session and a previously cached date is present" in {
        val pensionProviderName = "TEST"
        val sut                 = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString      -> pensionProviderName,
                AddPensionProviderStartDatePage.toString -> "2037-01-18"
              )
            )
          )
        )

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title()  must include(Messages("tai.addPensionProvider.startDateForm.pagetitle"))
        doc.toString must include("2037")
      }

      "redirect to the tax summary page if pension name is missing from the cache " in {
        val sut = createSUT(Some(userAnswers.copy(data = Json.obj("Missing" -> "Data"))))

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

      }
    }
  }

  "submit start date" must {
    "return redirect" when {
      "form is valid" in {
        val sut = createSUT(Some(userAnswers))

        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))

        val result =
          sut.submitPensionProviderStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                PensionAddDateForm.PensionFormDay   -> "09",
                PensionAddDateForm.PensionFormMonth -> "06",
                PensionAddDateForm.PensionFormYear  -> "2017"
              )
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url
      }
    }

    "return bad request" when {
      "form is invalid" in {
        val sut = createSUT(Some(userAnswers))

        val result =
          sut.submitPensionProviderStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                PensionAddDateForm.PensionFormDay   -> "01",
                PensionAddDateForm.PensionFormMonth -> "02",
                PensionAddDateForm.PensionFormYear  -> (LocalDate.now().getYear + 1).toString
              )
          )

        status(result) mustBe BAD_REQUEST
      }
    }

    "save details in cache" when {
      "form is valid" in {
        val sut = createSUT(Some(userAnswers))

        when(
          mockRepository.set(any())
        )
          .thenReturn(Future.successful(true))

        Await.result(
          sut.submitPensionProviderStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                PensionAddDateForm.PensionFormDay   -> "01",
                PensionAddDateForm.PensionFormMonth -> "02",
                PensionAddDateForm.PensionFormYear  -> "2017"
              )
          ),
          5 seconds
        )

//        verify(mockRepository, times(1)).set(
//          meq(
//            userAnswers.copy(data =
//              Json.obj(
//                AddPensionProviderNamePage.toString      -> "TEST-Employer",
//                AddPensionProviderStartDatePage.toString -> "2017-02-01"
//              )
//            )
//          )
//        )
      }
    }
  }

  "add pension number" must {
    "show the add pension number page" when {
      "the request has an authorised session and no previously cached pension number present" in {
        val sut = createSUT(Some(userAnswers))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
      }

      "the request has an authorised session and previously cached pension number choice is 'No', and no payroll number is held in cache" in {
        val pensionProviderName = "TEST"
        val sut                 = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString                -> pensionProviderName,
                AddPensionProviderPayrollNumberChoicePage.toString -> FormValuesConstants.NoValue
              )
            )
          )
        )

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-2][checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached pension number choice is 'No', and a payroll number is held in cache" in {
        val pensionProviderName = "TEST"
        val sut                 = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString                -> pensionProviderName,
                AddPensionProviderPayrollNumberChoicePage.toString -> FormValuesConstants.NoValue,
                AddPensionProviderPayrollNumberPage.toString       -> Messages("123456789")
              )
            )
          )
        )

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-2][checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached pension number choice is 'Yes' but no payroll number added" in {
        val pensionProviderName = "TEST"

        val sut = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString                -> pensionProviderName,
                AddPensionProviderPayrollNumberChoicePage.toString -> FormValuesConstants.YesValue
              )
            )
          )
        )

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice][checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe ""
      }

      "the request has an authorised session and previously cached pension number choice is 'Yes' and payroll number added" in {
        val pensionProviderName = "TEST"
        val sut                 = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString                -> pensionProviderName,
                AddPensionProviderPayrollNumberChoicePage.toString -> FormValuesConstants.YesValue,
                AddPensionProviderPayrollNumberPage.toString       -> Messages("123456789")
              )
            )
          )
        )

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice][checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe "123456789"
      }
    }
  }

  "submit pension number" must {
    "cache pension number" when {
      "the form is valid and user knows their pension number" in {
        val sut       = createSUT(Some(userAnswers))
        val payrollNo = "1234"
        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))
        Await.result(
          sut.submitPensionNumber()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                PayrollNumberChoice -> FormValuesConstants.YesValue,
                PayrollNumberEntry  -> payrollNo
              )
          ),
          5 seconds
        )
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user knows their pension number" in {
        val sut       = createSUT(Some(userAnswers))
        val payrollNo = "1234"
        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))

        val result = sut.submitPensionNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              PayrollNumberChoice -> FormValuesConstants.YesValue,
              PayrollNumberEntry  -> payrollNo
            )
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url
      }
    }

    "cache pension number as not known value" when {
      "the form is valid and user doesn't know its pension number" in {
        val sut = createSUT(Some(userAnswers))

        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        Await.result(
          sut.submitPensionNumber()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.NoValue, PayrollNumberEntry -> "")
          ),
          5 seconds
        )

//        verify(mockRepository, times(1)).set(
//          meq(
//            userAnswers.copy(
//              data = Json.obj(
//                AddPensionProviderNamePage.toString                -> "TEST-Employer",
//                AddPensionProviderPayrollNumberChoicePage.toString -> FormValuesConstants.NoValue,
//                AddPensionProviderPayrollNumberPage.toString       -> "I do not know"
//              )
//            )
//          )
//        )
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user doesn't know its pension number" in {
        val sut = createSUT(Some(userAnswers))

        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))

        val result = sut.submitPensionNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.NoValue, PayrollNumberEntry -> "")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT(Some(userAnswers))

        val result = sut.submitPensionNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.YesValue, PayrollNumberEntry -> "")
        )
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
      }
    }
  }

  "add telephone number" must {
    "show the contact by telephone page" when {
      "the request has an authorised session and no previously cached pension number present" in {
        val sut = createSUT(Some(userAnswers))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'No', and no telephone number is held in cache" in {
        val sut = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString              -> "TEST-pension",
                AddPensionProviderTelephoneQuestionPage.toString -> FormValuesConstants.NoValue
              )
            )
          )
        )

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 1
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'No', and a telephone number is held in cache" in {
        val sut = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString              -> "TEST-pension",
                AddPensionProviderTelephoneQuestionPage.toString -> FormValuesConstants.NoValue,
                AddPensionProviderTelephoneNumberPage.toString   -> "01215485965"
              )
            )
          )
        )

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 1
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'Yes', and no telephone number is held in cache" in {
        val sut = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString              -> "TEST-pension",
                AddPensionProviderTelephoneQuestionPage.toString -> FormValuesConstants.YesValue
              )
            )
          )
        )

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 1
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'Yes', and a telephone number is held in cache" in {
        val sut = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString              -> "TEST-pension",
                AddPensionProviderTelephoneQuestionPage.toString -> FormValuesConstants.YesValue,
                AddPensionProviderTelephoneNumberPage.toString   -> "01215485965"
              )
            )
          )
        )

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Can we call you if we need more information?")

        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 1
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe "01215485965"

      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val sut = createSUT(Some(userAnswers))

        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        val result = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "12345678"
            )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.checkYourAnswers().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val sut = createSUT(Some(userAnswers))

        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))

        val result = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.NoValue,
              FormValuesConstants.YesNoTextEntry -> "this value must not be cached"
            )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.checkYourAnswers().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val sut = createSUT(Some(userAnswers))

        val result = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> ""
            )
        )
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
      "there is a form validation error (additional, controller specific constraint)" in {
        val sut = createSUT(Some(userAnswers))

        val tooFewCharsResult = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "1234"
            )
        )
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc         = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() must include(Messages("tai.canWeContactByPhone.title"))

        val tooManyCharsResult = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "1234123412341234123412341234123"
            )
        )
        status(tooManyCharsResult) mustBe BAD_REQUEST
        val tooManyDoc         = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooManyDoc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
    }
  }

  "check your answers" must {
    "show the check answers summary page" when {
      "the request has an authorised session" in {
        val pensionName           = "a pension provider"
        val pensionDate           = "2017-06-09"
        val pensionPayrollNumber  = "pension-ref-1234"
        val telephoneQuestionPage = "Yes"
        val telephoneNumberPage   = "123456789"

        val sut = createSUT(
          Some(
            userAnswers.copy(data =
              Json.obj(
                AddPensionProviderNamePage.toString              -> pensionName,
                AddPensionProviderStartDatePage.toString         -> pensionDate,
                AddPensionProviderPayrollNumberPage.toString     -> pensionPayrollNumber,
                AddPensionProviderTelephoneQuestionPage.toString -> telephoneQuestionPage,
                AddPensionProviderTelephoneNumberPage.toString   -> telephoneNumberPage
              )
            )
          )
        )

        val result = sut.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }

    "redirect to the tax summary page if a value is missing from the cache " in {

      val sut = createSUT(Some(userAnswers))

      val result = sut.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }
  }

  "submit your answers" must {
    "redirect to the confirmation page" in {
      val pensionName           = "a pension provider"
      val pensionDate           = "2017-06-09"
      val pensionPayrollNumber  = "pension-ref-1234"
      val telephoneQuestionPage = "Yes"
      val telephoneNumberPage   = "123456789"
      val sut                   = createSUT(
        Some(
          userAnswers.copy(data =
            Json.obj(
              AddPensionProviderNamePage.toString              -> pensionName,
              AddPensionProviderStartDatePage.toString         -> pensionDate,
              AddPensionProviderPayrollNumberPage.toString     -> pensionPayrollNumber,
              AddPensionProviderTelephoneQuestionPage.toString -> telephoneQuestionPage,
              AddPensionProviderTelephoneNumberPage.toString   -> telephoneNumberPage
            )
          )
        )
      )

      val expectedModel = AddPensionProvider(
        pensionName,
        LocalDate.parse(pensionDate),
        pensionPayrollNumber,
        telephoneQuestionPage,
        Some(telephoneNumberPage)
      )

      when(pensionProviderService.addPensionProvider(any(), meq(expectedModel))(any(), any()))
        .thenReturn(Future.successful("envelope-123"))
      when(mockRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(mockRepository.set(any())).thenReturn(Future.successful(true))
      val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.confirmation().url
    }
  }

  "confirmation" must {
    "show the add pension confirmation page" in {
      val sut = createSUT(Some(userAnswers))

      val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      val doc    = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.pensionConfirmation.heading"))
    }
  }

  "cancel" must {
    "redirect to the the TaxAccountSummaryController" in {

      when(mockRepository.clear(any(), any())).thenReturn(Future.successful(true))

      val result = createSUT(Some(userAnswers)).cancel()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }
  }

}
