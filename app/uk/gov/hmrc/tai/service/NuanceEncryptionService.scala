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

package uk.gov.hmrc.tai.service

import com.google.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.crypto.{CryptoGCMWithKeysFromConfig, PlainText, Scrambled, Sha512Crypto}

/**
  * Service for encrypting data to send to Nuance (Virtual Assistance)
  * The configuration here (and algorithm) needs to match that in userid-recovery-api
  * which contains the decryption code
  */
case class NuanceEncryptionService @Inject()(configuration: Configuration) {
  protected val FIELD_PREFIX = "ENCRYPTED-"
  private val VALUE_SEPARATOR = "-"
  private val baseSettingsKey = "request-body-encryption"
  private val hashingKey: String = configuration.get[String](s"$baseSettingsKey.hashing-key")
  private val bytes = 32
  private val saltArray = new Array[Byte](bytes)
  private val salt: String = saltArray.map(_.toChar).mkString
  protected lazy val crypto: CryptoGCMWithKeysFromConfig = new CryptoGCMWithKeysFromConfig(
    baseConfigKey = baseSettingsKey,
    config = configuration.underlying
  )
  protected lazy val hasher: Sha512Crypto = new Sha512Crypto(hashingKey)
  def encryptField(rawValue: String): String =
    FIELD_PREFIX + crypto.encrypt(prefixWithHash(rawValue)).value
  private[services] def prefixWithHash(rawValue: String): PlainText =
    PlainText(saltedHashValue(rawValue).value + VALUE_SEPARATOR + rawValue)
  private def hashValue(rawValue: String): Scrambled = hasher.hash(PlainText(rawValue))
  private[services] def saltedHashValue(rawValue: String): Scrambled = hasher.hash(PlainText(salt + rawValue))
  private[services] def hashField(rawValue: String): String = hashValue(rawValue).value

  /**
    * Make a Nuance safe hash value from a raw value by hashing and then
    * mapping non-alphanumeric characters to alphanumeric characters.
    * Why? Nuance cannot handle chars such as "+", "-", "%" etc
    * Algorithm takes any non-alpha char and maps to A - Z
    */
  def nuanceSafeHash(rawValue: String): String =
    hashField(rawValue).map { char =>
      if (char.isLetterOrDigit) char else ((char.toInt % 26) + 'A'.toInt).toChar
    }
}
