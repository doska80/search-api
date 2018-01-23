package com.vivareal.search.api.itest.configuration.es;

import static com.vivareal.search.api.itest.configuration.data.TestData.createTestData;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ESIndexHandler {

  public static final String TEST_DATA_INDEX = "/testdata";
  public static final String TEST_DATA_TYPE = "testdata";
  public static final String SEARCH_API_PROPERTIES_INDEX = "/search-api-properties";
  public static final String SEARCH_API_PROPERTIES_TYPE = "properties";
  private static final Logger LOG = LoggerFactory.getLogger(ESIndexHandler.class);
  private final RestClient restClient;

  private final int standardDatasetSize;
  private final int standardDatasetFacetDecrease;
  private final Long timeout;

  private final Map<String, Object> properties;

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
    this.properties = new LinkedHashMap<>();
    this.setDefaultProperties();
  }

  public void truncateIndexData(String index) throws IOException {
    Response response =
        restClient.performRequest(
            "POST",
            index + "/_delete_by_query?refresh=true",
            emptyMap(),
            new NStringEntity(
                "{\n" + "  \"query\": {\n" + "    \"match_all\": {}\n" + "  }\n" + "}",
                APPLICATION_JSON));

    LOG.info(index + " index cleared, deleted " + response + " documents");
  }

  public void setDefaultProperties() {
    putStandardProperty("es.default.size", size);
    putStandardProperty("es.query.timeout.unit", queryTimeoutUnit);
    putStandardProperty("es.query.timeout.value", queryTimeoutValue);
  }

  public void addStandardProperties() {
    insertEntityByIndex(
        SEARCH_API_PROPERTIES_INDEX,
        SEARCH_API_PROPERTIES_TYPE,
        TEST_DATA_TYPE,
        writeValueAsStringFromMap(TEST_DATA_TYPE, properties));
    refreshIndex(SEARCH_API_PROPERTIES_INDEX);
  }

  public void putStandardProperty(final String key, final Object value) {
    this.properties.put(key, value);
  }

  public void addStandardTestData() {
    List<String> entities = new ArrayList<>(standardDatasetSize);
    for (int id = 1; id <= standardDatasetSize; id++) {
      String entity =
          createStandardEntityForId(
              id, (id <= (standardDatasetSize - standardDatasetFacetDecrease) ? 1 : 2));
      if (insertEntityByIndex(TEST_DATA_INDEX, TEST_DATA_TYPE, valueOf(id), entity))
        entities.add(entity);
    }

    LOG.info(TEST_DATA_INDEX + " inserted " + entities.size() + " documents");
    refreshIndex(TEST_DATA_INDEX);
  }

  public String createStandardEntityForId(int id, int facetValue) {
    return writeValueAsStringFromMap(id, createTestData(id, facetValue));
  }

  private String writeValueAsStringFromMap(Object id, Map<String, Object> data) {
    try {
      return new ObjectMapper().writeValueAsString(data);
    } catch (JsonProcessingException e) {
      LOG.error("Unable to create standard entity for id " + id, e);
      return "{}";
    }
  }

  private boolean insertEntityByIndex(String index, String type, String id, String body) {
    try {
      Response response =
          restClient.performRequest(
              "POST",
              index + "/" + type + "/" + id + "?refresh=true",
              emptyMap(),
              new NStringEntity(body, APPLICATION_JSON));
      LOG.info(String.format("%s adding document %s documents", index, response));
      return true;
    } catch (IOException e) {
      LOG.error("Unable to add entity for index: " + index, e);
    }
    return false;
  }

  private void refreshIndex(String index) {
    try {
      restClient.performRequest("POST", index + "/_refresh", emptyMap());
      MICROSECONDS.sleep(timeout);
      LOG.info("Forced commit into index: " + index);
    } catch (IOException | InterruptedException e) {
      LOG.error("Unable to force commit into index: " + index, e);
    }
  }
}
