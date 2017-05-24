FROM openjdk:8u121-jdk

COPY build/libs/search-api.jar /usr/local/
COPY build/libs/newrelic.jar /usr/local/
COPY build/resources/main/newrelic.yml /usr/local/

EXPOSE 4000 8482
ENTRYPOINT java -server -jar /usr/local/search-api.jar