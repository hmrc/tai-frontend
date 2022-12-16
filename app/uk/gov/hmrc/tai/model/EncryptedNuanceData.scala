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

package uk.gov.hmrc.tai.model

import com.google.inject.Inject
import uk.gov.hmrc.tai.service.NuanceEncryptionService

/**
  * Encrypted nuanceSessionId data field which need to be sent to Nuance to support gov.uk
  * users microservice will pick up this encrypted value, and pass
  * on to Nuance.  Encryption is necessary to avoid plaintext JSON data in the HTML.
  *
  * This class implements the encryption algorithm as agreed in AIV-1751
  *
  * Each plaintext value is prefixed by a Sha512 hash for receiver verification purposes
  * Then, the combined result is encrypted with AES256CGM using a shared secret. it then
  * uses base64 to encode further.
  *
  * nuanceSessionId: hashed SessionId value  (for gov.uk chat client session tracking)
  */
case class EncryptedNuanceData @Inject()(nuanceSessionId: String, mdtpSessionID: String)
object EncryptedNuanceData {

  /**
    * Construct encrypted fields using data from request and header carrier
    */
  def create(encryptionService: NuanceEncryptionService, sessionID: String): List[String] =
    List(encryptionService.nuanceSafeHash(sessionID), encryptionService.encryptField(sessionID))
}
