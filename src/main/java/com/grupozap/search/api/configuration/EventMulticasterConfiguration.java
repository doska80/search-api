package com.grupozap.search.api.configuration;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;

@Configuration
class EventMulticasterConfiguration {

  @Bean
  ApplicationEventMulticaster simpleApplicationEventMulticaster(Executor multicasterExecutor) {
    var multicaster = new SimpleApplicationEventMulticaster();
    multicaster.setTaskExecutor(multicasterExecutor);
    return multicaster;
  }

  @Bean
  Executor multicasterExecutor() {
    return newSingleThreadExecutor();
  }
}
