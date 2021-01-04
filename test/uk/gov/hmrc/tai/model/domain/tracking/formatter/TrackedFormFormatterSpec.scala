/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model.domain.tracking.formatter

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsResultException, Json}
import uk.gov.hmrc.tai.model.domain.tracking._

class TrackedFormFormatterSpec extends PlaySpec with TrackedFormFormatters {

  "trackingReads" should {
    "be able to parse incoming tracking" when {
      "the milestone is Received status" in {
        val json =
          """{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
            "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
            "milestones":[
              {"milestone": "Received","status": "current"},
              {"milestone": "Acquired","status": "incomplete"},
              {"milestone": "InProgress","status": "incomplete"},
              {"milestone": "Done","status": "incomplete"}
            ]}"""
        val tracking = Json.parse(json).as[TrackedForm](trackedFormReads)

        val expectedTracking = TrackedForm("R39_EN", "TES1", TrackedFormReceived)
        tracking mustBe expectedTracking
      }

      "the milestone is Acquired status" in {
        val json =
          """{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
            "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
            "milestones":[
              {"milestone": "Received","status": "complete"},
              {"milestone": "Acquired","status": "current"},
              {"milestone": "InProgress","status": "incomplete"},
              {"milestone": "Done","status": "incomplete"}
            ]}"""
        val tracking = Json.parse(json).as[TrackedForm](trackedFormReads)

        val expectedTracking = TrackedForm("R39_EN", "TES1", TrackedFormAcquired)
        tracking mustBe expectedTracking
      }

      "the milestone is In Progress status" in {
        val json =
          """{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
            "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
            "milestones":[
              {"milestone": "Received","status": "complete"},
              {"milestone": "Acquired","status": "complete"},
              {"milestone": "InProgress","status": "current"},
              {"milestone": "Done","status": "incomplete"}
            ]}"""
        val tracking = Json.parse(json).as[TrackedForm](trackedFormReads)

        val expectedTracking = TrackedForm("R39_EN", "TES1", TrackedFormInProgress)
        tracking mustBe expectedTracking
      }

      "the milestone is Done status" in {
        val json =
          """{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
            "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
            "milestones":[
              {"milestone": "Received","status": "complete"},
              {"milestone": "Acquired","status": "complete"},
              {"milestone": "InProgress","status": "complete"},
              {"milestone": "Done","status": "current"}
            ]}"""
        val tracking = Json.parse(json).as[TrackedForm](trackedFormReads)

        val expectedTracking = TrackedForm("R39_EN", "TES1", TrackedFormDone)
        tracking mustBe expectedTracking
      }

    }

    "throw an exception" when {
      "formId doesn't exist in the json" in {
        val json =
          """{"formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
            "receivedDate":"01 Apr 2016","completionDate":"06 May 2016", "milestones":[{"milestone":"Received","status":"current"}]}"""
        val result = the[JsResultException] thrownBy Json.parse(json).as[TrackedForm](trackedFormReads)
        result.getMessage must include("'formId' is undefined")
      }

      "form name doesn't exist in the json" in {
        val json =
          """{"formId":"R39_EN","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
            "receivedDate":"01 Apr 2016","completionDate":"06 May 2016", "milestones":[{"milestone":"Received","status":"current"}]}"""
        val result = the[JsResultException] thrownBy Json.parse(json).as[TrackedForm](trackedFormReads)
        result.getMessage must include("'formName' is undefined")
      }

      "milestones doesn't exist in the json" in {
        val json = """{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
            "receivedDate":"01 Apr 2016","completionDate":"06 May 2016"}"""
        val result = the[JsResultException] thrownBy Json.parse(json).as[TrackedForm](trackedFormReads)
        result.getMessage must include("'milestones' is undefined")
      }

      "milestones list is empty in the json" in {
        val json = """{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
            "receivedDate":"01 Apr 2016","completionDate":"06 May 2016", "milestones":[]}"""
        val result = the[JsResultException] thrownBy Json.parse(json).as[TrackedForm](trackedFormReads)
        result.getMessage must include("milestones list is empty")
      }

      "the milestone doesn't have an item with current status" in {
        val json =
          """{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
            "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
            "milestones":[
              {"milestone": "Received","status": "complete"},
              {"milestone": "Acquired","status": "complete"},
              {"milestone": "InProgress","status": "complete"},
              {"milestone": "Done","status": "complete"}
            ]}"""
        val result = the[JsResultException] thrownBy Json.parse(json).as[TrackedForm](trackedFormReads)
        result.getMessage must include("there is no milestone with status 'current'")
      }
    }
  }

  "TrackingSequenceReads" should {
    "Return nil" when {
      "The list is empty" in {
        val json =
          """{"submissions":[]}"""

        val trackingFormSequence = Json.parse(json).as[Seq[TrackedForm]](trackedFormSeqReads)

        trackingFormSequence.size mustBe 0
      }
    }
    "Return a list" when {
      "The list has one tracking element" in {
        val json =
          """{"submissions":[{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
                        "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
                        "milestones":[
                          {"milestone": "Received","status": "current"},
                          {"milestone": "Acquired","status": "incomplete"},
                          {"milestone": "InProgress","status": "incomplete"},
                          {"milestone": "Done","status": "incomplete"}
                        ]},
                        {"formId":"R38_EN","formName":"TES2","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
                         "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
                         "milestones":[
                           {"milestone": "Received","status": "complete"},
                           {"milestone": "Acquired","status": "current"},
                           {"milestone": "InProgress","status": "incomplete"},
                           {"milestone": "Done","status": "incomplete"}
                         ]}]}"""

        val trackingFormSequence = Json.parse(json).as[Seq[TrackedForm]](trackedFormSeqReads)
        trackingFormSequence mustBe (Seq(
          TrackedForm("R39_EN", "TES1", TrackedFormReceived),
          TrackedForm("R38_EN", "TES2", TrackedFormAcquired)))

      }

      "The list has multiple tracking elements" in {
        val json =
          """{"submissions":[{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
                        "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
                        "milestones":[
                          {"milestone": "Received","status": "current"},
                          {"milestone": "Acquired","status": "incomplete"},
                          {"milestone": "InProgress","status": "incomplete"},
                          {"milestone": "Done","status": "incomplete"}
                        ]}]}"""

        val trackingFormSequence = Json.parse(json).as[Seq[TrackedForm]](trackedFormSeqReads)
        trackingFormSequence mustBe (Seq(TrackedForm("R39_EN", "TES1", TrackedFormReceived)))
      }
    }
  }
}
