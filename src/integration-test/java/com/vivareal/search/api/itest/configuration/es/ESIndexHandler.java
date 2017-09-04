package com.vivareal.search.api.itest.configuration.es;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Service
public class ESIndexHandler {

    private static Logger LOG = LoggerFactory.getLogger(ESIndexHandler.class);

    public static final String TEST_DATA_INDEX = "/testdata";
    public static final String TEST_DATA_TYPE = "testdata";

    public static final String SEARCH_API_PROPERTIES_INDEX = "/search-api-properties";
    public static final String SEARCH_API_PROPERTIES_TYPE = "properties";

    private final RestClient restClient;
    private int standardDatasetSize;
    private final Long timeout;

    private Map<String, Object> properties;

    @Autowired
    public ESIndexHandler(RestClient restClient,
                          @Value("${itest.standard.dataset.size}") Integer standardDatasetSize,
                          @Value("${es.controller.search.timeout}") Long timeout) {
        this.restClient = restClient;
        this.standardDatasetSize = standardDatasetSize;
        this.timeout = timeout;
        this.properties = new LinkedHashMap<>();
        this.setDefaultProperties();
    }

    public void truncateIndexData(String index) throws IOException {
        Response response = restClient.performRequest("POST", index + "/_delete_by_query?refresh=true", emptyMap(),
        new NStringEntity(
        "{\n" +
        "  \"query\": {\n" +
        "    \"match_all\": {}\n" +
        "  }\n" +
        "}", APPLICATION_JSON));

        LOG.info(index + " index cleared, deleted " + response + " documents");
    }

    public void setDefaultProperties() {
        putStandardProperty("es.default.size", 20);
    }

    public void addStandardProperties() {
        insertEntityByIndex(SEARCH_API_PROPERTIES_INDEX, SEARCH_API_PROPERTIES_TYPE, TEST_DATA_TYPE, writeValueAsStringFromMap(TEST_DATA_TYPE, properties));
        refreshIndex(SEARCH_API_PROPERTIES_INDEX);
    }

    public void putStandardProperty(final String key, final Object value) {
        this.properties.put(key, value);
    }

    public void addStandardTestData() {
        List<String> entities = new ArrayList<>(standardDatasetSize);
        for(int id = 1; id<= standardDatasetSize; id++) {
            String entity = createStandardEntityForId(id);
            if(insertEntityByIndex(TEST_DATA_INDEX, TEST_DATA_TYPE, valueOf(id), entity))
                entities.add(entity);
        }

        LOG.info(TEST_DATA_INDEX + " inserted " + entities.size() + " documents");
        refreshIndex(TEST_DATA_INDEX);
    }

    private String createStandardEntityForId(int id) {
        boolean isEven = id % 2 == 0;

        Map<String, Object> kv = unmodifiableMap(Stream.of(
            new SimpleEntry<>("field", "common"),
            new SimpleEntry<>("array_string", rangeClosed(1, id).boxed().map(String::valueOf).collect(toList()))
        ).collect(toMap(SimpleEntry::getKey, e -> (Object) e.getValue())));

        Map<String, Double> geo = unmodifiableMap(Stream.of(
            new SimpleEntry<>("lat", id * -1d),
            new SimpleEntry<>("lon", id * 1d)
        ).collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

        Map<String, Object> nestedObject = unmodifiableMap(Stream.of(
            new SimpleEntry<>("object", kv),
            new SimpleEntry<>("number", id * 2),
            new SimpleEntry<>("float", id * 3.5f),
            new SimpleEntry<>("string", format("string with char %s", (char) (id + 'a' - 1))),
            new SimpleEntry<>("special_string", format("string with special chars * and + and %n and ? and %% and 5%% and _ and with_underscore of %s to search by like", (char) (id + 'a' - 1))),
            new SimpleEntry<>("boolean", !isEven),
            new SimpleEntry<>(isEven ? "even" : "odd", true)
        ).collect(toMap(SimpleEntry::getKey, e -> (Object) e.getValue())));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", valueOf(id));
        data.put("numeric", id);
        data.put("field" + id, "value" + id);
        data.put("isEven", isEven);
        data.put("array_integer", rangeClosed(1, id).boxed().collect(toList()));
        data.put("nested", nestedObject);
        data.put("object", nestedObject);
        data.put("geo", geo);

        return writeValueAsStringFromMap(id, data);
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
            Response response = restClient.performRequest("POST", index + "/" + type + "/" + id + "?refresh=true", emptyMap(), new NStringEntity(body, APPLICATION_JSON));
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
