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

package uk.gov.hmrc.tai.model.domain.tracking.formatter

import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.tai.model.domain.tracking._
import uk.gov.hmrc.tai.util.JsonExtra

trait TrackedFormFormatters {

  val trackedFormReads: Reads[TrackedForm] = new Reads[TrackedForm] {
    override def reads(json: JsValue): JsResult[TrackedForm] = {
      implicit val stringMapFormat: Format[Map[String, String]] = JsonExtra.mapFormat[String, String]("milestone", "status")

      val id = (json \ "formId").as[String]
      val name = (json \ "formName").as[String]
      val submissionReference = (json \ "dfsSubmissionReference").as[String]
      val milestones = (json \ "milestones").as[Map[String, String]]
      if (milestones.isEmpty) {
        Logger.warn(s"no milestones for the form with reference: $submissionReference")
        JsError("milestones list is empty")
      } else {
        milestones.filter(_._2 == "current").headOption match {
          case Some(("Received", _)) => JsSuccess(TrackedForm(id, name, TrackedFormReceived))
          case Some(("InProgress", _)) => JsSuccess(TrackedForm(id, name, TrackedFormInProgress))
          case Some(("Acquired", _)) => JsSuccess(TrackedForm(id, name, TrackedFormAcquired))
          case Some(("Done", _)) => JsSuccess(TrackedForm(id, name, TrackedFormDone))
          case None =>
            Logger.warn(s"no milestones with 'current' status for the form with reference: $submissionReference")
            JsError("there is no milestone with status 'current'")
        }
      }
    }
  }

  val trackedFormSeqReads: Reads[Seq[TrackedForm]] = new Reads[Seq[TrackedForm]] {
    override def reads(json: JsValue): JsResult[Seq[TrackedForm]] = {
      val result = (json \ "submissions").as[Seq[TrackedForm]](Reads.seq(trackedFormReads))
      JsSuccess(result)
    }
  }
}
