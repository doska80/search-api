package com.vivareal.search.api.adapter;

import com.google.common.collect.Lists;
import com.vivareal.search.api.exception.IndexNotFoundException;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Created by leandropereirapinto on 6/29/17.
 */
@Scope(SCOPE_SINGLETON)
@Component("ElasticsearchSettings")
public class ElasticsearchSettingsAdapter implements SettingsAdapter<Map<String, Map<String, Object>>, String> {

    private static Logger LOG = LoggerFactory.getLogger(ElasticsearchSettingsAdapter.class);

    @Autowired
    private TransportClient transportClient;

    private Map<String, Map<String, Object>> structuredIndices = new HashMap<>();
    private Map<String, Map<String, String>> mapIndices = new HashMap<>();

    public static final String SHARDS = "index.number_of_shards";
    public static final String REPLICAS = "index.number_of_replicas";

    @Override
    public Map<String, Map<String, Object>> settings() {
        if (isEmpty(structuredIndices))
            getSettingsInformationFromCluster();
        return structuredIndices;
    }

    @Override
    public String settingsByKey(final String index, final String key) {
        if (isEmpty(mapIndices))
            getSettingsInformationFromCluster();

        if (!mapIndices.containsKey(index))
            throw new IndexNotFoundException(index);

        return mapIndices.get(index).get(key);
    }

    @Scheduled(cron = "${es.settings.refresh.cron}")
    private void getSettingsInformationFromCluster() {
        GetIndexResponse getIndexResponse = this.transportClient.admin().indices().prepareGetIndex().get();

        Predicate<String> acceptSettings = new Predicate<String>() {
            List<String> validSettings = Lists.newArrayList(SHARDS, REPLICAS);

            @Override
            public boolean test(String s) {
                return validSettings.contains(s);
            }
        };

        stream(this.transportClient.admin().indices().prepareGetIndex().get().getIndices()).filter(a -> !startsWith(a, ".")).forEach(
            index -> {
                Settings settings = getIndexResponse.getSettings().get(index).filter(acceptSettings);
                structuredIndices.put(index, settings.getAsStructuredMap());
                mapIndices.put(index, settings.getAsMap());
            }
        );
        LOG.debug("Refresh Elasticsearch settings executed with success");
    }
}
