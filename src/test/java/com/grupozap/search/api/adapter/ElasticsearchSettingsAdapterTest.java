package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.adapter.ElasticsearchSettingsAdapter.REPLICAS;
import static com.grupozap.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_ALIAS_NAME;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.model.mapping.MappingType.*;
import static com.grupozap.search.api.utils.ReadFileUtils.readFileFromResources;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.grupozap.search.api.exception.IndexNotFoundException;
import com.grupozap.search.api.exception.PropertyNotFoundException;
import com.grupozap.search.api.model.http.BaseApiRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

public class ElasticsearchSettingsAdapterTest extends SearchTransportClientMock {

  private static final String VALID_FIELD_TEXT = "valid.field.text";
  private static final String VALID_FIELD_BOOLEAN = "valid.field.boolean";
  private static final String VALID_FIELD_LONG = "valid.field.long";
  private static final String VALID_FIELD_FLOAT = "valid.field.float";
  private static final String VALID_FIELD_NESTED = "valid.field.nested";
  private static final String VALID_FIELD_GEO_POINT = "valid.field.geo_point";
  private static final String VALID_FIELD_KEYWORD = "valid.field.keyword";
  private static final String VALID_FIELD_DATE = "valid.field.date";
  private final BaseApiRequest validIndexRequest = basicRequest.build();
  private final BaseApiRequest validIndexAliasRequest = basicRequestWithIndexAlias.build();
  private final BaseApiRequest invalidIndexRequest = basicRequest.index("not-valid-index").build();

  private ElasticsearchSettingsAdapter settingsAdapter;

  private Map<String, Map<String, Object>> structuredIndices;

  @Before
  public void setup() throws IOException {
    var applicationEventPublisher = mock(ApplicationEventPublisher.class);
    doNothing().when(applicationEventPublisher).publishEvent(any());

    var restClient = mock(RestClient.class);

    var response = mock(Response.class);
    when(restClient.performRequest(any(Request.class))).thenReturn(response);

    final HttpEntity entity = new StringEntity(readFileFromResources("cluster_state.json"));
    when(response.getEntity()).thenReturn(entity);

    this.settingsAdapter =
        spy(new ElasticsearchSettingsAdapter(applicationEventPublisher, restClient));
    this.structuredIndices = spy(structuredIndicesSettings());

    setField(this.settingsAdapter, "structuredIndices", structuredIndices);
  }

  @Test
  public void shouldGetConfigurationByExistKey() {
    var shards = settingsAdapter.settingsByKey(INDEX_NAME, SHARDS);
    assertEquals("8", shards);

    var replicas = settingsAdapter.settingsByKey(INDEX_NAME, REPLICAS);
    assertEquals("2", replicas);
  }

  @Test(expected = PropertyNotFoundException.class)
  public void shouldThrowExceptionWhenGetConfigurationByNonExistKey() {
    settingsAdapter.settingsByKey(INDEX_NAME, "property.not.found");
  }

  @Test
  public void checkValidIndex() {
    settingsAdapter.checkIndex(validIndexRequest);
    verify(structuredIndices, times(1)).containsKey(validIndexRequest.getIndex());
  }

  @Test
  public void checkValidIndexByAlias() {
    settingsAdapter.checkIndex(validIndexAliasRequest);
    verify(structuredIndices, times(1)).containsKey(validIndexAliasRequest.getIndex());
  }

  @Test(expected = IndexNotFoundException.class)
  public void checkInvalidIndex() {
    settingsAdapter.checkIndex(invalidIndexRequest);
  }

  @Test
  public void getFieldType() {
    assertEquals(
        "text", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_TEXT));
    assertEquals(
        "boolean", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_BOOLEAN));
    assertEquals(
        "nested", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_NESTED));
    assertEquals(
        "geo_point",
        settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_GEO_POINT));
    assertEquals(
        "keyword", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_KEYWORD));
    assertEquals(
        "long", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_LONG));
    assertEquals(
        "float", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_FLOAT));
    assertEquals(
        "date", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_DATE));
  }

  @Test
  public void isTypeOf() {
    assertTrue(
        settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_TEXT, FIELD_TYPE_TEXT));
    verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_TEXT);

    assertTrue(
        settingsAdapter.isTypeOf(
            validIndexRequest.getIndex(), VALID_FIELD_TEXT, FIELD_TYPE_STRING));
    verify(settingsAdapter, times(2)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_TEXT);

    assertTrue(
        settingsAdapter.isTypeOf(
            validIndexRequest.getIndex(), VALID_FIELD_KEYWORD, FIELD_TYPE_KEYWORD));
    verify(settingsAdapter, times(1))
        .getFieldType(validIndexRequest.getIndex(), VALID_FIELD_KEYWORD);

    assertTrue(
        settingsAdapter.isTypeOf(
            validIndexRequest.getIndex(), VALID_FIELD_KEYWORD, FIELD_TYPE_STRING));
    verify(settingsAdapter, times(2))
        .getFieldType(validIndexRequest.getIndex(), VALID_FIELD_KEYWORD);

    assertTrue(
        settingsAdapter.isTypeOf(
            validIndexRequest.getIndex(), VALID_FIELD_BOOLEAN, FIELD_TYPE_BOOLEAN));
    verify(settingsAdapter, times(1))
        .getFieldType(validIndexRequest.getIndex(), VALID_FIELD_BOOLEAN);

    assertTrue(
        settingsAdapter.isTypeOf(
            validIndexRequest.getIndex(), VALID_FIELD_NESTED, FIELD_TYPE_NESTED));
    verify(settingsAdapter, times(1))
        .getFieldType(validIndexRequest.getIndex(), VALID_FIELD_NESTED);

    assertTrue(
        settingsAdapter.isTypeOf(
            validIndexRequest.getIndex(), VALID_FIELD_GEO_POINT, FIELD_TYPE_GEOPOINT));
    verify(settingsAdapter, times(1))
        .getFieldType(validIndexRequest.getIndex(), VALID_FIELD_GEO_POINT);

    assertTrue(
        settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_LONG, FIELD_TYPE_LONG));
    verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_LONG);

    assertTrue(
        settingsAdapter.isTypeOf(
            validIndexRequest.getIndex(), VALID_FIELD_LONG, FIELD_TYPE_NUMBER));
    verify(settingsAdapter, times(2)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_LONG);

    assertTrue(
        settingsAdapter.isTypeOf(
            validIndexRequest.getIndex(), VALID_FIELD_FLOAT, FIELD_TYPE_FLOAT));
    verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_FLOAT);

    assertTrue(
        settingsAdapter.isTypeOf(
            validIndexRequest.getIndex(), VALID_FIELD_FLOAT, FIELD_TYPE_NUMBER));
    verify(settingsAdapter, times(2)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_FLOAT);

    assertTrue(
        settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_DATE, FIELD_TYPE_DATE));
    verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_DATE);
  }

  private Map<String, Map<String, Object>> structuredIndicesSettings() {
    Map<String, Map<String, Object>> structuredIndices = new HashMap<>();
    Map<String, Object> indexSettings = new HashMap<>();
    indexSettings.put(SHARDS, 8);
    indexSettings.put(REPLICAS, 2);
    indexSettings.put(VALID_FIELD_TEXT, "text");
    indexSettings.put(VALID_FIELD_BOOLEAN, "boolean");
    indexSettings.put(VALID_FIELD_LONG, "long");
    indexSettings.put(VALID_FIELD_FLOAT, "float");
    indexSettings.put(VALID_FIELD_NESTED, "nested");
    indexSettings.put(VALID_FIELD_GEO_POINT, "geo_point");
    indexSettings.put(VALID_FIELD_KEYWORD, "keyword");
    indexSettings.put(VALID_FIELD_DATE, "date");

    structuredIndices.put(INDEX_NAME, indexSettings);
    structuredIndices.put(INDEX_ALIAS_NAME, indexSettings);

    return structuredIndices;
  }
}
