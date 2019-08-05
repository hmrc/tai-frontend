/*
 * Copyright 2019 HM Revenue & Customs
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

package json

import com.kenshoo.play.metrics.PlayModule
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.test.FakeApplication
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import controllers.viewModels.TupleFormats._

case class ExampleTuple2(a: List[(String, String)])

object ExampleTuple2 {
  implicit val format = Json.format[ExampleTuple2]
}

case class ExampleTuple3(a: List[(String, String, String)])

object ExampleTuple3 {
  implicit val format = Json.format[ExampleTuple3]
}

class JsonTestSpec extends UnitSpec with WithFakeApplication with ScalaFutures {

  override lazy val fakeApplication = FakeApplication()
  override def bindModules = Seq(new PlayModule)

  "JSON serialization/deserialize" should {

    s"Successfully serialize/deserialize Tuple2" in {
      val rawJson = Json.parse("""{"a":[["a","b"],["c","d"]]}""")
      val tuple2 = ExampleTuple2(Seq(("a", "b"), ("c", "d")).toList)
      val jsonObject = Json.toJson(tuple2)

      jsonObject shouldBe rawJson
      rawJson.as[ExampleTuple2] shouldBe tuple2
    }

    s"Successfully serialize/deserialize Tuple3" in {
      val rawJson = Json.parse("""{"a":[["a","b","c"],["d","e","f"]]}""")
      val tuple3 = ExampleTuple3(Seq(("a", "b", "c"), ("d", "e", "f")).toList)
      val jsonObject = Json.toJson(tuple3)

      jsonObject shouldBe rawJson
      rawJson.as[ExampleTuple3] shouldBe tuple3
    }

  }
}
