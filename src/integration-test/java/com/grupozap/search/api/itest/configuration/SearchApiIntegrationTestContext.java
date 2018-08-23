package com.grupozap.search.api.itest.configuration;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ComponentScan(basePackages = "com.vivareal.search.api.itest.configuration")
public class SearchApiIntegrationTestContext {

  @Value("${es.hostname}")
  private String hostname;

  @Value("${es.rest.port}")
  private Integer port;

  private RestClientBuilder getRestClientBuilder() {
    return RestClient.builder(new HttpHost(hostname, port, "http"));
  }

  @Bean
  @Scope(SCOPE_SINGLETON)
  public RestClient restClient() {
    return getRestClientBuilder().build();
  }

  @Bean
  @Scope(SCOPE_SINGLETON)
  public RestHighLevelClient restHighLevelClient() {
    return new RestHighLevelClient(getRestClientBuilder());
  }
}
