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

package builders

import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.{Cell, RequestAttrKey}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Cookie, Cookies, Headers}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.sca.models.{PtaMinMenuConfig, WrapperDataResponse}
import uk.gov.hmrc.sca.utils.Keys

import java.util.UUID

object RequestBuilder {

  private val HTTP_VERBS = List("GET", "POST", "PUT", "DELETE")

  val uuid = UUID.randomUUID().toString

  val wrapperDataResponse: WrapperDataResponse = WrapperDataResponse(
    Seq.empty,
    PtaMinMenuConfig("", ""),
    List.empty,
    List.empty,
    None,
    None
  )

  def buildFakeRequestWithOnlySession(method: String) = {
    require(HTTP_VERBS contains method)

    FakeRequest(method = method, path = "")
      .withSession(SessionKeys.sessionId -> s"session-$uuid")
  }

  def buildFakeRequestWithAuth(method: String, headers: (String, String)*) =
    FakeRequest(method = method, path = "")
      .withFormUrlEncodedBody(
        "name"                  -> "test1",
        "description"           -> "description",
        "employmentId"          -> "14",
        "newAmount"             -> "1675",
        "oldAmount"             -> "11",
        "worksNumber"           -> "",
        "startDate"             -> "2013-08-03",
        "endDate"               -> "",
        "isLive"                -> "true",
        "isOccupationalPension" -> "false",
        "hasMultipleIncomes"    -> "true"
      )
      .withSession(SessionKeys.sessionId -> s"session-$uuid")
      .withHeaders(headers: _*)

  def buildFakeRequestWithAuth(method: String) =
    FakeRequest(
      method = method,
      uri = "/",
      headers = Headers(),
      body = AnyContentAsEmpty,
      attrs = TypedMap(
        Keys.wrapperIsAuthenticatedKey -> true,
        Keys.wrapperFilterHasRun       -> true,
        Keys.wrapperDataKey            -> wrapperDataResponse,
        Keys.messageDataKey            -> 0,
        RequestAttrKey.Cookies         -> Cell(Cookies(Seq(Cookie("PLAY_LANG", "en"))))
      )
    ).withFormUrlEncodedBody(
      "name"                  -> "test1",
      "description"           -> "description",
      "employmentId"          -> "14",
      "newAmount"             -> "1675",
      "oldAmount"             -> "11",
      "worksNumber"           -> "",
      "startDate"             -> "2013-08-03",
      "endDate"               -> "",
      "isLive"                -> "true",
      "isOccupationalPension" -> "false",
      "hasMultipleIncomes"    -> "true"
    ).withSession(SessionKeys.sessionId -> s"session-$uuid", SessionKeys.authToken -> "Bearer 1")

  def buildFakeRequestWithAuth(method: String, action: String) =
    FakeRequest(method = method, path = "")
      .withFormUrlEncodedBody(
        "name"                  -> "test1",
        "description"           -> "description",
        "employmentId"          -> "14",
        "newAmount"             -> "1675",
        "oldAmount"             -> "11",
        "worksNumber"           -> "",
        "startDate"             -> "2013-08-03",
        "endDate"               -> "",
        "isLive"                -> "true",
        "action"                -> action,
        "isOccupationalPension" -> "false",
        "hasMultipleIncomes"    -> "true"
      )
      .withSession(SessionKeys.sessionId -> s"session-$uuid", SessionKeys.authToken -> "Bearer 1")

  def buildFakeInvalidRequestWithAuth(method: String) =
    FakeRequest(method = method, path = "")
      .withFormUrlEncodedBody("name" -> "test1", "description" -> "description", "employmentId" -> "14")
      .withSession(SessionKeys.sessionId -> s"session-$uuid", SessionKeys.authToken -> "Bearer 1")

  def buildFakeRequestWithoutAuth(method: String) =
    FakeRequest(method = method, path = "")
      .withFormUrlEncodedBody(
        "name"                  -> "test1",
        "description"           -> "description",
        "employmentId"          -> "14",
        "newAmount"             -> "1675",
        "oldAmount"             -> "11",
        "worksNumber"           -> "",
        "startDate"             -> "2013-08-03",
        "endDate"               -> "",
        "isLive"                -> "true",
        "isOccupationalPension" -> "false",
        "hasMultipleIncomes"    -> "true"
      )
      .withSession(SessionKeys.sessionId -> s"session-$uuid", SessionKeys.authToken -> "Bearer 1")

  def buildFakePostRequestWithAuth(formArgs: (String, String)*) =
    buildFakeRequestWithAuth("POST")
      .withFormUrlEncodedBody(formArgs: _*)
      .withSession(SessionKeys.authToken -> "Bearer 1")

  def buildFakeGetRequestWithAuth(): FakeRequest[AnyContentAsFormUrlEncoded] =
    buildFakeRequestWithAuth("GET").withSession(SessionKeys.authToken -> "Bearer 1")

}
