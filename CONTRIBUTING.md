Tax Estimates Frontend
====================================================================

[![Build Status](https://travis-ci.org/hmrc/tai-frontend.svg?branch=master)](https://travis-ci.org/hmrc/tai-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/tai-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/tai-frontend/_latestVersion)

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.


How to contribute
-----------

If you want to contribute any changes to Tax Estimates Frontend application, then
 * Go to the [tai-front](https://github.com/hmrc/tai-frontend) repository on github.
 * Click the “Fork” button at the top right.
 * You’ll now have your own copy of that repository in your github account.
 * Open a terminal/shell and clone directory using below command

  ```$ git clone git@github.com:username/tai-frontend.git```

  where 'username' is your github user name

* You’ll now have a local copy of your version of that repository.
* Change to project directory tai-frontend and start with changes.

Post code changes check
-----------

Once you are done with the changes make sure that:
* all test cases pass successfully. Use below command to run the testcases
 
  ```$ sbt test```

* all your changes must be covered by unit test cases. If not, please write more testcases.
* Your code changes must not reduce existing code coverage. Use below command to generate coverage report
 
  ```$ sbt clean coverage test```

* you have taken latest code from master before you raise 
* there are no merge conflicts in your pull request.
* you have provided relevant comments while committing the changes and while raising the pull request. 
 
What happens next
------------

Once you have raised a pull request for the changes, tai-frontend owner team will receive an email. The team will review these changes and will advice you further. They will:
* check for unit test code coverage for the changes
* check the overall test coverage for the whole project
* review the changes and may ask you for further enhancements
* merge your changes and you will receive a mail for the same
