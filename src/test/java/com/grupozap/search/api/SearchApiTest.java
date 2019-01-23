package com.grupozap.search.api;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@ComponentScan(
    basePackages = {
      "com.grupozap.search.api.adapter",
      "com.grupozap.search.api.controller",
      "com.grupozap.search.api.service"
    })
public class SearchApiTest {
  // TODO check for viability: https://github.com/palantir/docker-compose-rule
}
