package com.vivareal.search.api.itest.configuration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.vivareal.search.api.itest.configuration")
public class SearchApiIntegrationTestContext {

    @Bean
    public RestClient elasticSearchClientBean(@Value("${es.hostname}") final String hostname,
                                              @Value("${es.rest.port}") final Integer port) {
        return RestClient.builder(new HttpHost(hostname, port, "http")).build();
    }
}
