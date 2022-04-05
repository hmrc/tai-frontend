/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service.journeyCache

import java.time.LocalDate
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.JourneyCacheConnector
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import utils.BaseSpec
import cats.syntax.either._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class JourneyCacheServiceSpec extends BaseSpec with BeforeAndAfterEach {

  override def beforeEach: Unit =
    Mockito.reset(journeyCacheConnector)

  "current value methods" must {

    "return the cached value as an instance of the relevant type" in {
      val sut = createSut
      when(
        journeyCacheConnector.currentValueAs[String](Matchers.eq(sut.journeyName), Matchers.eq("stringKey"), any())(
          any())).thenReturn(Future.successful(Some("found")))
      when(journeyCacheConnector.currentValueAs[Int](Matchers.eq(sut.journeyName), Matchers.eq("intKey"), any())(any()))
        .thenReturn(Future.successful(Some(1)))
      when(
        journeyCacheConnector.currentValueAs[Boolean](Matchers.eq(sut.journeyName), Matchers.eq("boolKey"), any())(
          any())).thenReturn(Future.successful(Some(true)))
      when(
        journeyCacheConnector.currentValueAs[LocalDate](Matchers.eq(sut.journeyName), Matchers.eq("dateKey"), any())(
          any())).thenReturn(Future.successful(Some(LocalDate.parse("2017-10-10"))))
      Await.result(sut.currentValue("stringKey"), 5 seconds) mustBe Some("found")
      Await.result(sut.currentValueAsInt("intKey"), 5 seconds) mustBe Some(1)
      Await.result(sut.currentValueAsBoolean("boolKey"), 5 seconds) mustBe Some(true)
      Await.result(sut.currentValueAsDate("dateKey"), 5 seconds) mustBe Some(LocalDate.parse("2017-10-10"))
      Await.result(sut.currentValueAs[Int]("intKey", s => s.toInt), 5 seconds) mustBe Some(1)
    }
  }

  "mandatory value methods" must {

    "return the value where found" in {
      val sut = createSut
      when(
        journeyCacheConnector
          .mandatoryJourneyValueAs[String](Matchers.eq(sut.journeyName), Matchers.eq("stringKey"), any())(any()))
        .thenReturn(Future.successful(Right("found")))
      when(
        journeyCacheConnector.mandatoryJourneyValueAs[Int](Matchers.eq(sut.journeyName), Matchers.eq("intKey"), any())(
          any()))
        .thenReturn(Future.successful(Right(1)))
      when(
        journeyCacheConnector
          .mandatoryJourneyValueAs[Boolean](Matchers.eq(sut.journeyName), Matchers.eq("boolKey"), any())(any()))
        .thenReturn(Future.successful(Right(true)))
      when(
        journeyCacheConnector
          .mandatoryJourneyValueAs[LocalDate](Matchers.eq(sut.journeyName), Matchers.eq("dateKey"), any())(any()))
        .thenReturn(Future.successful(Right(LocalDate.parse("2017-10-10"))))
      Await.result(sut.mandatoryJourneyValue("stringKey"), 5 seconds) mustBe "found".asRight
      Await.result(sut.mandatoryJourneyValueAsInt("intKey"), 5 seconds) mustBe 1.asRight
      Await.result(sut.mandatoryValueAsBoolean("boolKey"), 5 seconds) mustBe true.asRight
      Await.result(sut.mandatoryValueAsDate("dateKey"), 5 seconds) mustBe LocalDate.parse("2017-10-10").asRight
      Await.result(sut.mandatoryJourneyValueAs[Int]("intKey", s => s.toInt), 5 seconds) mustBe 1.asRight
    }

    "throw a runtime where not found" in {
      val sut = createSut
      val failed = Future.failed(new RuntimeException("not found"))
      when(
        journeyCacheConnector
          .mandatoryJourneyValueAs[String](Matchers.eq(sut.journeyName), Matchers.eq("stringKey"), any())(any()))
        .thenReturn((failed))
      when(
        journeyCacheConnector.mandatoryJourneyValueAs[Int](Matchers.eq(sut.journeyName), Matchers.eq("intKey"), any())(
          any()))
        .thenReturn((failed))
      when(
        journeyCacheConnector
          .mandatoryJourneyValueAs[Boolean](Matchers.eq(sut.journeyName), Matchers.eq("boolKey"), any())(any()))
        .thenReturn((failed))
      when(
        journeyCacheConnector
          .mandatoryJourneyValueAs[LocalDate](Matchers.eq(sut.journeyName), Matchers.eq("dateKey"), any())(any()))
        .thenReturn((failed))
      val thrown1 = the[RuntimeException] thrownBy Await.result(sut.mandatoryJourneyValue("stringKey"), 5 seconds)
      val thrown2 = the[RuntimeException] thrownBy Await.result(sut.mandatoryJourneyValueAsInt("intKey"), 5 seconds)
      val thrown3 = the[RuntimeException] thrownBy Await.result(sut.mandatoryValueAsBoolean("boolKey"), 5 seconds)
      val thrown4 = the[RuntimeException] thrownBy Await.result(sut.mandatoryValueAsDate("dateKey"), 5 seconds)
      val thrown5 = the[RuntimeException] thrownBy Await
        .result(sut.mandatoryJourneyValueAs[Int]("intKey", s => s.toInt), 5 seconds)
      thrown1.getMessage mustBe "not found"
      thrown2.getMessage mustBe "not found"
      thrown3.getMessage mustBe "not found"
      thrown4.getMessage mustBe "not found"
      thrown5.getMessage mustBe "not found"
    }
  }

  "mandatoryJourneyValues method (collection retrieval)" must {

    "return a sequence of all retrieved values" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))
      Await.result(sut.mandatoryJourneyValues("key1", "key2"), 5 seconds) mustBe Right(Seq("val1", "val2"))
    }

    "return an error message when a mandatory value is missing in the cache" in {
      val sut = createSut

      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(Map.empty[String, String]))
      Await.result(sut.mandatoryJourneyValues("key1", "key2"), 5 seconds) mustBe Left(
        "Mandatory values missing from cache")
    }

  }

//  "mandatoryJourneyValues method (collection retrieval)" must {
//
//    "return a sequence of all retrieved values" in {
//      val sut = createSut
//      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
//        .thenReturn(Future.successful(testCache))
//      Await.result(sut.mandatoryJourneyValues("key1", "key2"), 5 seconds) mustBe Seq("val1", "val2")
//    }
//
//    "throw a runtime exception if one or more of the requested values is not found" in {
//      val sut = createSut
//      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
//        .thenReturn(Future.successful(testCache))
//      val thrown = the[RuntimeException] thrownBy Await
//        .result(sut.mandatoryJourneyValues("key1", "doesntexist"), 5 seconds)
//      thrown.getMessage mustBe "The mandatory value under key 'doesntexist' was not found in the journey cache for 'fakeJourneyName'"
//    }
//
//    "throw a runtime exception if one or more of the requested values is the empty string" in {
//      val sut = createSut
//      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
//        .thenReturn(Future.successful(testCache))
//      val thrown = the[RuntimeException] thrownBy Await.result(sut.mandatoryJourneyValues("key1", "key3"), 5 seconds)
//      thrown.getMessage mustBe "The mandatory value under key 'key3' was not found in the journey cache for 'fakeJourneyName'"
//    }
//  }

  "Mandatory Journey value" must {

    "return a value" in {
      val sut = createSut
      val cacheValue = "val1"

      when(
        journeyCacheConnector.mandatoryJourneyValueAs[String](
          Matchers.eq(sut.journeyName),
          Matchers.eq("key1"),
          Matchers.any[Function1[String, String]]())(any()))
        .thenReturn(Future.successful(Right(cacheValue)): Future[Either[String, String]])

      Await.result(sut.mandatoryJourneyValue("key1"), 5 seconds) mustBe Right(cacheValue)
    }

    "return an error message when a mandatory value is missing in the cache" in {
      val sut = createSut
      val errorMessage = "Value missing from cache"

      when(
        journeyCacheConnector.mandatoryJourneyValueAs[String](
          Matchers.eq(sut.journeyName),
          Matchers.eq("key1"),
          Matchers.any[Function1[String, String]]())(any()))
        .thenReturn(Future.successful(Left(errorMessage)): Future[Either[String, String]])
      Await.result(sut.mandatoryJourneyValue("key1"), 5 seconds) mustBe Left(errorMessage)
    }

  }

  "Mandatory journey values as Int" must {

    "return an Int value" in {
      val sut = createSut
      val id = 1

      when(
        journeyCacheConnector.mandatoryJourneyValueAs[Int](
          Matchers.eq(sut.journeyName),
          Matchers.eq("key1"),
          Matchers.any[Function1[String, Int]]())(any()))
        .thenReturn(Future.successful(Right(id)): Future[Either[String, Int]])
      Await.result(sut.mandatoryJourneyValueAsInt("key1"), 5 seconds) mustBe Right(id)
    }

    "return an error message when a mandatory value is missing in the cache" in {
      val sut = createSut
      val errorMessage = "Value missing from cache"

      when(
        journeyCacheConnector.mandatoryJourneyValueAs[Int](
          Matchers.eq(sut.journeyName),
          Matchers.eq("key1"),
          Matchers.any[Function1[String, Int]]())(any()))
        .thenReturn(Future.successful(Left(errorMessage)): Future[Either[String, Int]])
      Await.result(sut.mandatoryJourneyValueAsInt("key1"), 5 seconds) mustBe Left(errorMessage)
    }

  }

  "collectedJourneyValues method" must {
    "return a sequence of all retrieved mandatory values" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))
      Await.result(sut.collectedJourneyValues(Seq("key1", "key2"), Seq("key4", "key9")), 5 seconds) mustBe Right(
        Seq("val1", "val2"),
        Seq(Some("val3"), None))
    }

    "return left if one or more of the mandatory values is not found" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))

      val message = Await
        .result(sut.collectedJourneyValues(Seq("key1", "key9"), Seq("key4", "key9")), 5 seconds)
      message mustBe Left("Mandatory values missing from cache")
    }

    "throw a runtime exception if one or more of the mandatory values is an empty string" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))

      val message = Await
        .result(sut.collectedJourneyValues(Seq("key1", "key3"), Seq("key4", "key9")), 5 seconds)
      message mustBe Left("Mandatory values missing from cache")
    }

    "return a none when an empty string is found within one of the optional values" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))

      Await.result(sut.collectedJourneyValues(Seq("key1", "key2"), Seq("key4", "key3")), 5 seconds) mustBe Right(
        Seq("val1", "val2"),
        Seq(Some("val3"), None))
    }
  }

  "collected journey values method" must {

    "return a sequence of all retrieved mandatory values" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))
      Await.result(sut.collectedJourneyValues(Seq("key1", "key2"), Seq("key4", "key9")), 5 seconds) mustBe
        Right(Seq("val1", "val2"), Seq(Some("val3"), None))
    }

    "return an error message if one or more of the mandatory values are not found" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))

      Await.result(sut.collectedJourneyValues(Seq("key1", "key9"), Seq("key4", "key9")), 5 seconds) mustBe
        Left("Mandatory values missing from cache")
    }

    "return an error message if one or more of the mandatory values is an empty string" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))

      Await.result(sut.collectedJourneyValues(Seq("key1", "key3"), Seq("key4", "key9")), 5 seconds) mustBe
        Left("Mandatory values missing from cache")
    }

    "return a none when an empty string is found within one of the optional values" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))

      Await.result(sut.collectedJourneyValues(Seq("key1", "key2"), Seq("key4", "key3")), 5 seconds) mustBe
        Right(Seq("val1", "val2"), Seq(Some("val3"), None))
    }

  }

  "optionalValues" must {
    "return sequence of strings when we have values in cache" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))

      Await.result(sut.optionalValues("key1", "key2"), 5 seconds) mustBe Seq(Some("val1"), Some("val2"))
    }
    "return sequence of strings and a None when we have one value as string and a none" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))

      Await.result(sut.optionalValues("key4", "key3"), 5 seconds) mustBe Seq(Some("val3"), None)
    }
    "return None when we have invalid values passed" in {
      val sut = createSut
      when(journeyCacheConnector.currentCache(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(testCache))

      Await.result(sut.optionalValues("key5", "key6"), 5 seconds) mustBe Seq(None, None)
    }
  }

  "flush the cache" must {
    "remove the cache" in {
      val sut = createSut
      when(journeyCacheConnector.flush(Matchers.eq(sut.journeyName))(any()))
        .thenReturn(Future.successful(TaiSuccessResponse))
      Await.result(sut.flush(), 5 seconds) mustBe TaiSuccessResponse
    }
  }

  val testCache = Map(
    "key1" -> "val1",
    "key2" -> "val2",
    "key3" -> " ",
    "key4" -> "val3"
  )

  private def createSut = new SUT

  val journeyCacheConnector: JourneyCacheConnector = mock[JourneyCacheConnector]

  private class SUT
      extends JourneyCacheService(
        "fakeJourneyName",
        journeyCacheConnector
      )

}
