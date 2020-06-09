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

package services

import org.scalatest.{Matchers, WordSpec}
import play.api.Configuration
import uk.gov.hmrc.crypto.Crypted

class NuanceEncryptionServiceSpec extends WordSpec with Matchers {

  class NuanceEncryptionServiceTest(configuration: Configuration) extends NuanceEncryptionService(configuration) {

    def decryptField(cipherText: String): (String, String) = {
      val stripped: String = cipherText.stripPrefix(FIELD_PREFIX)
      val plainText: String = crypto.decrypt(Crypted(stripped)).value

      plainText.split("-").toList match {
        case ::(hashed, ::(raw, Nil)) => (hashed, raw)
        case _ => throw new RuntimeException(s"Unable to decrypt cipherText: $cipherText")
      }
    }

    "Crypto service" should {

      "encrypt plain text field which can be decrypted using correct algorithm" in {
        val configuration = Configuration.from(
          Map(
            "request-body-encryption.hashing-key" -> "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G",
            "request-body-encryption.key" -> "QmFyMTIzNDVCYXIxMjM0NQ==",
            "request-body-encryption.previousKeys" -> List.empty
          )
        )

        val service = new NuanceEncryptionServiceTest(configuration)

        val fieldValue = "session-f5119029-a05a-4b0d-9d5a-6b6e08e6c526"

        val crypted: String = service.encryptField(fieldValue)
        val expectedHash: String = service.hashField(fieldValue)

        crypted should startWith("ENCRYPTED-") // should be marked as encrypted
        crypted.stripPrefix("ENCRYPTED-") should not be fieldValue // should be encrypted

        val (outputHash, outputRaw) = service.decryptField(crypted)

        outputHash should be(expectedHash) // hash seems correct

        outputRaw should be(fieldValue) // we got the original value out
      }
    }
  }

}