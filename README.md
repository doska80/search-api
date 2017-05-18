# Search API [![CircleCI](https://circleci.com/gh/VivaReal/search-api/tree/master.svg?&style=shield&circle-token=ba04762cae23d66aa73b715ef66562f0928dfafb)](https://circleci.com/gh/VivaReal/search-api/tree/master)

![Searching](https://github.com/VivaReal/search-api/raw/master/src/main/resources/static/house-search.jpg "House Searching")

Search API version 2.

Search API is a API created with Spring boot framework that does queries to our search engine. To generating a client, we should use [Haxe](https://haxe.org) cross-platform toolkit.

## Configuring project

### Eclipse

- Run the command `./gradlew cleanEclipse eclipse`;
- import project at Eclipse;

### IDEA

- import project as gradle project using `build.gradle` file and be happy <3

# How to Run

You can run with many ways:

## Gradle

```sh
./gradlew bootRun
```

## Java Run

```sh
java -jar build/libs/search-api.jar
```

## Docker

```sh
docker run --rm -it -p 8482:8482 vivareal/search-api-v2:<VERSION>
```

# How to Test

```sh
./gradlew test
```

# How to Deploy

## Deploying from Jenkins
 
<a href="http://jenkins.vivareal.com/view/SEARCH-API/job/SEARCH_API_V2_QA/build?delay=0sec">
  <img src="http://ftp-chi.osuosl.org/pub/jenkins/art/jenkins-logo/logo+title.svg" alt="Jenkins" width="150">
</a> 

## Deploying from your terminal

**Make sure you've pushed the docker image to dockerhub before deploying the environment.**

```
make ENV=${ENV} IMAGE_NAME=${IMAGE_NAME} STACK_ALIAS=${STACK_ALIAS} deploy-stack
```

Possible values for ENV: `prod` or `qa`.

`IMAGE_NAME` is a string with the image pushed to Dockerhub

`STACK_ALIAS` is a string used to name Cloudformation stack. If not present, the hash of the current commit will be used to identify the stack.
