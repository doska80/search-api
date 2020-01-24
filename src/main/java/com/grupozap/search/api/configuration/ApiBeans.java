package com.grupozap.search.api.configuration;

import static org.elasticsearch.client.RestClient.builder;

import com.grupozap.search.api.model.serializer.SearchResponseEnvelope;
import com.grupozap.search.api.serializer.ESResponseSerializer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
class ApiBeans {

  @Bean
  RestHighLevelClient restHighLevelClient(
      @Value("${es.hostname}") String hostname,
      @Value("${es.rest.port}") Integer port,
      @Value("${es.client.socket.timeout}") int socketTimeout,
      @Value("${es.client.conn.timeout}") int connTimeout,
      @Value("${es.client.http.max.conn.total}") int maxConnTotal,
      @Value("${es.client.http.max.conn.per-route}") int maxConnPerRoute) {
    return new RestHighLevelClient(
        builder(new HttpHost(hostname, port, "http"))
            .setHttpClientConfigCallback(
                http -> http.setMaxConnTotal(maxConnTotal).setMaxConnPerRoute(maxConnPerRoute))
            .setRequestConfigCallback(
                requestConfigBuilder ->
                    requestConfigBuilder
                        .setConnectTimeout(connTimeout)
                        .setSocketTimeout(socketTimeout)));
  }

  @Bean
  RestClient restClient(RestHighLevelClient restHighLevelClient) {
    return restHighLevelClient.getLowLevelClient();
  }

  @Bean
  Jackson2ObjectMapperBuilderCustomizer addCustomSearchResponseDeserialization() {
    return jacksonObjectMapperBuilder ->
        jacksonObjectMapperBuilder.serializerByType(
            SearchResponseEnvelope.class, new ESResponseSerializer());
  }
}
