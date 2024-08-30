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

package uk.gov.hmrc.tai.model.domain.income

import cats.data.EitherT
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.model.UserAnswers
import pages.income._

import scala.concurrent.{ExecutionContext, Future}

final case class IncomeSource(id: Int, name: String)

object IncomeSource {

  def create(
    journeyCacheNewRepository: JourneyCacheNewRepository,
    userAnswers: UserAnswers
  )(implicit ec: ExecutionContext): Future[Either[String, IncomeSource]] =
    EitherT(
      journeyCacheNewRepository.get(userAnswers.sessionId, userAnswers.nino).map {
        case Some(userAnswers) =>
          val idOpt = (userAnswers.data \ UpdateIncomeIdPage).asOpt[String]
          val nameOpt = (userAnswers.data \ UpdateIncomeNamePage).asOpt[String]

          (idOpt, nameOpt) match {
            case (Some(id), Some(name)) =>
              Right(IncomeSource(id.toInt, name))
            case _ =>
              Left("Mandatory journey values not found")
          }
        case None => Left("Mandatory journey values not found")
      }
    ).value

}
