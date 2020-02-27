package com.grupozap.search.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.grupozap.search.api.service.MetricsCollector.Metric;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Gauge.class, Counter.class})
public class MetricsCollectorTest {

  @Mock private Counter counter;
  @Mock private Counter.Child counterChild;
  @Mock private Gauge gauge;
  @Mock private Gauge.Child gaugeChild;
  @Mock private CollectorRegistry collectorRegistry;

  @Mock(answer = Answers.RETURNS_SELF)
  private Counter.Builder counterBuilder;

  @Mock(answer = Answers.RETURNS_SELF)
  private Gauge.Builder gaugeBuilder;

  private MetricsCollector subject;

  @Before
  public void setUp() throws Exception {
    mockStatic(Gauge.class);
    mockStatic(Counter.class);

    PowerMockito.when(Gauge.class, "build").thenReturn(gaugeBuilder);
    PowerMockito.when(gaugeBuilder, "register", any()).thenReturn(gauge);
    PowerMockito.when(Counter.class, "build").thenReturn(counterBuilder);
    PowerMockito.when(counterBuilder, "register", any()).thenReturn(counter);
    when(counter.labels(anyString())).thenReturn(counterChild);
    when(gauge.labels(anyString())).thenReturn(gaugeChild);

    subject = new MetricsCollector(collectorRegistry);
  }

  @Test
  public void shouldRecordACount() {
    final var anyIndex = "anyIndex";

    subject.record(Metric.SEARCH_BY_ID_FOUND, anyIndex);

    verify(counter).labels(anyIndex);
    verify(counterChild).inc();
  }

  @Test
  public void shouldNotAttemptToRecordACountInAGaugeCollector() {
    final var anyIndex = "anyIndex";

    subject.record(Metric.SEARCH_BY_ID_LATENCY, anyIndex);

    verify(counter, never()).labels(anyIndex);
    verify(counterChild, never()).inc();
  }

  @Test
  public void shouldRecordAGauge() {
    final var anyIndex = "anyIndex";
    final var anyValue = 1000L;

    subject.record(Metric.SEARCH_BY_ID_LATENCY, anyIndex, anyValue);

    verify(gauge).labels(anyIndex);
    verify(gaugeChild).set(anyValue);
  }

  @Test
  public void shouldNotAttemptToRecordAGaugeValueInACounterCollector() {
    final var anyIndex = "anyIndex";
    final var anyValue = 1000L;

    subject.record(Metric.SEARCH_BY_ID_FOUND, anyIndex, anyValue);

    verify(gauge, never()).labels(anyIndex);
    verify(gaugeChild, never()).set(anyValue);
  }
}
