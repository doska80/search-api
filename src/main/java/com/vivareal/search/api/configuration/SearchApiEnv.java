package com.vivareal.search.api.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.vivareal.search.api.configuration.SearchApiEnv.RemoteProperties.*;
import static java.util.Arrays.stream;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Created by leandropereirapinto on 6/29/17.
 */
@Component
@Scope(SCOPE_SINGLETON)
@PropertySource("classpath:application.properties")
public class SearchApiEnv {

    private static Logger LOG = LoggerFactory.getLogger(SearchApiEnv.class);

    private final RestClient restClient;

    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, Object> localProperties = new HashMap<>();
    private Map<String, Object> remoteProperties = new HashMap<>();

    public SearchApiEnv(Environment env, RestClient restClient) {
        this.restClient = restClient;
        ((AbstractEnvironment) env).getPropertySources().iterator().forEachRemaining(propertySource -> {
            if (propertySource instanceof MapPropertySource) {
                normalizeMap(((MapPropertySource) propertySource).getSource());
            }
        });
        loadEnvironmentProperties(this.localProperties);
        loadRemoteProperties();
    }

    private void normalizeMap(final Map<String, Object> localProperties) {
        localProperties.forEach((k, v) -> this.localProperties.put(k, String.valueOf(v)));
    }

    @Scheduled(cron = "${application.properties.refresh.cron}")
    private void loadRemoteProperties() {

        Header headers = new BasicHeader("Content-Type", "application/json; charset=UTF-8");
        String endpoint = String.format("/%s/%s/%s", APP_PROPERTIES_INDEX.getValue(), APP_PROPERTIES_TYPE.getValue(), PROFILE.getValue());

        try {
            HttpEntity entity = restClient.performRequest("GET", endpoint, headers).getEntity();

            if (entity != null) {
                String retSrc = EntityUtils.toString(entity);
                HashMap<String, Object> response = mapper.readValue(retSrc, new TypeReference<HashMap<String, Object>>() {});
                if (!isEmpty(response) && response.containsKey("_source")) {

                    @SuppressWarnings("unchecked")
                    HashMap<String, Object> source = (HashMap<String, Object>) response.get("_source");

                    this.remoteProperties.clear();
                    this.remoteProperties.putAll(source);

                    LOG.debug("Remote properties loaded with success. Endpoint: {}", endpoint);
                    loadEnvironmentProperties(source);
                } else {
                    LOG.warn("Remote properties cannot loaded because the profile {} not found on ES Index or content is empty.", PROFILE.getValue());
                }
            }
        } catch (ResponseException e) {
            LOG.error("Error to get response from endpoint {}. ErrorMessage: {}", endpoint, e.getMessage());
        } catch (IOException e) {
            LOG.error("Generic error to get response from endpoint {}. ErrorMessage: {}", endpoint, e.getMessage());
        }
    }

    private void loadEnvironmentProperties(final Map<String, Object> properties) {
        stream(RemoteProperties.values()).parallel().forEach(env -> {
            if (properties.containsKey(env.getProperty()))
                env.setValue(String.valueOf(properties.get(env.getProperty())));
        });
        LOG.debug("Environment Properties loaded with success");
    }

    public Map<String, Object> getLocalProperties() {
        return localProperties;
    }

    public Map<String, Object> getRemoteProperties() {
        return remoteProperties;
    }

    public enum RemoteProperties {

        PROFILE("spring.profiles.active"),
        QS_MM("querystring.default.mm"),
        QS_DEFAULT_FIELDS("querystring.default.fields"),
        ES_HOSTNAME("es.hostname"),
        ES_PORT("es.port"),
        ES_REST_PORT("es.rest.port"),
        ES_CLUSTER_NAME("es.cluster.name"),
        ES_DEFAULT_SIZE("es.default.size"),
        ES_MAX_SIZE("es.max.size"),
        ES_FACET_SIZE("es.facet.size"),
        ES_CONTROLLER_SEARCH_TIMEOUT("es.controller.search.timeout"),
        ES_CONTROLLER_STREAM_TIMEOUT("es.controller.stream.timeout"),
        ES_STREAM_SIZE("es.stream.size"),
        ES_SCROLL_TIMEOUT("es.scroll.timeout"),
        SOURCE_INCLUDES("source.default.includes"),
        SOURCE_EXCLUDES("source.default.excludes"),
        APP_PROPERTIES_INDEX("application.properties.index"),
        APP_PROPERTIES_TYPE("application.properties.type");

        RemoteProperties(String property) {
            this.property = property;
            this.value = "";
        }

        private String property;
        private String value;

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
