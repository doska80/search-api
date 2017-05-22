FROM openjdk:8u121-jdk

COPY build/libs/search-api.jar /usr/local/search-api.jar

EXPOSE 4000 8482
ENTRYPOINT java -server -jar /usr/local/search-api.jar