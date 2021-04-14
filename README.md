Tax Account for Individuals
=================

[![Build Status](https://travis-ci.org/hmrc/tai-frontend.svg)](https://travis-ci.org/hmrc/tai-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/tai-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/tai-frontend/_latestVersion)

This service Allows users to view and edit their paye tax information

Summary
-----------
This service covers the current tax year. Use the service to:
 * check your tax code and Personal Allowance
 * tell HM Revenue and Customs (HMRC) about changes that affect your tax code
 * update your employer or pension provider details
 * see an estimate of how much tax you’ll pay over the whole tax year
 * check and change the estimates of how much income you’ll get from your jobs, pensions or bank and building society savings interest


Requirements
------------

This service is written in [Scala 2.11](http://www.scala-lang.org/) and [Play 2.5](http://playframework.com/), so needs at least a [JRE 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) to run.


Optional
------------
Assets compilation can be done quickly by specifying sbt-web plugin to use NodeJS (if it is installed) by adding the following environment variable.
export SBT_OPTS="$SBT_OPTS -Dsbt.jse.engineType=Node"
For more information see: https://www.playframework.com/documentation/2.8.x/AssetsOverview#Managed-assets

Authentication
------------

This customer logs into this service using [GOV.UK Verify](https://www.gov.uk/government/publications/introducing-govuk-verify/introducing-govuk-verify).


Acronyms
--------
In the context of this service we use the following acronyms:

* [API]: Application Programming Interface

* [HoD]: Head of Duty

* [JRE]: Java Runtime Environment

* [JSON]: JavaScript Object Notation

* [URL]: Uniform Resource Locater

License
--------

This code is open source software licensed under the [Apache 2.0 License].

[NPS]: http://www.publications.parliament.uk/pa/cm201012/cmselect/cmtreasy/731/73107.htm
[HoD]: http://webarchive.nationalarchives.gov.uk/+/http://www.hmrc.gov.uk/manuals/sam/samglossary/samgloss249.htm
[NINO]: http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm
[National Insurance]: https://www.gov.uk/national-insurance/overview
[JRE]: http://www.oracle.com/technetwork/java/javase/overview/index.html
[API]: https://en.wikipedia.org/wiki/Application_programming_interface
[URL]: https://en.wikipedia.org/wiki/Uniform_Resource_Locator
[State Pension]: https://www.gov.uk/new-state-pension/overview
[SP]: https://www.gov.uk/new-state-pension/overview
[JSON]: http://json.org/

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html




