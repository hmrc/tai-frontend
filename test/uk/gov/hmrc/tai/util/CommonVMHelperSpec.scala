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

package uk.gov.hmrc.tai.util

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.test.UnitSpec

class CommonVMHelperSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport{

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

 "calling isTrue" must {
   "return true, if string is 'true'" in {
     CommonVMHelper.isTrue("true") mustBe true
   }

   "return false, if string is 'false'" in {
     CommonVMHelper.isTrue("false") mustBe false
   }

   "return false, if string is anything else" in {
     CommonVMHelper.isTrue("hello") mustBe false
   }
 }
}
