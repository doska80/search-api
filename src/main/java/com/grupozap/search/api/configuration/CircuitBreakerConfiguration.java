package com.grupozap.search.api.configuration;

import static com.grupozap.search.api.service.CircuitBreakerService.CircuitType.GET_BY_ID;
import static com.grupozap.search.api.service.CircuitBreakerService.CircuitType.SEARCH;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom;
import static java.time.Duration.ofSeconds;
import static java.util.Map.of;

import com.grupozap.search.api.configuration.CircuitBreakerConfiguration.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.jparsec.error.ParserException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CircuitBreakerProperties.class)
class CircuitBreakerConfiguration {

  @Bean
  CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerProperties properties) {
    return CircuitBreakerRegistry.of(
        of(
            SEARCH.name(),
            builder()
                .failureRateThreshold(properties.getSearch().getFailureRateThreshold())
                .minimumNumberOfCalls(properties.getSearch().getMinimumNumberOfCalls())
                .slidingWindowSize(properties.getSearch().getSlidingWindowSize())
                .waitDurationInOpenState(properties.getSearch().getWaitDurationInOpenState())
                .build(),
            GET_BY_ID.name(),
            builder()
                .failureRateThreshold(properties.getGetById().getFailureRateThreshold())
                .minimumNumberOfCalls(properties.getGetById().getMinimumNumberOfCalls())
                .slidingWindowSize(properties.getGetById().getSlidingWindowSize())
                .waitDurationInOpenState(properties.getGetById().getWaitDurationInOpenState())
                .build()));
  }

  private CircuitBreakerConfig.Builder builder() {
    return custom()
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .ignoreExceptions(
            IllegalArgumentException.class, ParserException.class, InvalidPropertyException.class);
  }

  @ConfigurationProperties(prefix = "search-api.circuit-breaker")
  static class CircuitBreakerProperties {

    private CircuitBreakerSettings search = new CircuitBreakerSettings(70, 30, 100, ofSeconds(10L));
    private CircuitBreakerSettings getById =
        new CircuitBreakerSettings(90, 100, 200, ofSeconds(5L));

    public CircuitBreakerSettings getSearch() {
      return search;
    }

    public void setSearch(CircuitBreakerSettings search) {
      this.search = search;
    }

    public CircuitBreakerSettings getGetById() {
      return getById;
    }

    public void setGetById(CircuitBreakerSettings getById) {
      this.getById = getById;
    }
  }

  static class CircuitBreakerSettings {

    private float failureRateThreshold;
    private int minimumNumberOfCalls;
    private int slidingWindowSize;
    private Duration waitDurationInOpenState;

    public CircuitBreakerSettings() {}

    public CircuitBreakerSettings(
        float failureRateThreshold,
        int minimumNumberOfCalls,
        int slidingWindowSize,
        Duration waitDurationInOpenState) {
      this.failureRateThreshold = failureRateThreshold;
      this.minimumNumberOfCalls = minimumNumberOfCalls;
      this.slidingWindowSize = slidingWindowSize;
      this.waitDurationInOpenState = waitDurationInOpenState;
    }

    public float getFailureRateThreshold() {
      return failureRateThreshold;
    }

    public void setFailureRateThreshold(float failureRateThreshold) {
      this.failureRateThreshold = failureRateThreshold;
    }

    public int getMinimumNumberOfCalls() {
      return minimumNumberOfCalls;
    }

    public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
      this.minimumNumberOfCalls = minimumNumberOfCalls;
    }

    public int getSlidingWindowSize() {
      return slidingWindowSize;
    }

    public void setSlidingWindowSize(int slidingWindowSize) {
      this.slidingWindowSize = slidingWindowSize;
    }

    public Duration getWaitDurationInOpenState() {
      return waitDurationInOpenState;
    }

    public void setWaitDurationInOpenState(Duration waitDurationInOpenState) {
      this.waitDurationInOpenState = waitDurationInOpenState;
    }
  }
}
