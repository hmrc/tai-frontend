/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util

object PathPatternMatcher {

  def patternMatches(pattern: String, path: String): Boolean = {
    val patternTokens = split(pattern)
    val pathTokens    = split(path)

    patternTokens.zipAll(pathTokens, "", "").forall {
      case (token, actual) if isParam(token) && actual.nonEmpty => true
      case (token, actual)                                      => token == actual
    }
  }

  private def split(s: String): Array[String] =
    s.split("/").filter(_.nonEmpty)

  private def isParam(token: String): Boolean =
    token.nonEmpty && token.charAt(0) == ':'
}
