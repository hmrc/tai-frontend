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

package hmrc.nps2

import org.joda.time.LocalDate
import com.github.nscala_time.time.Imports._

case class NpsPerson (
  nino: String,
  names: Seq[NpsPerson.Name],
  gender: NpsPerson.Gender.Value,
  bornOn: LocalDate,
  diedOn: Option[LocalDate] = None
) {
  val isDeceased = diedOn.isDefined
  val name = names.sorted.reverse.find{_.endedOn.isEmpty}.getOrElse{
    throw new RuntimeException(s"No active names given for person record $nino")
  }
}

object NpsPerson {
  object Honorific extends Enumeration {
    val Mr   = Value(1)
    val Mrs  = Value(2)
    val Miss = Value(3)
    val Ms   = Value(4)
    val Dr   = Value(5)
    val Rev  = Value(6)
    val Dame = Value(7)
    val Lady = Value(8)
    val Lord = Value(9)
    val Sir  = Value(10)
  }

  object Gender extends Enumeration {
    val Male   = Value("M")
    val Female  = Value("F")
  }

  case class Name(
    honorific: Option[NpsPerson.Honorific.Value],
    firstName: String,
    middleName: Option[String],
    lastName: String,
    startedOn: LocalDate,
    endedOn: Option[LocalDate]
  ) extends Ordered[Name] {

    val firstInitial = firstName.head
    val middleInitial = middleName.map(_.head)
    val short = s"$firstInitial $lastName"
    val long = Seq(
      honorific.map(_.toString),
      Some(firstName),
      middleName,
      Some(lastName)
    ).flatten.mkString(" ")

    override def toString = {
      "Name(" ++ long ++ ")"
    }

    def compare(that: Name) =
      (startedOn, endedOn, that.startedOn, that.endedOn) match {
        case (tiS,Some(tiE),taS,Some(taE)) if tiE == taE => tiS compare taS
        case (_,Some(ti),_,Some(ta)) => ti compare ta
        case (_,None,_,Some(_)) => 1
        case (_,Some(_),_,None) => -1
        case (tiS,_,taS,_) => tiS compare taS
      }
  }

}
