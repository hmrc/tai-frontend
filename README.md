Tax Account for Individuals
=================

This service provides the frontend endpoint for the [TAI](https://github.com/hmrc/tai) microservice. This service allows users to view and edit their paye tax information.

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

This service is written in [Scala 2.13](http://www.scala-lang.org/) and [Play 3.0](http://playframework.com/), so needs at least a [JRE 21](http://www.oracle.com/technetwork/java/javase/downloads/index.html) to run.



How to test the project
===================

Unit Tests
----------
- **Unit test the entire test suite:**  `sbt test`

- **Unit test a single spec file:**  sbt "test:testOnly *fileName"   (for e.g : `sbt "test:testOnly *AddEmploymentControllerSpec"`)


Integration tests
----------------
- **`sbt it/test`**


Acceptance tests
----------------
To verify the acceptance tests locally, follow the steps:
- start the sm2 container for TAI profile: `sm2 --start TAI_ALL`
- stop `TAI_FRONTEND` process running in sm2: `sm2 --stop TAI_FRONTEND`
- launch tai-frontend in terminal and execute the following command in the tai-frontend project directory: <br> `sbt "run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes"`
- open [tai-acceptance-test-suite](https://github.com/hmrc/tai-acceptance-test-suite) repository in the terminal and execute the script: `./run_tests_local.sh`


Acronyms
--------
In the context of this service we use the following acronyms:

* [SCA]: Single Customer Account

* [JRS]: Job Retention Scheme

* [NPS]: National Insurance and PAYE Service

* [API]: Application Programming Interface

* [HoD]: Head of Duty

* [JRE]: Java Runtime Environment

* [JSON]: JavaScript Object Notation

* [URL]: Uniform Resource Locator

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
[JRS]: https://www.gov.uk/guidance/claim-for-wages-through-the-coronavirus-job-retention-scheme
[SCA]: https://www.gov.uk/government/publications/single-customer-account-accounting-officer-assessment

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html




