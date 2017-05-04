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
docker run --rm vivareal/search-api:<VERSION>
```

# How to Test

```sh
./gradlew test
```

# How to Deploy

TODO
