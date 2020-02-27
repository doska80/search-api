package com.grupozap.search.api.service;

import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_ERRORS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_FOUND;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_LATENCY;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_NOT_FOUND;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_REQUESTS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_ERRORS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_LATENCY;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_REQUESTS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_WITHOUT_RESULTS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_WITH_RESULTS;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.SimpleCollector;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MetricsCollector {

  private static final String INDEX_LABEL = "index";

  private final Map<Metric, SimpleCollector<?>> collectors;

  public MetricsCollector(CollectorRegistry collectorRegistry) {
    this.collectors = new EnumMap<>(Metric.class);
    this.collectors.put(
        SEARCH_BY_ID_REQUESTS, createCounter(SEARCH_BY_ID_REQUESTS, collectorRegistry));
    this.collectors.put(SEARCH_BY_ID_FOUND, createCounter(SEARCH_BY_ID_FOUND, collectorRegistry));
    this.collectors.put(
        SEARCH_BY_ID_NOT_FOUND, createCounter(SEARCH_BY_ID_NOT_FOUND, collectorRegistry));
    this.collectors.put(SEARCH_BY_ID_ERRORS, createCounter(SEARCH_BY_ID_ERRORS, collectorRegistry));
    this.collectors.put(SEARCH_BY_ID_LATENCY, createGauge(SEARCH_BY_ID_LATENCY, collectorRegistry));

    this.collectors.put(SEARCH_REQUESTS, createCounter(SEARCH_REQUESTS, collectorRegistry));
    this.collectors.put(SEARCH_WITH_RESULTS, createCounter(SEARCH_WITH_RESULTS, collectorRegistry));
    this.collectors.put(
        SEARCH_WITHOUT_RESULTS, createCounter(SEARCH_WITHOUT_RESULTS, collectorRegistry));
    this.collectors.put(SEARCH_ERRORS, createCounter(SEARCH_ERRORS, collectorRegistry));
    this.collectors.put(SEARCH_LATENCY, createGauge(SEARCH_LATENCY, collectorRegistry));
  }

  public void record(Metric metric, String index) {
    final var collector = collectors.get(metric);
    if (collector instanceof Counter) {
      ((Counter) collectors.get(metric)).labels(index).inc();
    }
  }

  public void record(Metric metric, String index, long value) {
    final var collector = collectors.get(metric);
    if (collector instanceof Gauge) {
      ((Gauge) collectors.get(metric)).labels(index).set(value);
    }
  }

  private Counter createCounter(Metric metric, CollectorRegistry collectorRegistry) {
    return Counter.build()
        .name(metric.name().toLowerCase())
        .help(metric.name().toLowerCase())
        .labelNames(INDEX_LABEL)
        .register(collectorRegistry);
  }

  private Gauge createGauge(Metric metric, CollectorRegistry collectorRegistry) {
    return Gauge.build()
        .name(metric.name().toLowerCase())
        .help(metric.name().toLowerCase())
        .labelNames(INDEX_LABEL)
        .register(collectorRegistry);
  }

  enum Metric {
    SEARCH_BY_ID_REQUESTS,
    SEARCH_BY_ID_FOUND,
    SEARCH_BY_ID_NOT_FOUND,
    SEARCH_BY_ID_ERRORS,
    SEARCH_BY_ID_LATENCY,
    SEARCH_REQUESTS,
    SEARCH_WITH_RESULTS,
    SEARCH_WITHOUT_RESULTS,
    SEARCH_ERRORS,
    SEARCH_LATENCY
  }
}
