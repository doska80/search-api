FROM openjdk:8u121-jdk

ENV JVM_MEMORY 256m

COPY build/libs/search-api.jar /usr/local/search-api.jar

EXPOSE 4000 8482
ENTRYPOINT java -Xms${JVM_MEMORY} -Xmx${JVM_MEMORY} -server -jar /usr/local/search-api.jar