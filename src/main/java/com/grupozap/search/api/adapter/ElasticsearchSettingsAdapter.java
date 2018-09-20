package com.grupozap.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.grupozap.search.api.model.mapping.MappingType.FIELD_TYPE_OBJECT;
import static com.grupozap.search.api.model.mapping.MappingType.FIELD_TYPE_SCRIPT;
import static com.grupozap.search.api.utils.FlattenMapUtils.flat;
import static com.grupozap.search.api.utils.MapperUtils.parser;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.springframework.http.HttpMethod.GET;

import com.grupozap.search.api.exception.IndexNotFoundException;
import com.grupozap.search.api.exception.PropertyNotFoundException;
import com.grupozap.search.api.model.event.ClusterSettingsUpdatedEvent;
import com.grupozap.search.api.model.mapping.MappingType;
import com.grupozap.search.api.model.search.Indexable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("elasticsearchSettings")
@DependsOn("fieldCache")
public class ElasticsearchSettingsAdapter
    implements SettingsAdapter<Map<String, Map<String, Object>>, String> {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchSettingsAdapter.class);
  private static final String CLUSTER_STATE_PATH = "/_cluster/state?pretty";

  public static final String SHARDS = "number_of_shards";
  public static final String REPLICAS = "number_of_replicas";

  private final ApplicationEventPublisher applicationEventPublisher;
  private final RestClient restClient;
  private final Request request;

  private Map<String, Map<String, Object>> structuredIndices;

  @Autowired
  public ElasticsearchSettingsAdapter(
      ApplicationEventPublisher applicationEventPublisher, RestClient restClient) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.structuredIndices = new HashMap<>();
    this.restClient = restClient;
    this.request = new Request(GET.name(), CLUSTER_STATE_PATH);

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
  public String getFieldType(final String index, final String fieldName) {
    return valueOf(structuredIndices.get(index).get(fieldName));
  }

  @Override
  public boolean isTypeOf(final String index, final String fieldName, final MappingType type) {
    return type.typeOf(getFieldType(index, fieldName));
  }

  @SuppressWarnings("unchecked")
  @Scheduled(fixedRateString = "${es.settings.refresh.rate.ms}")
  private void loadSettingsInformationFromCluster() {
    try {
      final Map<String, Object> clusterState =
          parser(EntityUtils.toString(restClient.performRequest(this.request).getEntity()));

      Map<String, Map<String, Object>> structuredIndicesAux = new HashMap<>();
      final Map<String, Object> metadata = (Map) clusterState.get("metadata");

      /* Starting getting indices information */
      this.addIndicesInformation(metadata, structuredIndicesAux);

      /* Starting getting Stored-Scripts information */
      this.addStoredScripts(metadata, structuredIndicesAux);

      if (!structuredIndicesAux.isEmpty()) {
        structuredIndices = structuredIndicesAux;
      }

      LOG.debug("Refresh getting information from cluster settings executed with success");
      applicationEventPublisher.publishEvent(
          new ClusterSettingsUpdatedEvent(this, structuredIndices));

    } catch (IOException e) {
      LOG.error("Error on get stored_scripts", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void addIndicesInformation(
      Map<String, Object> metadata, Map<String, Map<String, Object>> structuredIndicesAux) {

    if (metadata.containsKey("indices")) {
      Map<String, Object> indices = (Map<String, Object>) metadata.get("indices");
      indices
          .keySet()
          .stream()
          .filter(index -> !startsWith(index, "."))
          .forEach(
              index -> {
                Map<String, Object> indexInfo = new ConcurrentSkipListMap<>();
                newArrayList(SHARDS, REPLICAS)
                    .forEach(
                        setting -> {
                          String value =
                              valueOf(
                                  ((Map)
                                          ((Map) ((Map) indices.get(index)).get("settings"))
                                              .get("index"))
                                      .get(setting));
                          if (isNotEmpty(value)) indexInfo.put(setting, value);
                        });

                Map<String, Object> mappings = (Map) ((Map) indices.get(index)).get("mappings");

                mappings
                    .keySet()
                    .iterator()
                    .forEachRemaining(obj -> getMappingFromType(indexInfo, mappings, index));

                structuredIndicesAux.put(index, indexInfo);
              });
    }
  }

  @SuppressWarnings("unchecked")
  private void addStoredScripts(
      Map<String, Object> metadata, Map<String, Map<String, Object>> structuredIndicesAux) {
    if (metadata.containsKey("stored_scripts")) {
      ((Map<String, Object>) metadata.get("stored_scripts"))
          .keySet()
          .forEach(
              script -> {
                final String index = script.split("_")[0];
                if (structuredIndicesAux.containsKey(index)) {
                  structuredIndicesAux.get(index).put(script, FIELD_TYPE_SCRIPT.getDefaultType());
                } else {
                  final Map<String, Object> value = new HashMap<>();
                  value.put(script, FIELD_TYPE_SCRIPT.getDefaultType());
                  structuredIndicesAux.put(index, value);
                }
              });
    }
  }

  private void getMappingFromType(
      Map<String, Object> indexInfo, Map<String, Object> mappings, String index) {

    try {
      indexInfo.putAll(
          flat(mappings, newArrayList("mappings", "properties", "type", "fields", index)));
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
          .forEach(
              params ->
                  params.forEach(
                      param -> indexInfo.putIfAbsent(param, FIELD_TYPE_OBJECT.getDefaultType())));
    } catch (Exception e) {
      LOG.error("Error on get mapping from index {} and type {}", index, e);
    }
  }
}
