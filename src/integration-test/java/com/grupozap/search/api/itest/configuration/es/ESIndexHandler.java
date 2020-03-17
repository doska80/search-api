package com.grupozap.search.api.itest.configuration.es;

import static com.google.common.collect.Lists.newArrayList;
import static com.grupozap.search.api.itest.configuration.data.TestData.createTestData;
import static java.lang.String.valueOf;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ESIndexHandler {

  public static final String TEST_DATA_INDEX = "/testdata";
  public static final String TEST_DATA_INDEX_ALIAS = "/testdata-alias";
  public static final String TEST_DATA_TYPE = "testdata";
  public static final String TEST_DATA_TYPE_ALIAS = "testdata-alias";
  public static final String SEARCH_API_PROPERTIES_INDEX = "/search-api-properties";
  public static final String DEFAULT_TYPE = "_doc";
  private static final Logger LOG = LoggerFactory.getLogger(ESIndexHandler.class);
  private final RestClient restClient;

  private final int standardDatasetSize;
  private final int standardDatasetFacetDecrease;
  private final Long timeout;

  private final Map<String, Object> testdataProperties;
  private final Map<String, Object> testdataAliasProperties;

  @Value("${es.query.timeout.unit}")
  private String queryTimeoutUnit;

  @Value("${es.query.timeout.value}")
  private Integer queryTimeoutValue;

  @Value("${es.default.size}")
  private Integer size;

  @Autowired
  public ESIndexHandler(
      RestClient restClient,
      @Value("${itest.standard.dataset.size}") Integer standardDatasetSize,
      @Value("${itest.standard.dataset.facet.decrease}") Integer standardDatasetFacetDecrease,
      @Value("${es.controller.search.timeout}") Long timeout) {
    this.restClient = restClient;
    this.standardDatasetSize = standardDatasetSize;
    this.standardDatasetFacetDecrease = standardDatasetFacetDecrease;
    this.timeout = timeout;
    this.testdataProperties = new LinkedHashMap<>();
    this.testdataAliasProperties = new LinkedHashMap<>();
    this.setDefaultProperties();
  }

  public void truncateIndexData(String index) throws IOException {

    final var request = new Request("POST", index + "/_delete_by_query?refresh=true");
    request.setEntity(
        new NStringEntity(
            "{\n" + "  \"query\": {\n" + "    \"match_all\": {}\n" + "  }\n" + "}",
            APPLICATION_JSON));

    var response = restClient.performRequest(request);

    LOG.info(index + " index cleared, deleted " + response + " documents");
  }

  @SuppressWarnings("unchecked")
  public void setDefaultProperties() {
    putStandardProperty("filter.default.clauses", List.of());
    putStandardProperty("es.default.size", size);
    putStandardProperty("es.query.timeout.unit", queryTimeoutUnit);
    putStandardProperty("es.query.timeout.value", queryTimeoutValue);

    var esSortMap = new HashMap<String, Object>();
    var sortList = new ArrayList<>();
    var sortsMap = new LinkedHashMap<String, LinkedHashMap<String, Object>>();

    esSortMap.put("disabled", false);
    esSortMap.put("default_sort", "numeric ASC");
    sortList.add(sortsMap);
    esSortMap.put("sorts", sortList);
    putStandardProperty("es.sort", esSortMap);

    Map<String, Object> searchAliases = new HashMap<>();
    Map<String, Object> fieldAliases =
        Map.of(
            "field_before_alias", "field_after_alias",
            "field_geo_before_alias", "field_geo_after_alias");
    searchAliases.put("fields", fieldAliases);
    putStandardProperty("es.alias", searchAliases);

    testdataAliasProperties.putAll(testdataProperties);
    testdataAliasProperties.put("filter.default.clauses", newArrayList("numeric<=10"));
  }

  public void addStandardProperties() {
    // add default properties to index testdata
    insertEntityByIndex(
        SEARCH_API_PROPERTIES_INDEX,
        TEST_DATA_TYPE,
        writeValueAsStringFromMap(TEST_DATA_TYPE, testdataProperties));

    // add default properties to index testdata-alias
    insertEntityByIndex(
        SEARCH_API_PROPERTIES_INDEX,
        TEST_DATA_TYPE_ALIAS,
        writeValueAsStringFromMap(TEST_DATA_TYPE_ALIAS, testdataAliasProperties));

    refreshIndex(SEARCH_API_PROPERTIES_INDEX);

    try {
      // waiting for complete ES refresh
      sleep(500);
    } catch (InterruptedException e) {
      LOG.error("Error on sleeping time to refresh the ES index", e);
    }
  }

  public void putStandardProperty(final String key, final Object value) {
    this.testdataProperties.put(key, value);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void putStandardPropertyInArray(
      final String mapName, final String arrayName, final Object value) {
    var map = (Map) this.testdataProperties.get(mapName);
    var list = (ArrayList) map.get(arrayName);
    list.add(value);
    this.testdataProperties.replace(mapName, map);
  }

  public void addStandardTestData() {
    List<String> entities = new ArrayList<>(standardDatasetSize);
    for (var id = 1; id <= standardDatasetSize; id++) {
      var entity =
          createStandardEntityForId(
              id, (id <= (standardDatasetSize - standardDatasetFacetDecrease) ? 1 : 2));
      if (insertEntityByIndex(TEST_DATA_INDEX, valueOf(id), entity)) {
        entities.add(entity);
      }
    }

    LOG.info(TEST_DATA_INDEX + " inserted " + entities.size() + " documents");
    refreshIndex(TEST_DATA_INDEX);
  }

  public String createStandardEntityForId(int id, int facetValue) {
    return writeValueAsStringFromMap(id, createTestData(id, facetValue));
  }

  public String writeValueAsStringFromMap(Object id, Map<String, Object> data) {
    try {
      return new ObjectMapper().writeValueAsString(data);
    } catch (JsonProcessingException e) {
      LOG.error("Unable to create standard entity for id " + id, e);
      return "{}";
    }
  }

  public boolean insertEntityByIndex(String index, String id, String body) {
    try {
      final var request =
          new Request("POST", index + "/" + DEFAULT_TYPE + "/" + id + "?refresh=true");
      request.setEntity(new NStringEntity(body, APPLICATION_JSON));
      var response = restClient.performRequest(request);

      LOG.info(String.format("%s adding document %s documents", index, response));
      return true;
    } catch (IOException e) {
      LOG.error("Unable to add entity for index: " + index, e);
    }
    return false;
  }

  public void refreshIndex(String index) {
    try {
      restClient.performRequest(new Request("POST", index + "/_refresh"));
      MICROSECONDS.sleep(timeout);
      LOG.info("Forced commit into index: " + index);
    } catch (IOException | InterruptedException e) {
      LOG.error("Unable to force commit into index: " + index, e);
    }
  }

  public void deleteIndex(String index) {
    try {
      var response = restClient.performRequest(new Request("DELETE", index));
      if (SC_OK == response.getStatusLine().getStatusCode()) {
        LOG.info("Index {} deleted with success", index);
      } else {
        throw new RuntimeException(
            "Error deleting index "
                + index
                + " - Status-code: "
                + response.getStatusLine().getStatusCode());
      }
    } catch (IOException e) {
      LOG.error("Error deleting index {}", index, e);
    }
  }
}
