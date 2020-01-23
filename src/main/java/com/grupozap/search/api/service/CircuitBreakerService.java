package com.grupozap.search.api.service;

import static java.lang.String.format;
import static java.util.Arrays.stream;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class CircuitBreakerService {

  @Autowired private CircuitBreakerRegistry registry;
  @Autowired private Environment env;

  public List<CircuitBreakerInfo> list() {
    return registry
        .getAllCircuitBreakers()
        .map(cb -> new CircuitBreakerInfo(cb.getName(), cb.getState(), cb.getMetrics()))
        .asJava();
  }

  public <T> T execute(CircuitType type, String index, Supplier<T> action) {
    final var circuit = circuitBreakerFor(type, index);
    return circuit.executeSupplier(action);
  }

  public CircuitBreaker circuitBreakerFor(CircuitType type, String index) {
    return registry.circuitBreaker(format("%s-%s", type, index), type.name());
  }

  public void modifyAll(State state) {
    if (stream(env.getActiveProfiles()).noneMatch("test"::equalsIgnoreCase)) {
      return;
    }
    final Consumer<CircuitBreaker> action;
    switch (state) {
      case OPEN:
        action = CircuitBreaker::transitionToOpenState;
        break;
      case FORCED_OPEN:
        action = CircuitBreaker::transitionToForcedOpenState;
        break;
      case HALF_OPEN:
        action = CircuitBreaker::transitionToHalfOpenState;
        break;
      case DISABLED:
        action = CircuitBreaker::transitionToDisabledState;
        break;
      case CLOSED:
        action = CircuitBreaker::transitionToClosedState;
        break;
      default:
        throw new IllegalArgumentException("unknown state");
    }
    registry
        .getAllCircuitBreakers()
        .forEach(
            cb -> {
              try {
                action.accept(cb);
              } catch (Exception ignored) {
              }
            });
  }

  public enum CircuitType {
    GET_BY_ID,
    SEARCH
  }

  public static class CircuitBreakerInfo {
    private final String name;
    private final State state;
    private final Metrics metrics;

    public CircuitBreakerInfo(String name, State state, Metrics metrics) {
      this.name = name;
      this.state = state;
      this.metrics = metrics;
    }

    public String getName() {
      return name;
    }

    public State getState() {
      return state;
    }

    public Metrics getMetrics() {
      return metrics;
    }
  }
}
