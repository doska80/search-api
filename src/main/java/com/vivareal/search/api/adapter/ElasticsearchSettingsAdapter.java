package com.vivareal.search.api.adapter;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.vivareal.search.api.exception.IndexNotFoundException;
import com.vivareal.search.api.exception.InvalidFieldException;
import com.vivareal.search.api.exception.PropertyNotFoundException;
import com.vivareal.search.api.model.search.Indexable;
import com.vivareal.search.api.model.mapping.MappingType;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newConcurrentMap;
import static com.vivareal.search.api.utils.FlattenMapUtils.flat;
import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.util.CollectionUtils.isEmpty;

@Scope(SCOPE_SINGLETON)
@Component("elasticsearchSettings")
public class ElasticsearchSettingsAdapter implements SettingsAdapter<Map<String, Map<String, Object>>, String> {

    private static Logger LOG = LoggerFactory.getLogger(ElasticsearchSettingsAdapter.class);

    @Autowired
    private TransportClient transportClient;

    private Map<String, Map<String, Object>> structuredIndices = new HashMap<>();

    public static final String SHARDS = "index.number_of_shards";
    public static final String REPLICAS = "index.number_of_replicas";

    @Override
    public Map<String, Map<String, Object>> settings() {
        if (isEmpty(structuredIndices))
            getSettingsInformationFromCluster();
        return structuredIndices;
    }

    @Override
    public String settingsByKey(final String index, final String property) {
        if (isEmpty(structuredIndices))
            getSettingsInformationFromCluster();

        if (!structuredIndices.get(index).containsKey(property))
            throw new PropertyNotFoundException(property, index);

        return valueOf(structuredIndices.get(index).get(property));
    }

    @Override
    public void checkIndex(final Indexable request) {
        if (isEmpty(structuredIndices))
            getSettingsInformationFromCluster();

        if (!structuredIndices.containsKey(request.getIndex()))
            throw new IndexNotFoundException(request.getIndex());
    }

    @Override
    public void checkFieldName(final String index, final String fieldName) {
        if (isEmpty(structuredIndices))
            getSettingsInformationFromCluster();

        if (!structuredIndices.get(index).containsKey(fieldName))
            throw new InvalidFieldException(fieldName, index);
    }

    @Override
    public String getFieldType(final String index, final String fieldName) {
        checkFieldName(index, fieldName);
        return valueOf(structuredIndices.get(index).get(fieldName));
    }

    @Override
    public boolean isTypeOf(final String index, final String fieldName, final MappingType type) {
        return type.typeOf(getFieldType(index, fieldName));
    }

    @Scheduled(cron = "${es.settings.refresh.cron}")
    private void getSettingsInformationFromCluster() {
        GetIndexResponse getIndexResponse = this.transportClient.admin().indices().prepareGetIndex().get();
        Map<String, Map<String, Object>> structuredIndicesAux = new HashMap<>();

        stream(this.transportClient.admin().indices().prepareGetIndex().get().getIndices()).filter(a -> !startsWith(a, ".")).forEach(
            index -> {
                Map<String, Object> indexInfo = newConcurrentMap();
                indexInfo.putAll(getIndexResponse.getSettings().get(index).filter(newArrayList(SHARDS, REPLICAS)::contains).getAsMap());

                ImmutableOpenMap<String, MappingMetaData> immutableIndexMapping = getIndexResponse.getMappings().get(index);
                immutableIndexMapping.keys().iterator().forEachRemaining(obj -> getMappingFromType(obj, indexInfo, immutableIndexMapping, index));

                structuredIndicesAux.put(index, indexInfo);
            }
        );

        if (!structuredIndicesAux.isEmpty()) {
            structuredIndices.clear();
            structuredIndices.putAll(structuredIndicesAux);
        }

        LOG.debug("Refresh Elasticsearch settings executed with success");
    }

    private void getMappingFromType(ObjectCursor<String> stringObjectCursor, Map<String, Object> indexInfo, ImmutableOpenMap<String, MappingMetaData> immutableIndexMapping, String index) {
        String type = stringObjectCursor.value;
        try {
            indexInfo.putAll(flat(immutableIndexMapping.get(type).getSourceAsMap(), newArrayList("mappings", "properties", "type", "fields", index, type)));
            indexInfo.entrySet().stream()
                .filter(stringObjectEntry -> stringObjectEntry.getKey().contains("."))
                .map(stringObjectEntry -> {
                    Set<String> missingParams = new HashSet<>();
                    StringBuilder composeMissingParams = new StringBuilder();
                    Stream.of(stringObjectEntry.getKey().split("\\."))
                        .forEach(
                            param -> {
                                composeMissingParams.append(param);
                                missingParams.add(composeMissingParams.toString());
                                composeMissingParams.append(".");
                            }
                        );
                    return missingParams;
                })
                .forEach(params -> params
                    .forEach(param -> indexInfo.putIfAbsent(param, "_obj"))
                );
        } catch (IOException e) {
            LOG.error("Error on get mapping from index {} and type {}", index, type, e);
        }
    }
}
