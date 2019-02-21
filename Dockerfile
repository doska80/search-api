FROM openjdk:11-jdk-slim

ARG ARTIFACT

COPY build/libs/newrelic.jar /usr/local/
COPY build/resources/main/newrelic.yml /usr/local/
COPY $ARTIFACT /usr/local/search-api.jar

EXPOSE 8482
ENTRYPOINT exec java $JAVA_OPTS -server -jar /usr/local/search-api.jar
