package com.vivareal.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.vivareal.search.api.utils.FlattenMapUtils.flat;
import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.startsWith;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.vivareal.search.api.exception.IndexNotFoundException;
import com.vivareal.search.api.exception.InvalidFieldException;
import com.vivareal.search.api.exception.PropertyNotFoundException;
import com.vivareal.search.api.model.event.ClusterSettingsUpdatedEvent;
import com.vivareal.search.api.model.mapping.MappingType;
import com.vivareal.search.api.model.search.Indexable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("elasticsearchSettings")
@DependsOn("fieldFactory")
public class ElasticsearchSettingsAdapter
    implements SettingsAdapter<Map<String, Map<String, Object>>, String> {

  public static final String SHARDS = "index.number_of_shards";
  public static final String REPLICAS = "index.number_of_replicas";
  public static final Set<String> WHITE_LIST_METAFIELDS =
      unmodifiableSet(newHashSet("_id", "_score"));
  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchSettingsAdapter.class);

  private final ApplicationEventPublisher applicationEventPublisher;
  private final ESClient esClient;

  private Map<String, Map<String, Object>> structuredIndices;

  @Autowired
  public ElasticsearchSettingsAdapter(
      ApplicationEventPublisher applicationEventPublisher, ESClient esClient) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.esClient = esClient;
    this.structuredIndices = new HashMap<>();

    loadSettingsInformationFromCluster();
  }

  @Override
  public Map<String, Map<String, Object>> settings() {
    return structuredIndices;
  }

  @Override
  public String settingsByKey(final String index, final String property) {
    if (!structuredIndices.get(index).containsKey(property))
      throw new PropertyNotFoundException(property, index);

    return valueOf(structuredIndices.get(index).get(property));
  }

  @Override
  public void checkIndex(final Indexable request) {
    if (!structuredIndices.containsKey(request.getIndex()))
      throw new IndexNotFoundException(request.getIndex());
  }

  @Override
  public boolean checkFieldName(
      final String index, final String fieldName, final boolean acceptAsterisk) {
    if ((acceptAsterisk && "*".equals(fieldName)) || WHITE_LIST_METAFIELDS.contains(fieldName))
      return true;

    if (!structuredIndices.get(index).containsKey(fieldName))
      throw new InvalidFieldException(fieldName, index);

    return true;
  }

  @Override
  public String getFieldType(final String index, final String fieldName) {
    return valueOf(structuredIndices.get(index).get(fieldName));
  }

  @Override
  public boolean isTypeOf(final String index, final String fieldName, final MappingType type) {
    return type.typeOf(getFieldType(index, fieldName));
  }

  @Scheduled(fixedRateString = "${es.settings.refresh.rate.ms}")
  private void loadSettingsInformationFromCluster() {
    GetIndexResponse getIndexResponse = esClient.getIndexResponse();
    Map<String, Map<String, Object>> structuredIndicesAux = new HashMap<>();

    stream(getIndexResponse.getIndices())
        .filter(index -> !startsWith(index, "."))
        .forEach(
            index -> {
              Map<String, Object> indexInfo = newConcurrentMap();
              indexInfo.putAll(
                  getIndexResponse
                      .getSettings()
                      .get(index)
                      .filter(newArrayList(SHARDS, REPLICAS)::contains)
                      .getAsMap());

              ImmutableOpenMap<String, MappingMetaData> immutableIndexMapping =
                  getIndexResponse.getMappings().get(index);
              immutableIndexMapping
                  .keys()
                  .iterator()
                  .forEachRemaining(
                      obj -> getMappingFromType(obj, indexInfo, immutableIndexMapping, index));

              structuredIndicesAux.put(index, indexInfo);
            });

    if (!structuredIndicesAux.isEmpty()) {
      structuredIndices = structuredIndicesAux;
    }

    LOG.debug("Refresh getting information from cluster settings executed with success");
    applicationEventPublisher.publishEvent(
        new ClusterSettingsUpdatedEvent(this, structuredIndicesAux));
  }

  private void getMappingFromType(
      ObjectCursor<String> stringObjectCursor,
      Map<String, Object> indexInfo,
      ImmutableOpenMap<String, MappingMetaData> immutableIndexMapping,
      String index) {
    String type = stringObjectCursor.value;
    try {
      indexInfo.putAll(
          flat(
              immutableIndexMapping.get(type).getSourceAsMap(),
              newArrayList("mappings", "properties", "type", "fields", index, type)));
      indexInfo
          .entrySet()
          .stream()
          .filter(stringObjectEntry -> stringObjectEntry.getKey().contains("."))
          .map(
              stringObjectEntry -> {
                Set<String> missingParams = new HashSet<>();
                StringBuilder composeMissingParams = new StringBuilder();
                Stream.of(stringObjectEntry.getKey().split("\\."))
                    .forEach(
                        param -> {
                          composeMissingParams.append(param);
                          missingParams.add(composeMissingParams.toString());
                          composeMissingParams.append(".");
                        });
                return missingParams;
              })
          .forEach(params -> params.forEach(param -> indexInfo.putIfAbsent(param, "_obj")));
    } catch (Exception e) {
      LOG.error("Error on get mapping from index {} and type {}", index, type, e);
    }
  }
}
