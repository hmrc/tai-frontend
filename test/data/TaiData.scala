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

package data

import java.io.File

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.tai.model.{IncomeData, TaiRoot, TaxSummaryDetails}
import uk.gov.hmrc.tai.viewModels.EstimatedIncomeViewModel

import scala.io.BufferedSource
import scala.util.Random

object TaiData {

  private val basePath = "test/data/"
  private lazy val additionalTaxWithoutUnderpayment = "AdditionalTax/TaxSummary.json"
  private lazy val additionalTaxWithUnderpayment = "AdditionalTaxWithUnderpayment/TaxSummary.json"
  private lazy val incomeAtBasicRateJson = "IncomeAtBasicRate/TaxSummary.json"
  private lazy val incomeAtHigherRateJson = "IncomeAtHigherRate/TaxSummary.json"
  private lazy val pensionsJson = "Pensions/TaxSummary.json"
  private lazy val incomesAndPensionsJson = "IncomesAndPensions/TaxSummary.json"
  private lazy val potentialUnderpaymentJson = "PotentialUnderpayment/TaxSummary.json"
  private lazy val inYearAdjustmentJson = "InYearAdjustment/TaxSummary.json"
  private lazy val inYearAdjustmentCYOnlyJson = "InYearAdjustment/TaxSummaryCY.json"
  private lazy val inYearAdjustmentCYPlusOneOnlyJson = "InYearAdjustment/TaxSummaryCYPlusOne.json"
  private lazy val inYearAdjustmentCYandNegativeCYPlusOneJson = "InYearAdjustment/TaxSummaryCYandNegativeCYPlusOne.json"
  private lazy val editableCeasedAndPension = "EditableCeasedAndPension/TaxSummary.json"
  private lazy val editableCeasedAndIncome= "EditableCeasedAndIncome/TaxSummary.json"
  private lazy val singlePensionInocme = "OnePensionIncome/TaxSummary.json"
  private lazy val gatekeeperUser = "GatekeeperUser/TaxSummary.json"
  private lazy val person = "Person/person.json"
  private lazy val taxCodeDetails = "TaxCodeDetails/TaxSummary.json"
  private lazy val everything = "Everything/TaxSummary.json"
  private lazy val currentYearTaxSummaryDetails = "SessionDetails/CurrentYearTaxSummaryDetails.json"
  private lazy val currentYearCeasedTaxSummaryDetails = "SessionDetails/CurrentYearCeasedTaxSummaryDetails.json"
  private lazy val currentYearMultipleCeasedTaxSummaryDetails = "SessionDetails/CurrentYearMultipleCeasedTaxSummaryDetails.json"
  private lazy val currentYearMultiplePotentialCeasedTaxSummaryDetails = "SessionDetails/CurrentYearMultiplePotentialCeasedTaxSummaryDetails.json"
  private lazy val estimatedIncomeViewModel = "NextYearComparison/EstimatedIncomeViewModel.json"

  private def getEstimatedIncomeViewModel(fileName: String): EstimatedIncomeViewModel = {
    val jsonFilePath = basePath + fileName
    val file : File = new File(jsonFilePath)

    val source:BufferedSource = scala.io.Source.fromFile(file)
    val jsVal = Json.parse(source.mkString(""))
    val result = Json.fromJson[EstimatedIncomeViewModel](jsVal)
    result.get
  }

  private def getTaxSummary(fileName: String):TaxSummaryDetails = {
    val jsonFilePath = basePath + fileName
    val file : File = new File(jsonFilePath)
    val nino = new Generator(new Random).nextNino

    val source:BufferedSource = scala.io.Source.fromFile(file)
    val jsVal = Json.parse(source.mkString("").replaceAll("\\$NINO", nino.nino.take(8)))
    val result = Json.fromJson[TaxSummaryDetails](jsVal)
    result.get
  }

  private def getPersonData(fileName: String):TaiRoot = {
    val jsonFilePath = basePath + fileName
    val file : File = new File(jsonFilePath)
    val nino = new Generator(new Random).nextNino

    val source:BufferedSource = scala.io.Source.fromFile(file)
    val jsVal = Json.parse(source.mkString("").replaceAll("\\$NINO", nino.nino))
    val result = Json.fromJson[TaiRoot](jsVal)
    result.get
  }

  private def getRtiData(fileName: String): RtiData = {
    val jsonFilePath = basePath + fileName
    val file : File = new File(jsonFilePath)

    val source:BufferedSource = scala.io.Source.fromFile(file)
    val jsVal = Json.parse(source.mkString(""))
    val result = Json.fromJson[RtiData](jsVal)
    result.get
  }

  private def getIncomeData(fileName: String): IncomeData = {
    val jsonFilePath = basePath + fileName
    val file : File = new File(jsonFilePath)

    val source:BufferedSource = scala.io.Source.fromFile(file)
    val jsVal = Json.parse(source.mkString(""))
    val result = Json.fromJson[IncomeData](jsVal)
    result.get
  }
  
  private def getErrorData(fileName: String):JsValue = {
    val jsonFilePath = basePath + fileName
    val file : File = new File(jsonFilePath)

    val source:BufferedSource = scala.io.Source.fromFile(file)
    Json.parse(source.mkString(""))
  }

  private def getJson(fileName: String):JsValue = {
    val jsonFilePath = basePath + fileName
    val file : File = new File(jsonFilePath)

    val source:BufferedSource = scala.io.Source.fromFile(file)
    Json.parse(source.mkString(""))
  }


  def getBasicRateTaxSummary = getTaxSummary(incomeAtBasicRateJson)
  def getHigherRateTaxSummary = getTaxSummary(incomeAtHigherRateJson)
  def getMultiplePensionsTaxSummary = getTaxSummary(pensionsJson)
  def getIncomesAndPensionsTaxSummary = getTaxSummary(incomesAndPensionsJson)
  def getPotentialUnderpaymentTaxSummary = getTaxSummary(potentialUnderpaymentJson)
  def getInYearAdjustmentJsonTaxSummary = getTaxSummary(inYearAdjustmentJson)
  def getInYearAdjustmentJsonTaxSummaryCYOnly = getTaxSummary(inYearAdjustmentCYOnlyJson)
  def getInYearAdjustmentJsonTaxSummaryCYPlusOneOnly = getTaxSummary(inYearAdjustmentCYPlusOneOnlyJson)
  def getInYearAdjustmentJsonTaxSummaryCYandNegativeCYPlusOne = getTaxSummary(inYearAdjustmentCYandNegativeCYPlusOneJson)
  def getEditableCeasedAndIncomeTaxSummary = getTaxSummary(editableCeasedAndIncome)
  def getAdditionalTaxWithoutUnderpayment = getTaxSummary(additionalTaxWithoutUnderpayment)
  def getSinglePensionIncome = getTaxSummary(singlePensionInocme)
  def getGatekeeperUser = getTaxSummary(gatekeeperUser)
  def getPerson = getPersonData(person)
  def getTaxCodeTaxSummary = getTaxSummary(taxCodeDetails)
  def getEverything = getTaxSummary(everything)
  def getCurrentYearTaxSummaryDetails = getTaxSummary(currentYearTaxSummaryDetails)
  def getEverythingJson = getJson(everything)
  def getEstimatedIncome = getEstimatedIncomeViewModel(estimatedIncomeViewModel)
  def getCurrentYearCeasedTaxSummaryDetails = getTaxSummary(currentYearCeasedTaxSummaryDetails)
  def getCurrentYearMultipleCeasedTaxSummaryDetails = getTaxSummary(currentYearMultipleCeasedTaxSummaryDetails)
  def getCurrentYearMultiplePotentialCeasedTaxSummaryDetails = getTaxSummary(currentYearMultiplePotentialCeasedTaxSummaryDetails)
}

