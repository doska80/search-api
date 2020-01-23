package com.grupozap.search.api.configuration;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;

@Configuration
class AsynchronousSpringEventsConfig {

  @Bean
  ApplicationEventMulticaster simpleApplicationEventMulticaster() {
    var eventMulticaster = new SimpleApplicationEventMulticaster();
    eventMulticaster.setTaskExecutor(newSingleThreadExecutor());
    return eventMulticaster;
  }
}
