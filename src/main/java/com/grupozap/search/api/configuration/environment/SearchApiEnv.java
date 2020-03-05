package com.grupozap.search.api.configuration.environment;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.DEFAULT_INDEX;
import static com.grupozap.search.api.utils.MapperUtils.parser;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.math.NumberUtils.createNumber;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("searchApiEnv")
@DependsOn("elasticsearchSettings")
public class SearchApiEnv {

  private static final Logger LOG = LoggerFactory.getLogger(SearchApiEnv.class);

  private static final String SEARCH_API_CONFIG_ENDPOINT =
      "/search-api-properties/_search?size=100";
  private static final Header SEARCH_API_HEADERS =
      new BasicHeader("Content-Type", "application/json; charset=UTF-8");

  private final ApplicationEventPublisher applicationEventPublisher;
  private final RestClient restClient;

  private final Map<String, Object> localProperties = new HashMap<>();
  private final Map<String, Object> remoteProperties = new HashMap<>();

  public SearchApiEnv(
      Environment env, ApplicationEventPublisher applicationEventPublisher, RestClient restClient) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.restClient = restClient;

    loadLocalProperties((AbstractEnvironment) env);
    loadRemoteProperties();
  }

  private void loadLocalProperties(AbstractEnvironment environment) {
    StreamSupport.stream(environment.getPropertySources().spliterator(), false)
        .filter(propertySource -> propertySource instanceof MapPropertySource)
        .map(mapPropertySource -> (Map<String, Object>) mapPropertySource.getSource())
        .forEach(
            sourceMap ->
                sourceMap.forEach(
                    (k, v) -> {
                      this.localProperties.put(
                          k, parseLocalEnvironmentValue(environment.getProperty(k)));
                    }));
    loadEnvironmentProperties(DEFAULT_INDEX, this.localProperties);
  }

  private Object parseLocalEnvironmentValue(String propertyValue) {
    if (isCreatable(propertyValue)) {
      return createNumber(propertyValue);
    } else if (TRUE.toString().equalsIgnoreCase(propertyValue)
        || FALSE.toString().equalsIgnoreCase(propertyValue)) {
      return parseBoolean(propertyValue);
    }
    return propertyValue;
  }

  @SuppressWarnings("unchecked")
  @Scheduled(fixedRateString = "${application.properties.refresh.rate.ms}")
  private void loadRemoteProperties() {
    try {

      final var request = new Request(GET.name(), SEARCH_API_CONFIG_ENDPOINT);
      final var options = RequestOptions.DEFAULT.toBuilder();
      options.addHeader(SEARCH_API_HEADERS.getName(), SEARCH_API_HEADERS.getValue());
      request.setOptions(options);

      var entity = restClient.performRequest(request).getEntity();

      if (entity != null) {
        final HashMap<String, Object> response = parser(EntityUtils.toString(entity));
        if (!isEmpty(response) && response.containsKey("hits")) {

          final Map hitsMap = (HashMap<String, Object>) response.get("hits");

          if (hitsMap.containsKey("hits")) {
            ((List<Map>) hitsMap.get("hits"))
                .forEach(
                    hit -> {
                      var source = (Map<String, Object>) hit.get("_source");

                      LOG.debug(
                          "Remote properties loaded with success. Endpoint: {}",
                          SEARCH_API_CONFIG_ENDPOINT);
                      var index = hit.get("_id").toString();
                      loadEnvironmentProperties(index, source);
                      this.remoteProperties.put(index, source);
                    });
          }
        }
      }
    } catch (ResponseException e) {
      LOG.error(
          "Error to get response from endpoint {}. ErrorMessage: {}",
          SEARCH_API_CONFIG_ENDPOINT,
          e.getMessage());
    } catch (IOException e) {
      LOG.error(
          "Generic error to get response from endpoint {}. ErrorMessage: {}",
          SEARCH_API_CONFIG_ENDPOINT,
          e.getMessage());
    }
  }

  private void loadEnvironmentProperties(final String index, final Map<String, Object> properties) {
    stream(RemoteProperties.values())
        .parallel()
        .filter(remoteProperty -> properties.containsKey(remoteProperty.getProperty()))
        .forEach(env -> env.setValue(index, properties.get(env.getProperty())));
    LOG.debug("Environment Properties loaded with success");

    applicationEventPublisher.publishEvent(new RemotePropertiesUpdatedEvent(this, index));
    LOG.debug("Environment properties refresh triggered");
  }

  public Map<String, Object> getLocalProperties() {
    return localProperties;
  }

  public Map<String, Object> getRemoteProperties() {
    return remoteProperties;
  }
}
