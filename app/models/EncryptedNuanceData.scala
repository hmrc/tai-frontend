/*
 * Copyright 2020 HM Revenue & Customs
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

package models

import com.google.inject.Inject
import services.NuanceEncryptionService
import uk.gov.hmrc.http.HeaderCarrier

/**
 * Encrypted and/or hashed data fields which need to be sent to Nuance to support Virtual Assistant
 * userid-recovery-api microservice will pick up these encrypted values, decrypt and pass
 * on to Nuance.  Encryption is necessary to avoid plaintext JSON data in the HTML.
 *
 * This class implements the encryption algorithm as agreed in AIV-1751
 *
 * Each plaintext value is prefixed by a Sha512 hash for receiver verification purposes
 * Then, the combined result is encrypted with AES256CGM using a shared secret
 * Finally, the Ciphertext is preprended with "ENCRYPTED-"
 *
 * nuanceSessionId: hashed SessionId value  (for gov.uk chat client session tracking)
 * mdtpSessionId: encrypted SessionId value (for TXM auditing)
 * deviceId: encrypted DeviceId value (for TXM auditing)
 *
 */

case class EncryptedNuanceData @Inject()(nuanceSessionId: String, mdtpSessionID: String, deviceID: String)

object EncryptedNuanceData {

  /**
   * Construct encrypted fields using data from request and header carrier
   */
  def create(cryptoService: NuanceEncryptionService, hc: HeaderCarrier): EncryptedNuanceData = {
    EncryptedNuanceData(
      cryptoService.nuanceSafeHash(sessionId(hc)),
      cryptoService.encryptField(sessionId(hc)),
      cryptoService.encryptField(deviceID(hc))
    )
  }

  private def sessionId(hc: HeaderCarrier): String =
    hc.sessionId.fold("")(_.value)

  private def deviceID(hc: HeaderCarrier): String =
    hc.deviceID.fold("")(_.toString)

}