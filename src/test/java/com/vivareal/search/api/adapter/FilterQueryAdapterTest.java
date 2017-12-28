package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.mapping.MappingType;
import com.vivareal.search.api.model.parser.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilterQueryAdapterTest {

    private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;
    private FilterQueryAdapter filterQueryAdapter;
    private QueryParser queryParser;

    public FilterQueryAdapterTest() {
        OperatorParser operatorParser = new OperatorParser();
        NotParser notParser = new NotParser();
        FieldParser fieldParser = new FieldParser(notParser);
        FilterParser filterParser = new FilterParser(fieldParser, operatorParser, new ValueParser());

        this.queryParser = new QueryParser(operatorParser, filterParser, notParser);
        this.settingsAdapter = mock(SettingsAdapter.class);
        this.filterQueryAdapter = new FilterQueryAdapter(settingsAdapter, queryParser);
    }

    private BoolQueryBuilder getQueryBuilder(boolean isNested, boolean ignoreNestedQueryBuilder) {
        final String field = "nested.field";
        final Object value = "Lorem Ipsum";

        when(settingsAdapter.isTypeOf(INDEX_NAME, field.split("\\.")[0], MappingType.FIELD_TYPE_NESTED)).thenReturn(isNested);

        BoolQueryBuilder boolQueryBuilder = boolQuery();

        filterQueryAdapter.apply(boolQueryBuilder, queryParser.parse(format("%s=\"%s\"", field, value)), INDEX_NAME, new HashMap<>(), ignoreNestedQueryBuilder);

        return boolQueryBuilder;
    }

    @Test
    public void shouldApplyNestedQueryBuilderWhenNecessary() {
        BoolQueryBuilder boolQueryBuilder = getQueryBuilder(true, false);
        assertEquals(NestedQueryBuilder.class, boolQueryBuilder.filter().get(0).getClass());
    }

    @Test
    public void shouldIgnoreNestedQueryBuilderForNestedField() {
        BoolQueryBuilder boolQueryBuilder = getQueryBuilder(true, true);
        assertNotEquals(NestedQueryBuilder.class, boolQueryBuilder.filter().get(0).getClass());
    }

    @Test
    public void shouldNotApplyNestedQueryBuilderForNonNestedField() {
        BoolQueryBuilder boolQueryBuilder = getQueryBuilder(false, false);
        assertNotEquals(NestedQueryBuilder.class, boolQueryBuilder.filter().get(0).getClass());
    }

    @Test
    public void shouldIgnoreNestedQueryBuilderForNotNestedField() {
        BoolQueryBuilder boolQueryBuilder = getQueryBuilder(false, true);
        assertNotEquals(NestedQueryBuilder.class, boolQueryBuilder.filter().get(0).getClass());
    }
}
