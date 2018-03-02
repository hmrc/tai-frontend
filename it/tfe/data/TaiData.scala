package tfe.data

import play.api.libs.json.Json
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.tai.model.TaxSummaryDetails

object TaiData {
  private val currentYearTaxCodeSummaryJson =
    """{
      |  "nino": "$NINO",
      |  "version": 1,
      |  "accounts":[],
      |  "increasesTax": {
      |    "incomes": {
      |      "taxCodeIncomes": {
      |        "occupationalPensions": {
      |          "taxCodeIncomes": [
      |            {
      |              "name": "employer1",
      |              "taxCode": "104Y",
      |              "employmentId": 1,
      |              "employmentPayeRef": "MS26",
      |              "employmentType": 1,
      |              "incomeType": 1,
      |              "employmentStatus": 1,
      |              "tax": {
      |                "totalIncome": 2304,
      |                "totalTaxableIncome": 1258,
      |                "totalTax": 251.6,
      |                "taxBands": [
      |                  {
      |                    "income": 1258,
      |                    "tax": 251.6,
      |                    "lowerBand": 0,
      |                    "upperBand": 31865,
      |                    "rate": 20
      |                  },
      |                  {
      |                    "income": 0,
      |                    "tax": 0,
      |                    "lowerBand": 31865,
      |                    "upperBand": 150000,
      |                    "rate": 40
      |                  },
      |                  {
      |                    "income": 0,
      |                    "tax": 0,
      |                    "lowerBand": 150000,
      |                    "upperBand": 0,
      |                    "rate": 45
      |                  }
      |                ],
      |                "allowReliefDeducts": 1046
      |              },
      |              "worksNumber": "D013100541583",
      |              "jobTitle": "PENSION £1257 PA",
      |              "startDate": "2004-11-01",
      |              "income": 2304,
      |              "otherIncomeSourceIndicator": false,
      |              "isEditable": true,
      |              "isLive": true,
      |              "isOccupationalPension": true,
      |              "isPrimary": true
      |            },
      |            {
      |              "name": "PAYESCHEMEOPERATORNAME54249",
      |              "taxCode": "500T",
      |              "employmentId": 8,
      |              "employmentPayeRef": "MA83247",
      |              "employmentType": 2,
      |              "incomeType": 1,
      |              "employmentStatus": 1,
      |              "tax": {
      |                "totalIncome": 3805,
      |                "totalTaxableIncome": 3805,
      |                "totalTax": 761,
      |                "taxBands": [
      |                  {
      |                    "income": 3805,
      |                    "tax": 761,
      |                    "lowerBand": 0,
      |                    "upperBand": 31865,
      |                    "rate": 20
      |                  },
      |                  {
      |                    "lowerBand": 31865,
      |                    "upperBand": 150000,
      |                    "rate": 40
      |                  },
      |                  {
      |                    "lowerBand": 150000,
      |                    "upperBand": 0,
      |                    "rate": 45
      |                  }
      |                ],
      |                "allowReliefDeducts": 0,
      |                "actualTaxDueAssumingBasicRateAlreadyPaid": 0.0
      |              },
      |              "worksNumber": "$NINO",
      |              "startDate": "2013-03-18",
      |              "income": 3805,
      |              "otherIncomeSourceIndicator": false,
      |              "isEditable": true,
      |              "isLive": true,
      |              "isOccupationalPension": true,
      |              "isPrimary": false
      |            },
      |            {
      |              "name": "PAYESCHEMEOPERATORNAME59243",
      |              "taxCode": "BR",
      |              "employmentId": 3,
      |              "employmentPayeRef": "LLB8A",
      |              "employmentType": 2,
      |              "incomeType": 1,
      |              "employmentStatus": 1,
      |              "tax": {
      |                "totalIncome": 1744,
      |                "totalTaxableIncome": 1744,
      |                "totalTax": 348.8,
      |                "taxBands": [
      |                  {
      |                    "income": 1744,
      |                    "tax": 348.8,
      |                    "lowerBand": 0,
      |                    "upperBand": 31865,
      |                    "rate": 20
      |                  },
      |                  {
      |                    "lowerBand": 31865,
      |                    "upperBand": 150000,
      |                    "rate": 40
      |                  },
      |                  {
      |                    "lowerBand": 150000,
      |                    "upperBand": 0,
      |                    "rate": 45
      |                  }
      |                ],
      |                "allowReliefDeducts": 0,
      |                "actualTaxDueAssumingBasicRateAlreadyPaid": 0.0
      |              },
      |              "worksNumber": "10664108",
      |              "jobTitle": "A R PENSION £17508",
      |              "startDate": "2004-10-04",
      |              "income": 1744,
      |              "otherIncomeSourceIndicator": false,
      |              "isEditable": true,
      |              "isLive": true,
      |              "isOccupationalPension": true,
      |              "isPrimary": false
      |            }
      |          ],
      |          "totalIncome": 7853,
      |          "totalTax": 1361.4,
      |          "totalTaxableIncome": 6807
      |        },
      |        "hasDuplicateEmploymentNames": false,
      |        "totalIncome": 7853,
      |        "totalTaxableIncome": 6807,
      |        "totalTax": 1361.4
      |      },
      |      "noneTaxCodeIncomes": {
      |        "statePension": 9614,
      |        "totalIncome": 9614
      |      },
      |      "total": 17467
      |    },
      |    "total": 17467
      |  },
      |  "decreasesTax": {
      |    "personalAllowance": 10660,
      |    "personalAllowanceSourceAmount": 10660,
      |    "paTapered": false,
      |    "total": 10660
      |  },
      |  "totalLiability": {
      |    "nonSavings": {
      |      "totalIncome": 17467,
      |      "totalTaxableIncome": 6807,
      |      "totalTax": 1361.4,
      |      "taxBands": [
      |        {
      |          "income": 6807,
      |          "tax": 1361.4,
      |          "lowerBand": 0,
      |          "upperBand": 31865,
      |          "rate": 20
      |        },
      |        {
      |          "income": 0,
      |          "tax": 0,
      |          "lowerBand": 31865,
      |          "upperBand": 150000,
      |          "rate": 40
      |        },
      |        {
      |          "income": 0,
      |          "tax": 0,
      |          "lowerBand": 150000,
      |          "upperBand": 0,
      |          "rate": 45
      |        }
      |      ],
      |      "allowReliefDeducts": 10660
      |    },
      |    "mergedIncomes": {
      |      "totalIncome": 17467,
      |      "totalTaxableIncome": 6807,
      |      "totalTax": 1361.4,
      |      "taxBands": [
      |        {
      |          "income": 6807,
      |          "tax": 1361.4,
      |          "lowerBand": 0,
      |          "upperBand": 31865,
      |          "rate": 20
      |        },
      |        {
      |          "income": 0,
      |          "tax": 0,
      |          "lowerBand": 31865,
      |          "upperBand": 150000,
      |          "rate": 40
      |        },
      |        {
      |          "income": 0,
      |          "tax": 0,
      |          "lowerBand": 150000,
      |          "upperBand": 0,
      |          "rate": 45
      |        }
      |      ],
      |      "allowReliefDeducts": 10660
      |    },
      |    "totalLiability": 1361.4,
      |    "totalTax": 1361.4,
      |    "totalTaxOnIncome": 1361.4,
      |    "underpaymentPreviousYear": 0,
      |    "outstandingDebt": 0,
      |    "childBenefitAmount": 0,
      |    "childBenefitTaxDue": 0,
      |    "liabilityReductions": {
      |      "enterpriseInvestmentSchemeRelief": {
      |        "codingAmount": 0,
      |        "amountInTermsOfTax": 0
      |      },
      |      "concessionalRelief": {
      |        "codingAmount": 0,
      |        "amountInTermsOfTax": 0
      |      },
      |      "maintenancePayments": {
      |        "codingAmount": 0,
      |        "amountInTermsOfTax": 0
      |      },
      |      "doubleTaxationRelief": {
      |        "codingAmount": 0,
      |        "amountInTermsOfTax": 0
      |      }
      |    },
      |    "liabilityAdditions": {
      |      "excessGiftAidTax": {
      |        "codingAmount": 0,
      |        "amountInTermsOfTax": 0
      |      },
      |      "excessWidowsAndOrphans": {
      |        "codingAmount": 0,
      |        "amountInTermsOfTax": 0
      |      },
      |      "pensionPaymentsAdjustment": {
      |        "codingAmount": 0,
      |        "amountInTermsOfTax": 0
      |      }
      |    }
      |  },
      |  "adjustedNetIncome": 17467,
      |  "extensionReliefs": {
      |    "giftAid": {
      |      "sourceAmount": 0,
      |      "reliefAmount": 0
      |    },
      |    "personalPension": {
      |      "sourceAmount": 0,
      |      "reliefAmount": 0
      |    }
      |  },
      |  "taxCodeDetails": {
      |    "employment": [
      |      {
      |        "id": 1,
      |        "name": "employer1",
      |        "taxCode": "104Y"
      |      },
      |      {
      |        "id": 8,
      |        "name": "PAYESCHEMEOPERATORNAME54249",
      |        "taxCode": "500T"
      |      },
      |      {
      |        "id": 3,
      |        "name": "PAYESCHEMEOPERATORNAME59243",
      |        "taxCode": "BR"
      |      }
      |    ],
      |    "taxCode": [
      |      {
      |        "taxCode": "Y"
      |      },
      |      {
      |        "taxCode": "T"
      |      },
      |      {
      |        "taxCode": "BR",
      |        "rate": 20
      |      }
      |    ],
      |    "taxCodeDescriptions": [
      |      {
      |        "taxCode": "104Y",
      |        "name": "employer1",
      |        "taxCodeDescriptors": [
      |          {
      |            "taxCode": "Y"
      |          }
      |        ]
      |      },
      |      {
      |        "taxCode": "500T",
      |        "name": "PAYESCHEMEOPERATORNAME54249",
      |        "taxCodeDescriptors": [
      |          {
      |            "taxCode": "T"
      |          }
      |        ]
      |      },
      |      {
      |        "taxCode": "BR",
      |        "name": "PAYESCHEMEOPERATORNAME59243",
      |        "taxCodeDescriptors": [
      |          {
      |            "taxCode": "BR",
      |            "rate": 20
      |          }
      |        ]
      |      }
      |    ],
      |    "deductions": [
      |      {
      |        "description": "state pension/state benefits",
      |        "amount": 9614,
      |        "componentType": 1
      |      }
      |    ],
      |    "allowances": [
      |      {
      |        "description": "Tax Free Amount",
      |        "amount": 10660,
      |        "componentType": 0
      |      }
      |    ],
      |    "splitAllowances": false,
      |    "total": 1046
      |  },
      |  "incomeData": {
      |    "incomeExplanations": [
      |      {
      |        "employerName": "employer1",
      |        "incomeId": 1,
      |        "hasDuplicateEmploymentNames": false,
      |        "worksNumber": "D013100541583",
      |        "notificationDate": "2016-05-05",
      |        "updateActionDate": "2016-05-05",
      |        "startDate": "2007-04-06",
      |        "employmentStatus": 1,
      |        "employmentType": 1,
      |        "isPension": true,
      |        "isJSA": false,
      |        "iabdSource": 46,
      |        "payToDate": 2304,
      |        "calcAmount": 2304,
      |        "grossAmount": 2304,
      |        "payFrequency": "W1",
      |        "editableDetails": {
      |          "isEditable": true,
      |          "payRollingBiks": false
      |        }
      |      },
      |      {
      |        "employerName": "PAYESCHEMEOPERATORNAME54249",
      |        "incomeId": 8,
      |        "hasDuplicateEmploymentNames": false,
      |        "worksNumber": "$NINO",
      |        "notificationDate": "2016-05-05",
      |        "updateActionDate": "2016-05-05",
      |        "startDate": "2007-04-06",
      |        "employmentStatus": 1,
      |        "employmentType": 1,
      |        "isPension": true,
      |        "isJSA": false,
      |        "iabdSource": 46,
      |        "payToDate": 3805,
      |        "calcAmount": 3805,
      |        "grossAmount": 3805,
      |        "payFrequency": "W1",
      |        "editableDetails": {
      |          "isEditable": true,
      |          "payRollingBiks": false
      |        }
      |      },
      |      {
      |        "employerName": "PAYESCHEMEOPERATORNAME59243",
      |        "incomeId": 8,
      |        "hasDuplicateEmploymentNames": false,
      |        "worksNumber": "10664108",
      |        "notificationDate": "2016-05-05",
      |        "updateActionDate": "2016-05-05",
      |        "startDate": "2007-04-06",
      |        "employmentStatus": 1,
      |        "employmentType": 1,
      |        "isPension": true,
      |        "isJSA": false,
      |        "iabdSource": 46,
      |        "payToDate": 1744,
      |        "calcAmount": 1744,
      |        "grossAmount": 1744,
      |        "payFrequency": "W1",
      |        "editableDetails": {
      |          "isEditable": true,
      |          "payRollingBiks": false
      |        }
      |      }
      |    ]
      |  },
      |  "accounts": [
      |    {
      |      "year": 2016,
      |      "nps": {
      |        "date": "10/12/2014",
      |        "totalEstTax": 1360.6,
      |        "totalLiability": {
      |          "nonSavings": {
      |            "taxBands": [
      |              {
      |                "income": 6807,
      |                "tax": 1361.4,
      |                "lowerBand": 0,
      |                "upperBand": 31865,
      |                "rate": 20
      |              },
      |              {
      |                "income": 0,
      |                "tax": 0,
      |                "lowerBand": 31865,
      |                "upperBand": 150000,
      |                "rate": 40
      |              },
      |              {
      |                "income": 0,
      |                "tax": 0,
      |                "lowerBand": 150000,
      |                "upperBand": 0,
      |                "rate": 45
      |              }
      |            ],
      |            "totalTax": 1361.4,
      |            "totalTaxableIncome": 6807
      |          }
      |        },
      |        "incomeSources": [
      |          {
      |            "employmentId": 1,
      |            "employmentType": 1,
      |            "employmentStatus": 1,
      |            "employmentTaxDistrictNumber": 581,
      |            "employmentPayeRef": "MS26",
      |            "pensionIndicator": true,
      |            "jsaIndicator": false,
      |            "otherIncomeSourceIndicator": false,
      |            "name": "employer1",
      |            "endDate": null,
      |            "worksNumber": null,
      |            "taxCode": "104Y",
      |            "potentialUnderpayment": 0,
      |            "employmentRecord": {
      |              "employerName": "employer1",
      |              "employmentType": 1,
      |              "sequenceNumber": 1,
      |              "worksNumber": "D013100541583",
      |              "taxDistrictNumber": "581",
      |              "iabds": [],
      |              "startDate": "01/11/2004"
      |            }
      |          },
      |          {
      |            "employmentId": 8,
      |            "employmentType": 2,
      |            "employmentStatus": 1,
      |            "employmentTaxDistrictNumber": 120,
      |            "employmentPayeRef": "MA83247",
      |            "pensionIndicator": true,
      |            "jsaIndicator": false,
      |            "otherIncomeSourceIndicator": false,
      |            "name": "PAYESCHEMEOPERATORNAME54249",
      |            "endDate": null,
      |            "worksNumber": null,
      |            "taxCode": "500T",
      |            "potentialUnderpayment": 0,
      |            "employmentRecord": {
      |              "employerName": "PAYESCHEMEOPERATORNAME54249",
      |              "employmentType": 2,
      |              "sequenceNumber": 8,
      |              "worksNumber": "$NINO",
      |              "taxDistrictNumber": "120",
      |              "iabds": [],
      |              "startDate": "18/03/2013"
      |            }
      |          },
      |          {
      |            "employmentId": 3,
      |            "employmentType": 2,
      |            "employmentStatus": 1,
      |            "employmentTaxDistrictNumber": 846,
      |            "employmentPayeRef": "LLB8A",
      |            "pensionIndicator": true,
      |            "jsaIndicator": false,
      |            "otherIncomeSourceIndicator": false,
      |            "name": "PAYESCHEMEOPERATORNAME59243",
      |            "endDate": null,
      |            "worksNumber": null,
      |            "taxCode": "BR",
      |            "potentialUnderpayment": 0,
      |            "employmentRecord": {
      |              "employerName": "PAYESCHEMEOPERATORNAME59243",
      |              "employmentType": 2,
      |              "sequenceNumber": 3,
      |              "worksNumber": "10664108",
      |              "taxDistrictNumber": "846",
      |              "iabds": [],
      |              "startDate": "04/10/2004"
      |            }
      |          }
      |        ],
      |        "iabds": []
      |      },
      |      "rti": {
      |        "request": {
      |          "nino": "$NINO",
      |          "relatedTaxYear": "15-16",
      |          "requestId": "M1423125062647"
      |        },
      |        "individual": {
      |          "employments": {
      |            "employment": [
      |              {
      |                "empRefs": {
      |                  "officeNo": "120",
      |                  "payeRef": "MA83247",
      |                  "aoRef": "002PX00100089"
      |                },
      |                "payments": {
      |                  "inYear": [
      |                    {
      |                      "payFreq": "M1",
      |                      "pmtDate": "2015-04-26",
      |                      "rcvdDate": "2015-04-19",
      |                      "mandatoryMonetaryAmount": [
      |                        {
      |                          "type": "TaxablePayYTD",
      |                          "amount": 99999.41
      |                        },
      |                        {
      |                          "type": "TotalTaxYTD",
      |                          "amount": 427.8
      |                        },
      |                        {
      |                          "type": "TaxablePay",
      |                          "amount": 2135.41
      |                        },
      |                        {
      |                          "type": "TaxDeductedOrRefunded",
      |                          "amount": 427.8
      |                        }
      |                      ],
      |                      "optionalMonetaryAmount": [
      |                        {
      |                          "type": "OccPensionAmount",
      |                          "amount": 6000.33
      |                        }
      |                      ],
      |                      "niLettersAndValues": [
      |                        {
      |                          "niFigure": [
      |                            {
      |                              "type": "EmpeeContribnsInPd",
      |                              "amount": 4800.00
      |                            },
      |                            {
      |                              "type": "EmpeeContribnsYTD",
      |                              "amount": 7200.00
      |                            }
      |                          ]
      |                        }
      |                      ],
      |                      "payId": null,
      |                      "occPenInd": false,
      |                      "irrEmp": false,
      |                      "weekNo": null,
      |                      "monthNo": null
      |                    },
      |                    {
      |                      "payFreq": "M1",
      |                      "pmtDate": "2015-05-31",
      |                      "rcvdDate": "2015-05-17",
      |                      "mandatoryMonetaryAmount": [
      |                        {
      |                          "type": "TaxablePayYTD",
      |                          "amount": 999.66
      |                        },
      |                        {
      |                          "type": "TotalTaxYTD",
      |                          "amount": 333.33
      |                        },
      |                        {
      |                          "type": "TaxablePay",
      |                          "amount": 833.33
      |                        },
      |                        {
      |                          "type": "TaxDeductedOrRefunded",
      |                          "amount": 166.66
      |                        }
      |                      ],
      |                      "optionalMonetaryAmount": [],
      |                      "niLettersAndValues": [
      |                        {
      |                          "niFigure": [
      |                            {
      |                              "type": "EmpeeContribnsInPd",
      |                              "amount": 200.00
      |                            },
      |                            {
      |                              "type": "EmpeeContribnsYTD",
      |                              "amount": 5000.00
      |                            }
      |                          ]
      |                        }
      |                      ],
      |                      "payId": null,
      |                      "occPenInd": false,
      |                      "irrEmp": false,
      |                      "weekNo": null,
      |                      "monthNo": null
      |                    }
      |                  ]
      |                },
      |                "sequenceNumber": 3
      |              },
      |              {
      |                "empRefs": {
      |                  "officeNo": "581",
      |                  "payeRef": "MS26",
      |                  "aoRef": "840PG00002273"
      |                },
      |                "payments": {
      |                  "inYear": [
      |                    {
      |                      "payFreq": "M1",
      |                      "pmtDate": "2015-04-26",
      |                      "rcvdDate": "2015-04-19",
      |                      "mandatoryMonetaryAmount": [
      |                        {
      |                          "type": "TaxablePayYTD",
      |                          "amount": 2000.41
      |                        },
      |                        {
      |                          "type": "TotalTaxYTD",
      |                          "amount": 269.75
      |                        },
      |                        {
      |                          "type": "TaxablePay",
      |                          "amount": 2135.41
      |                        },
      |                        {
      |                          "type": "TaxDeductedOrRefunded",
      |                          "amount": 269.75
      |                        }
      |                      ],
      |                      "optionalMonetaryAmount": [
      |                        {
      |                          "type": "OccPensionAmount",
      |                          "amount": 17000.33
      |                        }
      |                      ],
      |                      "niLettersAndValues": [
      |                        {
      |                          "niFigure": [
      |                            {
      |                              "type": "EmpeeContribnsInPd",
      |                              "amount": 400.00
      |                            },
      |                            {
      |                              "type": "EmpeeContribnsYTD",
      |                              "amount": 1000.00
      |                            }
      |                          ]
      |                        }
      |                      ],
      |                      "payId": null,
      |                      "occPenInd": false,
      |                      "irrEmp": false,
      |                      "weekNo": null,
      |                      "monthNo": null
      |                    },
      |                    {
      |                      "payFreq": "M1",
      |                      "pmtDate": "2016-02-28",
      |                      "rcvdDate": "2015-05-17",
      |                      "mandatoryMonetaryAmount": [
      |                        {
      |                          "type": "TaxablePayYTD",
      |                          "amount": 100000
      |                        },
      |                        {
      |                          "type": "TotalTaxYTD",
      |                          "amount": 539.5
      |                        },
      |                        {
      |                          "type": "TaxablePay",
      |                          "amount": 2135.41
      |                        },
      |                        {
      |                          "type": "TaxDeductedOrRefunded",
      |                          "amount": 269.75
      |                        }
      |                      ],
      |                      "optionalMonetaryAmount": [],
      |                      "niLettersAndValues": [
      |                        {
      |                          "niFigure": [
      |                            {
      |                              "type": "EmpeeContribnsYTD",
      |                              "amount": 1000.00
      |                            }
      |                          ]
      |                        }
      |                      ],
      |                      "payId": null,
      |                      "payId": null,
      |                      "occPenInd": false,
      |                      "irrEmp": false,
      |                      "weekNo": null,
      |                      "monthNo": null
      |                    }
      |                  ]
      |                },
      |                "currentPayId": "$NINO",
      |                "sequenceNumber": 2
      |              }
      |            ]
      |          }
      |        }
      |      }
      |    }
      |  ]
      |}"""
  private val nonCodedTaxSummaryJson =
    """{
      |  "nino": "$NINO",
      |  "version": 1,
      |  "accounts":[],
      |  "increasesTax": {
      |    "incomes": {
      |      "taxCodeIncomes": {
      |        "taxableStateBenefitIncomes": {
      |          "taxCodeIncomes": [
      |            {
      |              "name": "PAYESCHEMEOPERATORNAME52992",
      |              "taxCode": "K199",
      |              "employmentId": 15,
      |              "employmentPayeRef": "ESA500",
      |              "employmentType": 1,
      |              "incomeType": 2,
      |              "employmentStatus": 3,
      |              "tax": {
      |                "totalIncome": 50000,
      |                "totalTaxableIncome": 52000,
      |                "totalTax": 14427,
      |                "potentialUnderpayment": 3406.03,
      |                "taxBands": [
      |                  {
      |                    "income": 31865,
      |                    "tax": 6373,
      |                    "lowerBand": 0,
      |                    "upperBand": 31865,
      |                    "rate": 20
      |                  },
      |                  {
      |                    "income": 20135,
      |                    "tax": 8054,
      |                    "lowerBand": 31865,
      |                    "upperBand": 150000,
      |                    "rate": 40
      |                  },
      |                  {
      |                    "income": 0,
      |                    "tax": 0,
      |                    "lowerBand": 150000,
      |                    "upperBand": 0,
      |                    "rate": 45
      |                  }
      |                ],
      |                "allowReliefDeducts": -2000
      |              },
      |              "worksNumber": "94-732",
      |              "jobTitle": "Employment and Support Allowance",
      |              "startDate": "2014-03-31",
      |              "endDate": "2014-04-21",
      |              "income": 50000,
      |              "isEditable": false,
      |              "isLive":false,
      |              "isOccupationalPension" : false,
      |              "isPrimary": true
      |            }
      |          ],
      |          "totalIncome": 50000,
      |          "totalTax": 14427,
      |          "totalTaxableIncome": 52000
      |        },
      |        "hasDuplicateEmploymentNames": false,
      |        "totalIncome": 50000,
      |        "totalTaxableIncome": 52000,
      |        "totalTax": 14427
      |      },
      |      "noneTaxCodeIncomes": {
      |        "otherIncome": {
      |          "amount": 219,
      |          "componentType": 0,
      |          "description": "",
      |          "iabdSummaries": [
      |            {
      |              "iabdType": 19,
      |              "description": "Non-Coded Income",
      |              "amount": 219
      |            }
      |          ]
      |        },
      |        "totalIncome": 219
      |      },
      |      "total": 50219
      |    },
      |    "benefitsFromEmployment": {
      |      "amount": 12000,
      |      "componentType": 0,
      |      "description": "",
      |      "iabdSummaries": [
      |        {
      |          "iabdType": 28,
      |          "description": "Benefit in Kind",
      |          "amount": 12000,
      |          "employmentId": 15,
      |          "employmentName": "PAYESCHEMEOPERATORNAME52992"
      |        }
      |      ]
      |    },
      |    "total": 62219
      |  },
      |  "decreasesTax": {
      |    "personalAllowance": 10000,
      |    "personalAllowanceSourceAmount": 10000,
      |    "paTransferredAmount" : 0,
      |    "paReceivedAmount" : 0,
      |    "paTapered": false,
      |    "total": 10000
      |  },
      |  "totalLiability": {
      |    "nonSavings": {
      |      "totalIncome": 62219,
      |      "totalTaxableIncome": 52219,
      |      "totalTax": 14514.6,
      |      "taxBands": [
      |        {
      |          "income": 31865,
      |          "tax": 6373,
      |          "lowerBand": 0,
      |          "upperBand": 31865,
      |          "rate": 20
      |        },
      |        {
      |          "income": 20354,
      |          "tax": 8141.6,
      |          "lowerBand": 31865,
      |          "upperBand": 150000,
      |          "rate": 40
      |        },
      |        {
      |          "lowerBand": 150000,
      |          "upperBand": 0,
      |          "rate": 45
      |        }
      |      ],
      |      "allowReliefDeducts": 10000
      |    },
      |    "mergedIncomes": {
      |      "totalIncome": 62219,
      |      "totalTaxableIncome": 52219,
      |      "totalTax": 14514.6,
      |      "taxBands": [
      |        {
      |          "income": 31865,
      |          "tax": 6373,
      |          "lowerBand": 0,
      |          "upperBand": 31865,
      |          "rate": 20
      |        },
      |        {
      |          "income": 20354,
      |          "tax": 8141.6,
      |          "lowerBand": 31865,
      |          "upperBand": 150000,
      |          "rate": 40
      |        },
      |        {
      |          "lowerBand": 150000,
      |          "upperBand": 0,
      |          "rate": 45
      |        }
      |      ],
      |      "allowReliefDeducts": 10000
      |    },
      |    "nonCodedIncome": {
      |      "totalIncome": 219,
      |      "totalTaxableIncome": 219,
      |      "totalTax": 87.6
      |    },
      |    "totalLiability": 14514.6,
      |    "totalTax": 14514.6,
      |    "totalTaxOnIncome":14514.6,
      |    "underpaymentPreviousYear":0,
      |    "outstandingDebt":0,
      |    "childBenefitAmount": 0,
      |    "childBenefitTaxDue": 0
      |  },
      |  "adjustedNetIncome":62219
      |}"""

  private def parseJson(json: String): TaxSummaryDetails = {
    val nino = new Generator().nextNino

    val jsVal = Json.parse(json.stripMargin.replaceAll("\\$NINO", nino.nino))
    val result = Json.fromJson[TaxSummaryDetails](jsVal)
    result.get
  }

  val currentYearTaxSummary: TaxSummaryDetails = parseJson(currentYearTaxCodeSummaryJson)
  val nonCodedTaxSummary: TaxSummaryDetails = parseJson(nonCodedTaxSummaryJson)
}
