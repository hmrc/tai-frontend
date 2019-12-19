package uk.gov.hmrc.tai.util

object EitherOpsObject {

  implicit class EitherOps[A, B](e: Either[A, B]) {

    def zip[C](other: Either[A, C]): Either[A, (B, C)] =
      e.right.flatMap(b => other.right.map(c => (b, c)))
  }

}