# htbhf-claimant-service

[![Build Status](https://img.shields.io/travis/com/DepartmentOfHealth-htbhf/htbhf-claimant-service/master.svg)](https://travis-ci.com/DepartmentOfHealth-htbhf/htbhf-claimant-service)
[![Known Vulnerabilities](https://snyk.io/test/github/DepartmentOfHealth-htbhf/htbhf-claimant-service/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/DepartmentOfHealth-htbhf/htbhf-claimant-service?targetFile=build.gradle)
[![Coverage Status](https://codecov.io/gh/DepartmentOfHealth-htbhf/htbhf-claimant-service/branch/master/graph/badge.svg)](https://codecov.io/gh/DepartmentOfHealth-htbhf/htbhf-claimant-service)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

[View latest test reports](https://departmentofhealth-htbhf.github.io/htbhf-claimant-service/)

This is a Spring Boot project to provide a rest service responsible for storing the details of claimants.

Documentation of the API is available online when the service is running (e.g. http://localhost:8080/v2/api-docs),
and is also available as [swagger.yml](swagger.yml) - paste the contents into https://editor.swagger.io/ for a friendly UI.

### Deployment to the PaaS
The jar artefact of the API project is deployed to the PaaS, using the [manifest.yml](manifest.yml) file in the parent project.
Note that this requires some services be present in the space to which it is deployed: 
* `htbhf-claimant-service-postgres` (see [db/README.md](db/README.md) for further details)
* `logit-ssl-drain` (see [https://docs.cloud.service.gov.uk/monitoring_apps.html#configure-app](https://docs.cloud.service.gov.uk/monitoring_apps.html#configure-app) for further details)

### Running locally
In order to run the application locally, a valid notify api key will need to be set in an environment variable called `NOTIFY_API_KEY`. e.g. `export NOTIFY_API_KEY=MY-API-KEY`
and an environment variable called `GOOGLE-ANALYTICS_TRACKING-ID` (exact value doesn't matter), e.g. `export GOOGLE-ANALYTICS_TRACKING-ID=test-key`.

Note, you must set the environment variable in the same context you are running the application in. 

If you are  running the application in intellij, environment variables can be set via [run/debug configurations](https://www.jetbrains.com/help/idea/creating-and-editing-run-debug-configurations.html).

### Create claims for local testing
There is a test utility for creating claims in a locally running persisted database. See the test-claimant-creator [README](api/src/test/java/uk/gov/dhsc/htbhf/claimant/creator/README.md) for details on how to run it. 
