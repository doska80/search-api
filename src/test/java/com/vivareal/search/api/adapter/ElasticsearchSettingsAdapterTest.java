package com.vivareal.search.api.adapter;

import com.vivareal.search.api.exception.IndexNotFoundException;
import com.vivareal.search.api.exception.InvalidFieldException;
import com.vivareal.search.api.exception.PropertyNotFoundException;
import com.vivareal.search.api.model.http.BaseApiRequest;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.REPLICAS;
import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.vivareal.search.api.model.mapping.MappingType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ElasticsearchSettingsAdapterTest extends SearchTransportClientMock {

    private ElasticsearchSettingsAdapter settingsAdapter;
    private Map<String, Map<String, Object>> structuredIndices;
    private BaseApiRequest validIndexRequest = basicRequest.build();
    private BaseApiRequest invalidIndexRequest = basicRequest.index("not-valid-index").build();

    @Before
    public void setup() {
        this.settingsAdapter = spy(new ElasticsearchSettingsAdapter());
        this.structuredIndices = spy(structuredIndicesSettings());

        setField(this.settingsAdapter, "transportClient", transportClient);
        setField(this.settingsAdapter, "structuredIndices", structuredIndices);
    }

    @Test
    public void shouldGetConfigurationByExistKey() {
        String shards = settingsAdapter.settingsByKey(INDEX_NAME, SHARDS);
        assertEquals("8" , shards);

        String replicas = settingsAdapter.settingsByKey(INDEX_NAME, REPLICAS);
        assertEquals("2" , replicas);
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

    @Test(expected = IndexNotFoundException.class)
    public void checkInvalidIndex() {
        settingsAdapter.checkIndex(invalidIndexRequest);
    }

    @Test
    public void checkFieldName() {
        assertTrue(settingsAdapter.checkFieldName(validIndexRequest.getIndex(), VALID_FIELD_TEXT, false));
        assertTrue(settingsAdapter.checkFieldName(validIndexRequest.getIndex(), "*", true));
    }

    @Test(expected = InvalidFieldException.class)
    public void checkInvalidFieldName() {
        settingsAdapter.checkFieldName(validIndexRequest.getIndex(), "invalid.field.text", false);
    }

    @Test(expected = InvalidFieldException.class)
    public void checkFieldNameWithAsteriskWhenAcceptAsteriskIsFalse() {
        settingsAdapter.checkFieldName(validIndexRequest.getIndex(), "*", false);
    }

    @Test
    public void getFieldType() {
        assertEquals("text", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_TEXT));
        verify(settingsAdapter, times(1)).checkFieldName(validIndexRequest.getIndex(), VALID_FIELD_TEXT, false);

        assertEquals("boolean", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_BOOLEAN));
        verify(settingsAdapter, times(1)).checkFieldName(validIndexRequest.getIndex(), VALID_FIELD_BOOLEAN, false);

        assertEquals("nested", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_NESTED));
        verify(settingsAdapter, times(1)).checkFieldName(validIndexRequest.getIndex(), VALID_FIELD_NESTED, false);

        assertEquals("geo_point", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_GEO_POINT));
        verify(settingsAdapter, times(1)).checkFieldName(validIndexRequest.getIndex(), VALID_FIELD_GEO_POINT, false);

        assertEquals("keyword", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_KEYWORD));
        verify(settingsAdapter, times(1)).checkFieldName(validIndexRequest.getIndex(), VALID_FIELD_KEYWORD, false);

        assertEquals("long", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_LONG));
        verify(settingsAdapter, times(1)).checkFieldName(validIndexRequest.getIndex(), VALID_FIELD_LONG, false);

        assertEquals("float", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_FLOAT));
        verify(settingsAdapter, times(1)).checkFieldName(validIndexRequest.getIndex(), VALID_FIELD_FLOAT, false);

        assertEquals("date", settingsAdapter.getFieldType(validIndexRequest.getIndex(), VALID_FIELD_DATE));
        verify(settingsAdapter, times(1)).checkFieldName(validIndexRequest.getIndex(), VALID_FIELD_DATE, false);
    }

    @Test
    public void isTypeOf() {
        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_TEXT, FIELD_TYPE_TEXT));
        verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_TEXT);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_TEXT, FIELD_TYPE_STRING));
        verify(settingsAdapter, times(2)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_TEXT);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_KEYWORD, FIELD_TYPE_KEYWORD));
        verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_KEYWORD);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_KEYWORD, FIELD_TYPE_STRING));
        verify(settingsAdapter, times(2)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_KEYWORD);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_BOOLEAN, FIELD_TYPE_BOOLEAN));
        verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_BOOLEAN);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_NESTED, FIELD_TYPE_NESTED));
        verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_NESTED);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_GEO_POINT, FIELD_TYPE_GEOPOINT));
        verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_GEO_POINT);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_LONG, FIELD_TYPE_LONG));
        verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_LONG);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_LONG, FIELD_TYPE_NUMBER));
        verify(settingsAdapter, times(2)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_LONG);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_FLOAT, FIELD_TYPE_FLOAT));
        verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_FLOAT);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_FLOAT, FIELD_TYPE_NUMBER));
        verify(settingsAdapter, times(2)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_FLOAT);

        assertTrue(settingsAdapter.isTypeOf(validIndexRequest.getIndex(), VALID_FIELD_DATE, FIELD_TYPE_DATE));
        verify(settingsAdapter, times(1)).getFieldType(validIndexRequest.getIndex(), VALID_FIELD_DATE);
    }

    private static final String VALID_FIELD_TEXT = "valid.field.text";
    private static final String VALID_FIELD_BOOLEAN = "valid.field.boolean";
    private static final String VALID_FIELD_LONG = "valid.field.long";
    private static final String VALID_FIELD_FLOAT = "valid.field.float";
    private static final String VALID_FIELD_NESTED = "valid.field.nested";
    private static final String VALID_FIELD_GEO_POINT = "valid.field.geo_point";
    private static final String VALID_FIELD_KEYWORD = "valid.field.keyword";
    private static final String VALID_FIELD_DATE = "valid.field.date";

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
        return structuredIndices;
    }

}
