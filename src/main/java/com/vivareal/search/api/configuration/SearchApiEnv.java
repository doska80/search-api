package com.vivareal.search.api.configuration;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.vivareal.search.api.configuration.SearchApiEnv.RemoteProperties.*;
import static java.util.Arrays.stream;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

/**
 * Created by leandropereirapinto on 6/29/17.
 */
@Component
@Scope(SCOPE_SINGLETON)
@PropertySource("classpath:application.properties")
public class SearchApiEnv {

    private static Logger LOG = LoggerFactory.getLogger(SearchApiEnv.class);

    private final TransportClient transportClient;

    private Map<String, Object> localProperties = new HashMap<>();
    private Map<String, Object> remoteProperties = new HashMap<>();

    public SearchApiEnv(Environment env, TransportClient transportClient) {
        this.transportClient = transportClient;
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
        GetRequestBuilder requestBuilder = transportClient.prepareGet().setIndex(APP_PROPERTIES_INDEX.getValue()).setType(APP_PROPERTIES_TYPE.getValue()).setId(PROFILE.getValue());
        GetResponse getResponse = requestBuilder.get();
        if (getResponse.isExists() && !getResponse.getSourceAsMap().isEmpty()) {
            LOG.debug("Remote properties loaded with success. {}", requestBuilder.request());
            this.remoteProperties.clear();
            this.remoteProperties.putAll(getResponse.getSourceAsMap());
            loadEnvironmentProperties(this.remoteProperties);
        } else {
            LOG.warn("Remote properties cannot loaded because the profile {} not found on ES Index or content is empty.", PROFILE.getValue());
        }
    }

    private void loadEnvironmentProperties(final Map<String, Object> properties) {
        stream(RemoteProperties.values()).parallel().forEach(env -> {
            if (properties.containsKey(env.getProperty()))
                env.setValue(String.valueOf(properties.get(env.getProperty())));
        });
    }

    public Map<String, Object> getLocalProperties() {
        return localProperties;
    }

    public Map<String, Object> getRemoteProperties() {
        return remoteProperties;
    }

    public enum RemoteProperties {

        PROFILE("spring.profiles.active", ""),
        QS_MM("querystring.default.mm", ""),
        QS_DEFAULT_FIELDS("querystring.default.fields", ""),
        ES_CLUSTER_NAME("es.cluster.name", ""),
        ES_DEFAULT_SIZE("es.default.size", ""),
        ES_FACET_SIZE("es.facet.size", ""),
        SOURCE_INCLUDES("source.default.includes", ""),
        SOURCE_EXCLUDES("source.default.excludes", ""),
        APP_PROPERTIES_INDEX("application.properties.index", ""),
        APP_PROPERTIES_TYPE("application.properties.type", "");

        RemoteProperties(String property, String value) {
            this.property = property;
            this.value = value;
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
