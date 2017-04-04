FROM openjdk:8u121-jdk

MAINTAINER Marcos A. Sobrinho

ENV ES_HOSTNAME 127.0.0.1
ENV ES_PORT 9300
ENV ES_CLUSTER_NAME elasticsearch
ENV JVM_MEMORY 256m

COPY build/libs/search-api-*.*.*.jar /usr/local/search-api.jar

EXPOSE 4000 8080
WORKDIR /usr/local/
ENTRYPOINT java -Xms${JVM_MEMORY} -Xmx${JVM_MEMORY} -server -jar search-api.jar
CMD ["--es.hostname=${ES_HOSTNAME}", "--es.port=${ES_PORT}", "--es.cluster.name=${ES_CLUSTER_NAME}"]
