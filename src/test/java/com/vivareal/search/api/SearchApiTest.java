package com.vivareal.search.api;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@ComponentScan(basePackages = {"com.vivareal.search.api.adapter",
        "com.vivareal.search.api.configuration",
        "com.vivareal.search.api.controller",
        "com.vivareal.search.api.service"})
public class SearchApiTest {
    // TODO check for viability: https://github.com/palantir/docker-compose-rule
}
