package com.grupozap.search.api.configuration;

import static org.elasticsearch.client.RestClient.builder;

import com.grupozap.search.api.model.serializer.SearchResponseEnvelope;
import com.grupozap.search.api.serializer.ESResponseSerializer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
class ApiBeans implements DisposableBean {

  private RestClient restClient = null;
  private RestHighLevelClient restHighLevelClient = null;

  @Value("${es.hostname}")
  private String hostname;

  @Value("${es.rest.port}")
  private Integer restPort;

  @Value("${es.cluster.name}")
  private String clusterName;

  @Bean
  RestHighLevelClient restHighLevelClient(
      @Value("${es.client.socket.timeout}") int socketTimeout,
      @Value("${es.client.conn.timeout}") int connTimeout,
      @Value("${es.client.conn.request.timeout}") int connRequestTimeout,
      @Value("${es.client.http.max.conn.total}") int maxConnTotal,
      @Value("${es.client.http.max.conn.per-route}") int maxConnPerRoute) {
    this.restHighLevelClient =
        new RestHighLevelClient(
            builder(new HttpHost(hostname, restPort, "http"))
                .setHttpClientConfigCallback(
                    http -> http.setMaxConnTotal(maxConnTotal).setMaxConnPerRoute(maxConnPerRoute))
                .setRequestConfigCallback(
                    requestConfigBuilder ->
                        requestConfigBuilder
                            .setConnectionRequestTimeout(connRequestTimeout)
                            .setConnectTimeout(connTimeout)
                            .setSocketTimeout(socketTimeout)));
    return restHighLevelClient;
  }

  @Bean
  RestClient restClient() {
    restClient = RestClient.builder(new HttpHost(hostname, restPort, "http")).build();
    return restClient;
  }

  @Bean
  Jackson2ObjectMapperBuilderCustomizer addCustomSearchResponseDeserialization() {
    return jacksonObjectMapperBuilder ->
        jacksonObjectMapperBuilder.serializerByType(
            SearchResponseEnvelope.class, new ESResponseSerializer());
  }

  @Override
  public void destroy() throws Exception {
    if (restHighLevelClient != null) restHighLevelClient.close();
    if (restClient != null) restClient.close();
  }
}
