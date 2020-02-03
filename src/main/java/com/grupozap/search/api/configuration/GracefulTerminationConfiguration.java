package com.grupozap.search.api.configuration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ThreadPoolExecutor;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

@Configuration
@ConditionalOnProperty(
    value = "application.graceful-termination.enabled",
    matchIfMissing = true,
    havingValue = "true")
class GracefulTerminationConfiguration {

  @Bean
  TomcatConnectorCustomizer gracefulConnector(
      @Value("${application.termination.grace-period:25000}") long gracePeriod) {
    return new GracefulConnector(gracePeriod);
  }

  static class GracefulConnector
      implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

    private final long gracePeriod;
    private Connector connector;

    GracefulConnector(long gracePeriod) {
      this.gracePeriod = gracePeriod;
    }

    @Override
    public void customize(Connector connector) {
      this.connector = connector;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
      connector.pause();
      final var executor = connector.getProtocolHandler().getExecutor();
      if (executor instanceof ThreadPoolExecutor) {
        try {
          final var poolExecutor = (ThreadPoolExecutor) executor;
          poolExecutor.shutdown();
          poolExecutor.awaitTermination(gracePeriod, MILLISECONDS);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
