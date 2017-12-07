package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.parser.SortParser;
import com.vivareal.search.api.model.query.*;
import com.vivareal.search.api.model.query.Sort.Item;
import com.vivareal.search.api.model.search.Sortable;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SORT;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.elasticsearch.search.sort.SortOrder.valueOf;

@Component
public class SortQueryAdapter {

    private static final FieldSortBuilder DEFAULT_TIEBREAKER = fieldSort("_uid").order(DESC);

    private final SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;
    private SortParser sortParser;

    public SortQueryAdapter(@Qualifier("elasticsearchSettings") SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter,
                            SortParser sortParser) {
        this.settingsAdapter = settingsAdapter;
        this.sortParser = sortParser;
    }

    public void apply(SearchRequestBuilder searchRequestBuilder, final Sortable request) {
        if (request.getSort() != null && "".equals(request.getSort().trim()))
            return;

        sortParser.parse(ES_DEFAULT_SORT.getValue(request.getSort(), request.getIndex()))
            .stream()
            .map(item -> asFieldSortBuilder(request.getIndex(), item))
            .forEach(searchRequestBuilder::addSort);
        searchRequestBuilder.addSort(DEFAULT_TIEBREAKER);
    }

    private FieldSortBuilder asFieldSortBuilder(String index, Item item) {
        String fieldName = item.getField().getName();

        FieldSortBuilder fieldSortBuilder = fieldSort(fieldName).order(valueOf(item.getOrderOperator().name()));
        String parentField = fieldName.split("\\.")[0];

        if (settingsAdapter.isTypeOf(index, parentField, FIELD_TYPE_NESTED)) {
            fieldSortBuilder.setNestedPath(parentField);
            item.getQueryFragmentList()
                .map(this::asTermsQueryBuilder)
                .ifPresent(fieldSortBuilder::setNestedFilter);
        }
        return fieldSortBuilder;
    }

    private TermQueryBuilder asTermsQueryBuilder(QueryFragmentList queryFragmentList) {
        // FIX ME - We are waiting a refactor for ElasticSearchQueryAdapter in order to reuse the Filter created from a QueryFragment
        QueryFragmentItem queryFragment = (QueryFragmentItem) queryFragmentList.get(0);

        Filter filter = queryFragment.getFilter();
        return termQuery(filter.getField().getName(), filter.getValue().value());
    }
}
