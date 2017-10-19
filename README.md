# Search API [![CircleCI](https://circleci.com/gh/VivaReal/search-api/tree/master.svg?&style=shield&circle-token=ba04762cae23d66aa73b715ef66562f0928dfafb)](https://circleci.com/gh/VivaReal/search-api/tree/master)

![Search](src/main/resources/static/search.png "SearchAPI")

The **SearchAPI** is a Web API that responsibles to interface our search engine. There is a main controller called [SearchController](src/main/java/com/vivareal/search/api/controller/SearchController.java) to searching documents. You can make queries using features such as filters, facets, sorts and much more. Please, see [API Reference](#api-reference) to more details.

To generating a client, we can use [Haxe](https://haxe.org) cross-platform toolkit (incubating feature).

# Table of contents

- [Application Checklist](#application-checklist)
- [Setup](#setup)
    - [Build requirements](#build-requirements)
    - [How to Build](#how-to-build)
    - [How to Run](#how-to-run)
    - [How to Test](#how-to-test)
- [API Reference](#api-reference)
    - [Query DSL](#query-language-syntax)
- [Deploy](#deploy)
    - [Deploying from Jenkins](#deploying-from-jenkins)
- [Load Tests](#load-tests)

## Application Checklist

- [X] [API Docs](http://search-api-v2.vivareal.com/swagger-ui.html)
- [X] [CD](http://jenkins.vivareal.com/view/SEARCH-API/job/SEARCH_API_V2_PROD/build?delay=0sec)
- [x] [CircleCI](https://circleci.com/gh/VivaReal/search-api)
- [X] [Code Quality](https://sonarqube.vivareal.io/dashboard/index/14469)
- [X] [DockerHub](https://hub.docker.com/r/vivareal/search-api-v2/)
- [X] Logs
    - [X] [Graylog](http://logs-dash.vivareal.com/search?rangetype=relative&fields=message,source&width=1920&highlightMessage=&relative=7200&q=application:search-api-v2)
    - [X] [Sentry](https://sentry-logs.vivareal.com/vivareal/searchapi-v2)
- [X] [New Relic](https://rpm.newrelic.com/accounts/645240/applications/47165905)

## Setup

### Build requirements

In order to build SearchAPI application you need to have:

- JDK 1.8
- Setup `JAVA_HOME` environment variables with path to JDK 1.8

### How to Build

To build this project, first time you try to build you need to run this:

```sh
./gradlew build
```

You can import the project in your favorite IDE:

- Eclipse
    - Run the command `./gradlew cleanEclipse eclipse`;
    - import project at Eclipse;

- Intellij IDEA
    - Import project as Gradle project using `build.gradle`

### How to Run

You must pass the `-Des.cluster.name` Java parameter.


Tool      | Command
--------- | -------
<img src="src/main/resources/static/gradle.png" alt="Gradle" width="75" />   | ```./gradlew bootRun -Des.cluster.name=<YOUR_CLUSTER_NAME>```
<img src="src/main/resources/static/java.png" alt="Java" width="75" />       | ```java -Des.cluster.name=<YOUR_CLUSTER_NAME> -jar build/libs/search-api.jar```
<img src="src/main/resources/static/docker.png" alt="Docker" width="75"/>    | ```docker run --rm -it -p 8482:8482 -e JAVA_OPTS='-Des.cluster.name=<YOUR_CLUSTER_NAME>' vivareal/search-api-v2:<VERSION>```

### How to Test

There are two test types:

- Unit tests

```sh
./gradlew test
```
When you run just `test` the integration tests **always** run together.

- Integration tests

```sh
./gradlew integrationTest
```

The `integration tests` are responsible to guarantees a SearchAPI fine integration to ElasticSearch. We are using [Docker Compose](https://github.com/VivaReal/search-api/blob/master/docker-compose.yml) to up Elasticsearch and SearchAPI Docker containers and run [SearchApiIntegrationTest](https://github.com/VivaReal/search-api/blob/master/src/integration-test/java/com/vivareal/search/api/itest/SearchApiIntegrationTest.java) class.

To skipping:
 - Integration tests just use `-x integrationTest` in your Gradle execution.
 - Docker compose just use `-PnoDockerCompose` Gradle parameter.

## API Reference

There are only two endpoints to **searching** and **monitoring**:

- **Searching** endpoints:

    - `GET /v2/{index}`: Search documents
    - `GET /v2/{index}/{id}`: Search document by id
    - `GET /v2/{index}/stream`: Streaming endpoint (using [application/x-ndjson](http://ndjson.org) content type)

    Main parameters:

    | Name              | Type       | Description
    | ----              | ----       | -----------
    | `from`            | `int`      | From index to start the search from
    | `size`            | `int`      | The number of search hits to return
    | `filter`          | `string`   | Query DSL
    | `includeFields`   | `string[]` | Fields that will be included in the result
    | `excludeFields`   | `string[]` | Fields that will be excluded in the result

    There are many parameters and you can see [here](http://search-api-v2.vivareal.com).

- **Monitoring** endpoints:

    - `GET /v2/cluster/settings`: Get all configs
    - `GET /v2/properties/local`: Get local properties
    - `GET /v2/properties/remote`: Get remote properties

### Query language syntax

SearchAPI provides a Query DSL creating flexible queries to our search engine. The idea is to abstract any kind of complexity to making queries and search optimizations.

Basically, you need just use `filter` query parameter for that, for example:

<pre>
curl -X GET http://api/v2/listings<b>?filter=</b>field1 EQ 'value1' AND (field2 EQ 'value2'OR field3 EQ 'value3')
</pre>

SearchAPI parses this query using different kind of parsers and generates an Abstract Syntax Tree with the query fragments. To explanation the query fragments, please see the image below:

![QueryDSL](https://github.com/VivaReal/search-api/raw/master/src/main/resources/static/query-dsl.png "Query DSL")

You can see more details in [wiki](https://github.com/VivaReal/search-api/wiki).

## How to Deploy

We are deploying SearchAPI with Amazon AWS using [El Asno Alado](https://github.com/VivaReal/el-asno-alado) project and the mainly file to configure deploy is a [Makefile](https://github.com/VivaReal/search-api/blob/master/Makefile) located in the project's root directory.

First of all, you need to setup your AWS Credentials and sync git submodules:

```sh
export AWS_ACCESS_KEY_ID=<YOUR_ACCESS_KEY>
export AWS_SECRET_ACCESS_KEY=<YOUR_SECRET_KEY>

git submodule init && git submodule update --init --recursive
```

After that you can simply run `make deploy` passing all required parameters to deploy:

**Make sure you've pushed the docker image to [DockerHub](https://hub.docker.com/) before deploying the environment.**

```sh
make ENV=${ENV} \
     IMAGE_NAME=${IMAGE_NAME} \
     STACK_ALIAS=${STACK_ALIAS} \
     AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION} \
     ES_CLUSTER_NAME=${ES_CLUSTER_NAME} \
     deploy
```

- `ENV`: environment (`prod` or `qa`).

- `IMAGE_NAME` is a string with the image pushed to Dockerhub

- `STACK_ALIAS` is a string used to name Cloudformation stack. If not present, the hash of the current commit will be used to identify the stack.

- `AWS_DEFAULT_REGION` is a string with the AWS region to deploy. (Eg. `sa-east-1`)

- `ES_CLUSTER_NAME` is a string with the current [ElasticSearch](https://github.com/VivaReal/search-es) cluster.

### Deploying from Jenkins

<a href="http://jenkins.vivareal.com/view/SEARCH-API/job/SEARCH_API_V2_PROD/build?delay=0sec">
  <img src="http://ftp-chi.osuosl.org/pub/jenkins/art/jenkins-logo/logo+title.svg" alt="Jenkins" width="150">
</a>

## Load Tests

There is a `load-test` sub-project that responsible to execute load tests for **SearchAPI**.

See [Load Test](load-test/)

## Code Benchmark test

We are using [jmh](http://openjdk.java.net/projects/code-tools/jmh) to code benchmark test and [jmh Gradle Plugin](https://github.com/melix/jmh-gradle-plugin).

To run code benchmark tests:

```sh
make benchmark
```

----

Made with <a href="https://www.myinstants.com/media/sounds/i-will-always-love-you-low.mp3">&#9829;</a> by the VivaReal's engineering team.
