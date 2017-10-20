package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.http.SearchApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SORT;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SortQueryAdapterTest extends SearchTransportClientMock {

    private static SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter = mock(SettingsAdapter.class);

    private SortQueryAdapter sortQueryAdapter = new SortQueryAdapter(settingsAdapter);

    @BeforeClass
    public static void setup() {
        ES_DEFAULT_SORT.setValue(INDEX_NAME, "id ASC");
    }

    @Test
    public void shouldApplySortByDefaultProperty() {
        SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
        SearchApiRequest request = fullRequest.build();

        when(settingsAdapter.isTypeOf(request.getIndex(), "id", FIELD_TYPE_NESTED)).thenReturn(false);

        sortQueryAdapter.apply(requestBuilder, request);
        List<FieldSortBuilder> sorts = (List) requestBuilder.request().source().sorts();

        assertEquals("id", sorts.get(0).getFieldName());
        assertEquals("ASC", sorts.get(0).order().name());
        assertNull(sorts.get(0).getNestedPath());

        assertEquals("_uid", sorts.get(1).getFieldName());
        assertEquals("DESC", sorts.get(1).order().name());
        assertNull(sorts.get(1).getNestedPath());
    }

    @Test
    public void shouldApplySortByRequest() {
        String fieldName1 = "field";
        SortOrder sortOrder1 = ASC;

        String fieldName2 = "nested.field";
        SortOrder sortOrder2 = DESC;

        SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
        SearchApiRequest request = fullRequest.build();
        request.setSort(fieldName1 + " " + sortOrder1.name() + ", " + fieldName2 + " " + sortOrder2.name());

        when(settingsAdapter.isTypeOf(request.getIndex(), fieldName1, FIELD_TYPE_NESTED)).thenReturn(false);
        when(settingsAdapter.isTypeOf(request.getIndex(), fieldName2.split("\\.")[0], FIELD_TYPE_NESTED)).thenReturn(true);

        sortQueryAdapter.apply(requestBuilder, request);
        List<FieldSortBuilder> sorts = (List) requestBuilder.request().source().sorts();

        assertEquals(fieldName1, sorts.get(0).getFieldName());
        assertEquals(sortOrder1, sorts.get(0).order());
        assertNull(sorts.get(0).getNestedPath());

        assertEquals(fieldName2, sorts.get(1).getFieldName());
        assertEquals(sortOrder2, sorts.get(1).order());
        assertEquals("nested", sorts.get(1).getNestedPath());

        assertEquals("_uid", sorts.get(2).getFieldName());
        assertEquals("DESC", sorts.get(2).order().name());
        assertNull(sorts.get(2).getNestedPath());
    }

    @Test
    public void shouldNotApplySortWhenClientInputSortEmptyOnRequest() {
        SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
        SearchApiRequest request = fullRequest.build();
        request.setSort("");

        sortQueryAdapter.apply(requestBuilder, request);
        assertNull(requestBuilder.request().source());
    }

}
