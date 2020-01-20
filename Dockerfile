FROM openjdk:11-jdk-slim

ARG ARTIFACT

COPY build/libs/datadog.jar /usr/local/
COPY $ARTIFACT /usr/local/search-api.jar

EXPOSE 8482
ENTRYPOINT exec java -server $JAVA_OPTS -jar /usr/local/search-api.jar
