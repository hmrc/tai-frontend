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

package controllers

import builders.UserBuilder
import data.TaiData
import uk.gov.hmrc.tai.model.SessionData
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.domain.{Generator, Nino}
import play.api.http.Status._
import uk.gov.hmrc.tai.model.{CeasedEmploymentDetails, SessionData, TaiRoot}
import uk.gov.hmrc.tai.util.TaiConstants.CEASED_MINUS_TWO
import play.api.mvc.Results.Ok

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ServiceChecksSpec extends PlaySpec with FakeTaiPlayApplication {

  implicit val hc = HeaderCarrier()
  implicit val user = UserBuilder.apply()

  val nino = new Generator().nextNino

  val testTaxSummary = TaiData.getHigherRateTaxSummary.copy(ceasedEmploymentDetail = Some(CeasedEmploymentDetails(None, None, Some(CEASED_MINUS_TWO), None)))

  val defineTaiRoot = (mci:Boolean, di: Option[Boolean]) => TaiRoot(manualCorrespondenceInd = mci, deceasedIndicator = di)

  val sessionData = (deceasedIndicator: Option[Boolean])  => SessionData(nino=nino.nino,  taiRoot = Some(defineTaiRoot(false, deceasedIndicator)), taxSummaryDetailsCY = testTaxSummary)
  implicit val timeout = 16


  "CeasedEmploymentOrDeceasedCheck " should {

    "redirect the user to WDYWTD page if user is ceased" in {
      val result = ServiceChecks.executeWithServiceChecks(nino = nino, checkType = SimpleServiceCheck,
        sessionData = sessionData(Some(false)), ignore = false)(custom = None)(user, hc)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).getOrElse("") mustBe routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage.url
    }

    "redirect the user to deceased page if user is deceased indicator is true" in {
      val result = ServiceChecks.executeWithServiceChecks(nino = nino, checkType = SimpleServiceCheck,
        sessionData = sessionData(Some(true)), ignore = false)(custom = None)(user, hc)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).getOrElse("") mustBe routes.DeceasedController.deceased.url
    }
  }

  "personDetailsCheck in ServiceCheckLite" should {
    "redirect users" when {
      "deceased indicator is true for the user" in {
        implicit val taiRoot = defineTaiRoot(true, Some(true))
        val result = ServiceCheckLite.personDetailsCheck{
          Future.successful(Ok("test"))
        }

        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe routes.DeceasedController.deceased.url
      }

      "MCI indicator is true for the user" in {
        implicit val taiRoot = defineTaiRoot(true, Some(false))
        val result = ServiceCheckLite.personDetailsCheck{
          Future.successful(Ok("test"))
        }

        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe routes.ServiceController.gateKeeper.url
      }
    }

    "not be redirected" when {
      "deceased indicator and MCI is false" in {
        implicit val taiRoot = defineTaiRoot(false, Some(false))
        val result = ServiceCheckLite.personDetailsCheck{
          Future.successful(Ok("test"))
        }

        status(result) mustBe OK
      }
    }
  }

}
