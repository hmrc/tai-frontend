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

package uk.gov.hmrc.tai.util

import cats.data.{EitherT, NonEmptyList}
import play.api.mvc.{Call, Result}
import play.api.mvc.Results.Redirect
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}

object FutureOps {

  implicit class FutureEitherStringOps[A](f: Future[Either[String, A]]) {

    def getOrFail(implicit ec: ExecutionContext): Future[A] = f.flatMap {
      case Right(a)  => Future.successful(a)
      case Left(err) => Future.failed(new RuntimeException(err))
    }

    def getOrRedirect(call: Call)(implicit ec: ExecutionContext): Future[Either[Result, A]] = f.map {
      case Right(a) => Right(a)
      case Left(_)  => Left(Redirect(call))
    }
  }

  implicit class AttemptTNel[A](f: Future[A]) {
    def attemptTNel(implicit ec: ExecutionContext): EitherT[Future, NonEmptyList[Throwable], A] =
      f.attemptT.leftMap(NonEmptyList.one)
  }
}
