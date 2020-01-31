package com.grupozap.search.api.configuration;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.elasticsearch.client.RestClient.builder;

import com.grupozap.search.api.model.serializer.SearchResponseEnvelope;
import com.grupozap.search.api.serializer.ESResponseSerializer;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ElasticsearchClientConfiguration {

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

  @Bean
  ApplicationListener<ApplicationReadyEvent> readyListener(
      @Value("${es.client.warm.threads:5}") int threads,
      @Value("${es.client.warm.requests:128}") int requests,
      @Value("${es.client.warm.timeout:5000}") long timeout,
      RestHighLevelClient client) {
    return e -> warmClient(client, threads, requests, timeout);
  }

  private void warmClient(RestHighLevelClient client, int threads, int requests, long timeout) {
    if (threads <= 0 || requests <= 0) {
      return;
    }
    final Runnable task =
        () -> {
          try {
            client.ping(RequestOptions.DEFAULT);
          } catch (IOException ignored) {
          }
        };
    final var executor = newFixedThreadPool(threads);
    try {
      for (var i = 0; i < requests; i++) {
        executor.submit(task);
      }
      executor.shutdown();
      executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }
}
