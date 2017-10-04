package com.vivareal.search.api.itest.configuration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Configuration
@ComponentScan(basePackages = "com.vivareal.search.api.itest.configuration")
public class SearchApiIntegrationTestContext {

    @Value("${es.hostname}")
    private String hostname;

    @Value("${es.rest.port}")
    private Integer port;

    @Bean
    @Scope(SCOPE_SINGLETON)
    public RestClient restClient() {
        return RestClient.builder(new HttpHost(hostname, port, "http")).build();
    }

    @Bean
    @Scope(SCOPE_SINGLETON)
    public RestHighLevelClient restHighLevelClient() {
        return new RestHighLevelClient(restClient());
    }
}
